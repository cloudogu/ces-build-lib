package com.cloudogu.ces.cesbuildlib

abstract class Maven implements Serializable {
    def script

    // Args added to each mvn call
    String additionalArgs = ""

    // Private vars lead to exceptions when accessing them from methods of this class. So, don't make them private...
    Repository deploymentRepository = null
    SignatureCredentials signatureCredentials = null

    Maven(script) {
        this.script = script
    }

    def call(args) {
        mvn(args)
    }

    abstract def mvn(String args)

    String createCommandLineArgs(String args) {
        // Apache Maven related side notes:
        // --batch-mode : recommended in CI to inform maven to not run in interactive mode (less logs)
        // -V : strongly recommended in CI, will display the JDK and Maven versions in use.
        //      Very useful to be quickly sure the selected versions were the ones you think.
        // -U : force maven to update snapshots each time (default : once an hour, makes no sense in CI).
        // -Dsurefire.useFile=false : useful in CI. Displays test errors in the logs directly (instead of
        //                            having to crawl the workspace files to see the cause).

        "--batch-mode -V -U -e -Dsurefire.useFile=false ${args + " " + additionalArgs}"
    }

    String getVersion() {
        def matcher = script.readFile('pom.xml') =~ '<version>(.+?)</version>'
        matcher ? matcher[0][1] : ""
    }

    String getMavenProperty(String propertyKey) {
        // Match multi line = (?s)
        def matcher = script.readFile('pom.xml') =~ "(?s)<properties>.*<$propertyKey>(.+)</$propertyKey>.*</properties>"
        matcher ? matcher[0][1] : ""
    }

    void setDepoymentRepo(String id, String url, String credentialsIdUsernameAndPassword) {
        deploymentRepository = new Repository(id, url, credentialsIdUsernameAndPassword)
    }

    void setSignatureCredentials(String publicKeyRingFile, String secretKeyRingFile, String secretKeyPassPhrase) {
        signatureCredentials = new SignatureCredentials(publicKeyRingFile, secretKeyRingFile, secretKeyPassPhrase)
    }

    /**
     * Signs jar and deploys using the nexus-staging-maven-plugin.
     *
     * Can be used to deploy th maven central
     * Project must adhere to the requirements: http://central.sonatype.org/pages/requirements.html
     *
     * 'ossrh' // Sonatype OSSRH (OSS Repository Hosting)
     */

    void deployToNexusRepository(String additionalDeployArgs = '') {
        deployToNexusRepository(false, additionalDeployArgs)
    }

    void deployToNexusRepository(Boolean useNexusStaging, String additionalDeployArgs = '') {
        if (!deploymentRepository) {
            script.error 'No deployment repository set. Cannot perform maven deploy.'
        }

        if (signatureCredentials) {
            script.withCredentials([script.file(credentialsId: signatureCredentials.publicKeyRingFile, variable: 'pubring'),
                                    script.file(credentialsId: signatureCredentials.secretKeyRingFile, variable: 'secring'),
                                    script.string(credentialsId: signatureCredentials.secretKeyPassPhrase, variable: 'passphrase')
            ]) {
                additionalDeployArgs =
                        // Sign jar using gpg
                        "gpg:sign -Dgpg.publicKeyring=${script.env.pubring} " +
                                "-Dgpg.secretKeyring=${script.env.secring} " +
                                "-Dgpg.passphrase=${script.env.passphrase} " +
                                additionalDeployArgs

                // Signatures requires the gpg binary on the PATH. It is present in the maven docker container.
                // For local maven installation
                doDeployToNexusRepository(useNexusStaging, additionalDeployArgs)
            }
        } else {
            script.echo 'No signature credentials set. Deploying unsigned'
            doDeployToNexusRepository(useNexusStaging, additionalDeployArgs)
        }
    }

