package com.cloudogu.ces.cesbuildlib

abstract class Maven implements Serializable {
    protected script

    // Args added to each mvn call
    String additionalArgs = ""

    // Private vars lead to exceptions when accessing them from methods of this class. So, don't make them private...
    Repository deploymentRepository = null
    SignatureCredentials signatureCredentials = null

    Maven(script) {
        this.script = script
    }

    def call(String args) {
        mvn(args)
    }

    abstract def mvn(String args)

    protected def mvnw(String args) {
        script.sh "./mvnw ${createCommandLineArgs(args)}"
    }

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

    @Deprecated
    void setDeploymentRepository(String id, String url, String credentialsIdUsernameAndPassword) {
        // Legacy behavior support nexus 2 only
        useDeploymentRepository([id: id, url: url, credentialsId: credentialsIdUsernameAndPassword, type: 'Nexus2'])
    }

    void useDeploymentRepository(Map config) {
        // Naming this method set..() causes Groovy issues on Jenkins because the parameters are a Map but the object is a Repository:
        // Cannot cast object 'com.cloudogu.ces.cesbuildlib.Maven$Nexus3@11f9131c' with class 'com.cloudogu.ces.cesbuildlib.Maven$Nexus3' to class 'java.util.Map'

        script.echo "Setting deployment repository with config ${config}"

        String id = config['id']
        String url = config['url']
        String creds = config['credentialsId']
        if ('Nexus2'.equals(config['type'])) {
            deploymentRepository = new Nexus2(id, url, creds)
        } else {
            if (!'Nexus3'.equals(config['type'])) {
                script.echo "useDeploymentRepository() - Repository type \"${config['type']}\" empty or unknown. Defaulting to Nexus 3."
            }
            deploymentRepository = new Nexus3(id, url, creds)
        }
    }

    /**
     * Set Jenkins credentials for signing artifacts deployed to nexus via deployToNexusRepository().
     * If not set, no singing will take place.
     *
     * @param secretKeyAscFile an asc file that contains the secret ring
     *                         Can be exported via  {@code gpg --export-secret-keys -a ABCDEFGH > secretkey.asc}
     * @param secretKeyPassPhrase the passphrase to the {@code secretKeyAscFile}
     */
    void setSignatureCredentials(String secretKeyAscFile, String secretKeyPassPhrase) {
        signatureCredentials = new SignatureCredentials(secretKeyAscFile, secretKeyPassPhrase)
    }

    /**
     * Deploy to a maven repository using the default maven-deploy-plugin.
     * Note that if the pom.xml's version contains {@code -SNAPSHOT}, the artifacts are automatically deployed to the
     * snapshot repository. Otherwise, the artifacts are deployed to the release repo.
     *
     * Make sure to configure repository before calling, using
     * {@link #useDeploymentRepository(java.util.Map)}.
     *
     * If you want to deploy a signed jar, set signature credentials using
     * {@link #setSignatureCredentials(java.lang.String, java.lang.String)}.
     */
    void deployToNexusRepository(String additionalDeployArgs = '') {
        deployToNexusRepository(DeployGoal.REGULAR, additionalDeployArgs)
    }

    /**
     * Deploy to a maven repository using the nexus-staging-maven-plugin.
     * Note that if the pom.xml's version contains {@code -SNAPSHOT}, the artifacts are automatically deployed to the
     * snapshot repository. Otherwise, the artifacts are deployed to the release repo.
     *
     * Make sure to configure repository before calling, using
     * {@link #useDeploymentRepository(java.util.Map)}.
     *
     * If you want to deploy a signed jar, set signature credentials using
     * {@link #setSignatureCredentials(java.lang.String, java.lang.String)}.
     * E.g. {@code mvn.setSignatureCredentials('mavenCentral-secretKey-asc-file','mavenCentral-secretKey-Passphrase')}
     *
     * This can be used to deploy to maven central.
     * The project must adhere to the requirements: http://central.sonatype.org/pages/requirements.html
     * {@code mvn.useDeploymentRepository([id: ossrh, url: 'https://oss.sonatype.org/', credentialsId:
     * 'mavenCentral-acccessToken-credential', type: 'Nexus2'])}
     * where 'ossrh' means Sonatype OSS Repository Hosting.
     * Note that signing is mandatory to deploy releases to maven central.
     *
     */
    void deployToNexusRepositoryWithStaging(String additionalDeployArgs = '') {
        deployToNexusRepository(DeployGoal.NEXUS_STAGING, additionalDeployArgs)
    }

    /**
     * Deploy site to a maven repository using the maven-site-plugin's "site:deploy" goal.
     * Note that the site plugin does not provide options to specify the target repository via the command line
     * (http://maven.apache.org/plugins/maven-site-plugin/deploy-mojo.html).
     *
     * That is, it has to be configured in the pom.xml like so:
     *
     *     &lt;distributionManagement&gt;
     *         &lt;site&gt;
     *             &lt;id&gt;YOUR-ID&lt;/id&gt;
     *             &lt;name&gt;site repository cloudogu ecosystem&lt;/name&gt;
     *             &lt;url&gt;dav:https://your.domain/nexus/repository/Site-repo/${project.groupId}/${project.artifactId}/${project.version}/&lt;/url&gt;
     *         &lt;/site&gt;
     *     &lt;/distributionManagement&gt;
     *
     * Make sure to configure repository before calling, using
     * {@link #useDeploymentRepository(java.util.Map)}, where the id parameter must match the one specified in the pom
     * ("YOUR-ID" in the example above) and the url parameter is ignored (taken from pom.xml).
     *
     * If you want to deploy a signed jar, set signature credentials using
     * {@link #setSignatureCredentials(java.lang.String, java.lang.String)}.
     */
    void deploySiteToNexus(String additionalDeployArgs = '') {
        deployToNexusRepository(DeployGoal.SITE, additionalDeployArgs)
    }

    protected void deployToNexusRepository(DeployGoal goal, String additionalDeployArgs = '') {
        if (!deploymentRepository) {
            script.error 'No deployment repository set. Cannot perform maven deploy.'
        }
        def missingMandatoryField = goal.validateMandatoryFields(deploymentRepository)
        if (missingMandatoryField) {
            script.error missingMandatoryField
        }

        if (signatureCredentials) {
            script.withCredentials([script.file(credentialsId: signatureCredentials.secretKeyAscFile, variable: 'ascFile'),
                                    script.string(credentialsId: signatureCredentials.secretKeyPassPhrase, variable: 'passphrase')
            ]) {
                // Use koshuke's pgp instead of the maven gpg plugin.
                // gpg version2 forces the usage of a key agent which is difficult on CI server
                // http://kohsuke.org/pgp-maven-plugin/secretkey.html
                script.withEnv(["PGP_SECRETKEY=keyfile:${script.env.ascFile}",
                                "PGP_PASSPHRASE=literal:${script.env.passphrase}"]) {

                    additionalDeployArgs = 'org.kohsuke:pgp-maven-plugin:sign ' + additionalDeployArgs

                    doDeployToNexusRepository(goal, additionalDeployArgs)
                }
            }
        } else {
            script.echo 'No signature credentials set. Deploying unsigned'
            doDeployToNexusRepository(goal, additionalDeployArgs)
        }
    }