    void doDeployToNexusRepository(Boolean useNexusStaging, String additionalDeployArgs = '') {

        String deployArgs = 'deploy'

        if (useNexusStaging) {
            // Use nexus-staging-maven-plugin instead of maven-deploy-plugin
            // https://github.com/sonatype/nexus-maven-plugins/tree/master/staging/maven-plugin#maven2-only-or-explicit-maven3-mode
            deployArgs =
                    "org.sonatype.plugins:nexus-staging-maven-plugin:deploy -Dmaven.deploy.skip=true " +
                    "-DserverId=${deploymentRepository.id} -DnexusUrl=${deploymentRepository.url} -DautoReleaseAfterClose=true "
        }

        script.withCredentials([script.usernamePassword(credentialsId: deploymentRepository.credentialsIdUsernameAndPassword,
                passwordVariable: 'password', usernameVariable: 'username')]) {

            String usernameProperty = "${deploymentRepository.id}.username"
            String passwordProperty = "${deploymentRepository.id}.password"

            // The deploy plugin does not provide an option of passing server credentials via command line
            // So, create settings.xml that contains custom properties that can be set via command line (property
            // interpolation) - https://stackoverflow.com/a/28074776/1845976
            writeSettingsXmlWithServer(deploymentRepository.id,
                    "\${${usernameProperty}}",
                    "\${${passwordProperty}}")

            mvn "source:jar javadoc:jar $deployArgs -DskipTests " +
                    // credentials for deploying to sonatype
                    "-D${usernameProperty}=${script.env.username} -D${passwordProperty}=${script.env.password} " +
                    "-DaltReleaseDeploymentRepository=${deploymentRepository.id}::default::${deploymentRepository.url}/content/repositories/releases/ " +
                    "-DaltSnapshotDeploymentRepository=${deploymentRepository.id}::default::${deploymentRepository.url}/content/repositories/snapshots/ " +
                    "$additionalDeployArgs " +
                    // Deploy last to make sure signature, source/javadoc jars, and potential additional goals are executed first
                    deployArgs
        }
    }

    /**
     * Writes a {@code HOME/.m2/settings.xml} file containing a server ID and password.
     *
     * This is useful when trying to deploy artifacts to a maven repo, as the maven-deploy-plugin does not allow for
     * passing credentials via command line.
     *
     * Note that the server.xml is written to the workspace on the build executor. This kind of breaks the security
     * concepts of Jenkins credentials. So it is recommended to <b>not</b> write your {@code serverUsername} and
     * {@code serverPassword} directly, but to use property interpolation in settings.xml and set them via command
     * line:
     *
     * {@code
     * mvn.writeSettingsXmlWithServer ('ossrh', ' $ossrh.username', ' $ossrh.password' )
     * mvn "<goals> -Dossrh.username=$username -Dossrh.password=$password "
     *}
     * That way, they are not written to the settings.xml file.
     *
     * See https://stackoverflow.com/a/28074776/1845976
     *
     * @param serverId
     * @param serverUsername
     * @param serverPassword
     */
    void writeSettingsXmlWithServer(def serverId, def serverUsername, def serverPassword) {
        script.echo "Writing ${script.pwd()}/.m2/settings.xml"
        script.writeFile file: "${script.pwd()}/.m2/settings.xml", text: """
<settings>
    <servers>
        <server>
          <id>$serverId</id>
          <username>$serverUsername</username>
          <password>$serverPassword</password>
        </server>
    </servers>
</settings>"""
    }

    // Unfortunately, inner classes cannot be accessed from Jenkinsfile: unable to resolve class Maven.Repository
    // So we just use setters... See setDepoymentRepo()
    static class Repository implements Serializable {

        Repository(String id, String url, String credentialsIdUsernameAndPassword) {
            this.id = id
            this.url = url
            this.credentialsIdUsernameAndPassword = credentialsIdUsernameAndPassword
        }

        String id
        String url
        String credentialsIdUsernameAndPassword
    }

    /**
     * Holds the Jenkins credentials IDs necessary for signing the jar.
     */
    static class SignatureCredentials implements Serializable {

        SignatureCredentials(String publicKeyRingFile, String secretKeyRingFile, String secretKeyPassPhrase) {
            this.publicKeyRingFile = publicKeyRingFile
            this.secretKeyRingFile = secretKeyRingFile
            this.secretKeyPassPhrase = secretKeyPassPhrase
        }

        String publicKeyRingFile
        String secretKeyRingFile
        String secretKeyPassPhrase
    }
}