    protected void doDeployToNexusRepository(DeployGoal goal, String additionalDeployArgs = '') {

        script.echo "creating goal with additionalDeployArgs=$additionalDeployArgs"
        String deployGoal = goal.createGoal(deploymentRepository, additionalDeployArgs)
        script.echo "created goal $deployGoal"

        // When using "env.x", x may not contain dots, and may not start with a number (e.g. subdomains, IP addresses)
        String usernameProperty = "NEXUS_REPO_CREDENTIALS_USERNAME"
        String passwordProperty = "NEXUS_REPO_CREDENTIALS_PASSWORD"

        script.withCredentials([script.usernamePassword(credentialsId: deploymentRepository.credentialsIdUsernameAndPassword,
                passwordVariable: passwordProperty, usernameVariable: usernameProperty)]) {

            // The deploy plugin does not provide an option of passing server credentials via command line
            // So, create settings.xml that contains custom properties that can be set via command line (property
            // interpolation) - https://stackoverflow.com/a/28074776/1845976
            String settingsXmlPath = writeSettingsXmlWithServer(deploymentRepository.id,
                    "\${env.${usernameProperty}}",
                    "\${env.${passwordProperty}}")
            mvn "-DskipTests " +
                // TODO when using nexus staging, we might have to deploy to two different repos. E.g. for maven central:
                // https://oss.sonatype.org/service/local/staging/deploy/maven2 and
                // https://oss.sonatype.org/content/repositories/snapshots
                // However, nexus-staging-maven-plugin does not seem to pick up the -DaltDeploymentRepository parameters
                // See: https://issues.sonatype.org/browse/NEXUS-15464
                // "-DaltDeploymentRepository=${deploymentRepository.id}::default::${deploymentRepository.url}/content/repositories/snapshots " +
                "-DaltReleaseDeploymentRepository=${deploymentRepository.id}::default::${deploymentRepository.url}${deploymentRepository.releasesRepository} " +
                "-DaltSnapshotDeploymentRepository=${deploymentRepository.id}::default::${deploymentRepository.url}${deploymentRepository.snapshotRepository} " +
                "-s \"${settingsXmlPath}\" " + // Not needed for MavenInDocker (but does no harm) but for MavenLocal
                deployGoal
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
     * @return
     */
    String writeSettingsXmlWithServer(def serverId, def serverUsername, def serverPassword) {
        def settingsXmlPath = "${script.pwd()}/.m2/settings.xml"
        script.echo "Writing $settingsXmlPath"
        script.writeFile file: settingsXmlPath, text: """
<settings>
    <servers>
        <server>
          <id>$serverId</id>
          <username>$serverUsername</username>
          <password>$serverPassword</password>
        </server>
    </servers>
</settings>"""
        return settingsXmlPath
    }

    // Unfortunately, inner classes cannot be accessed from Jenkinsfile: unable to resolve class Maven.Repository
    // So we just use setters... See setDepoymentRepo()
    static abstract class Repository implements Serializable {

        String id
        String url
        String credentialsIdUsernameAndPassword

        Repository(String id, String url, String credentialsIdUsernameAndPassword) {
            this.id = id
            this.url = url
            this.credentialsIdUsernameAndPassword = credentialsIdUsernameAndPassword
        }

        abstract String getSnapshotRepository()
        abstract String getReleasesRepository()
    }

    /**
     * Holds the Jenkins credentials IDs necessary for signing the jar.
     */
    static class SignatureCredentials implements Serializable {

        SignatureCredentials(String secretKeyAscFile, String secretKeyPassPhrase) {
            this.secretKeyAscFile = secretKeyAscFile
            this.secretKeyPassPhrase = secretKeyPassPhrase
        }

        String secretKeyAscFile
        String secretKeyPassPhrase
    }

    static class Nexus2 extends Repository {
        String snapshotRepository =  '/content/repositories/snapshots'
        String releasesRepository =  '/content/repositories/releases'

        Nexus2(String id, String url, String credentialsIdUsernameAndPassword) {
            super(id, url, credentialsIdUsernameAndPassword)
        }
    }

    static class Nexus3 extends Repository {
        String snapshotRepository =  '/repository/maven-snapshots'
        String releasesRepository =  '/repository/maven-releases'

        Nexus3(String id, String url, String credentialsIdUsernameAndPassword) {
            super(id, url, credentialsIdUsernameAndPassword)
        }
    }

    enum DeployGoal {
        REGULAR(
                // Build sources and javadoc first, because they need to be signed as well.
                // Otherwise we'll run into an NPE here: https://github.com/kohsuke/pgp-maven-plugin/blob/master/src/main/java/org/kohsuke/maven/pgp/PgpMojo.java#L188
                SOURCE_JAVADOC_PACKAGE +
                '${additionalDeployArgs} ' +
                // Deploy last to make sure package, source/javadoc jars, signature and potential additional goals are executed first
                'deploy:deploy',
                ['id', 'url', 'credentialsIdUsernameAndPassword']
        ),
        NEXUS_STAGING(
                SOURCE_JAVADOC_PACKAGE +
                '${additionalDeployArgs} ' +
                // Use nexus-staging-maven-plugin instead of maven-deploy-plugin
                // https://github.com/sonatype/nexus-maven-plugins/tree/master/staging/maven-plugin#maven2-only-or-explicit-maven3-mode
                'org.sonatype.plugins:nexus-staging-maven-plugin:deploy -Dmaven.deploy.skip=true ' +
                '-DserverId=${id} -DnexusUrl=${url} ' +
                '-DautoReleaseAfterClose=true ',
                ['id', 'url', 'credentialsIdUsernameAndPassword']
        ),
        SITE('${additionalDeployArgs} ' +
                'site:deploy',
                ['id', 'credentialsIdUsernameAndPassword'])

        private static final String SOURCE_JAVADOC_PACKAGE = 'source:jar javadoc:jar package '

        final String goal
        final List mandatoryFields

        private DeployGoal(String goal, mandatoryFields) {
            this.goal = goal
            this.mandatoryFields = mandatoryFields
        }

        String createGoal(Repository repository, String additionalDeployArgs) {

            goal.replace('${id}', (repository.id ? repository.id : ""))
                .replace('${url}', (repository.url ? repository.url : ""))
                .replace('${additionalDeployArgs}', additionalDeployArgs)
        }

        String validateMandatoryFields(Repository repository) {
            for (String fieldKey  : mandatoryFields) {
                // Note: "[]" syntax (and also getProperty()) leads to
                // Scripts not permitted to use staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods getAt
                if (!(repository."$fieldKey")) {
                    // We can't access "script" variable here to call script.error directly. So just return a string
                    return "Missing required '${fieldKey}' parameter."
                }
            }
            return ""
        }
    }
}