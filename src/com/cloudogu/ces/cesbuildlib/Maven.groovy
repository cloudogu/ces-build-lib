package com.cloudogu.ces.cesbuildlib

abstract class Maven implements Serializable {
    protected script

    // Args added to each mvn call
    String additionalArgs = ""

    // Private vars lead to exceptions when accessing them from methods of this class. So, don't make them private...
    Repository deploymentRepository = null
    List<Repository> repositories = []

    SignatureCredentials signatureCredentials = null

    // When using "env.x", x may not contain dots, and may not start with a number (e.g. subdomains, IP addresses)
    String usernameProperty = "NEXUS_REPO_CREDENTIALS_USERNAME"
    String passwordProperty = "NEXUS_REPO_CREDENTIALS_PASSWORD"

    String settingsXmlPath

    Maven(script) {
        this.script = script
    }

    def call(String args, boolean printStdOut = true) {
        if (repositories.isEmpty()) {
            mvn(args, printStdOut)
        } else {
            script.withCredentials(createRepositoryCredentials(repositories)) {
                mvn(args, printStdOut)
            }
        }
    }

    /**
     * @param printStdOut - returns output of mvn as String instead of printing to console
     */
    protected abstract def mvn(String args, boolean printStdOut = true)

    def mvnw(String args, boolean printStdOut) {
        sh("MAVEN_USER_HOME=${script.env.WORKSPACE}/.m2", printStdOut)
        sh("MVNW_VERBOSE=true ./mvnw ${createCommandLineArgs(args)}", printStdOut)
    }

    void sh(String command, boolean printStdOut) {
        script.echo "executing sh: ${command}, return Stdout: ${printStdOut}"
        if (printStdOut) {
            // -V : strongly recommended in CI, will display the JDK and Maven versions in use.
            // Don't use this with sh(returnStdout: true ..) !
            script.sh "${command} -V"
        } else {
            new Sh(script).returnStdOut command
        }
    }

    String createCommandLineArgs(String args) {
        // Apache Maven related side notes:
        // --batch-mode : recommended in CI to inform maven to not run in interactive mode (less logs)
        //      Very useful to be quickly sure the selected versions were the ones you think.
        // -U : force maven to update snapshots each time (default : once an hour, makes no sense in CI).
        // -Dsurefire.useFile=false : useful in CI. Displays test errors in the logs directly (instead of
        //                            having to crawl the workspace files to see the cause).

        // -V : strongly recommended in CI, will display the JDK and Maven versions in use.
        // --> Only used when not returning to stdout! See sh()

        String commandLineArgs = "--batch-mode -U -e -Dsurefire.useFile=false ${args + " " + additionalArgs} "
        if (!repositories.isEmpty()) {
            commandLineArgs += "-s \"${settingsXmlPath}\" " // Not needed for MavenInDocker (but does no harm) but for MavenLocal
        }

        return commandLineArgs
    }

    String getVersion() {
        return evaluateExpression('project.version')
    }

    String getGroupId() {
        return evaluateExpression('project.groupId')
    }

    String getArtifactId() {
        return evaluateExpression('project.artifactId')
    }

    String getName() {
        return evaluateExpression('project.name')
    }

    String getMavenProperty(String propertyKey) {
        return evaluateExpression(propertyKey)
    }

    String evaluateExpression(String expression) {
        // See also: https://blog.soebes.de/blog/2018/06/09/help-plugin/
        def evaluatedString = call("org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=${expression} -q -DforceStdout", false)
        // we take only the last line of the evaluated expression,
        // because in the case of maven wrapper the home and sometimes the download is printed before
        return evaluatedString.trim().readLines().last()
    }

    @Deprecated
    void setDeploymentRepository(String id, String url, String credentialsIdUsernameAndPassword) {
        // Legacy behavior support nexus 2 only
        useDeploymentRepository([id: id, url: url, credentialsId: credentialsIdUsernameAndPassword, type: 'Nexus2'])
    }

    @Deprecated
    void useDeploymentRepository(Map config) {
        // Legacy. The same mechanism is also used to resolve dependencies, not only for deploying
        useRepositoryCredentials(config)
    }

    void useRepositoryCredentials(Map... configs) {

        for (int i=0; i<configs.size(); i++) {
            def repository = createRepository(configs[i])
            repositories.add(repository)

            if (configs[i].url) {
                if (deploymentRepository) {
                    script.error "Multiple repositories with URL passed. Maven CLI only allows for passing one alt deployment repo."
                }
                script.echo "WARNING: Using useRepositoryCredentials() with 'url' parameter is deprecated and might be removed in future. Better describe this in maven pom.xml and remove 'url' parameter from Jenkins."
                // Pass this repo's URL explicitly as altDeploymentRepository to maven
                deploymentRepository = repository
            }
        }

        writeSettingsXml()
    }

    Repository createRepository(config) {
        script.echo "Adding repository with config ${config}"

        String id = config['id']
        String url = config['url']
        String creds = config['credentialsId']

        Repository potentialRepository
        if ('Nexus2'.equals(config['type'])) {
            potentialRepository = new Nexus2(id, url, creds)
        } else {
            if (!'Nexus3'.equals(config['type'])) {
                script.echo "Adding Maven repository with type \"${config['type']}\" empty or unknown. Defaulting to Nexus 3."
            }
            potentialRepository = new Nexus3(id, url, creds)
        }

        def missingMandatoryField = potentialRepository.validateMandatoryFields()
        if (missingMandatoryField) {
            script.error missingMandatoryField
        }

        potentialRepository
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
     * Set version of maven project including all modules.
     *
     * @param newVersion new project version
     */
    void setVersion(String newVersion) {
        call "versions:set -DgenerateBackupPoms=false -DnewVersion=${newVersion}"
    }

    /**
     * Set version to next minor snapshot e.g.: 2.0.1 becomes 2.1.0-SNAPSHOT
     */
    void setVersionToNextMinorSnapshot() {
        call "build-helper:parse-version versions:set -DgenerateBackupPoms=false -DnewVersion='\${parsedVersion.majorVersion}.\${parsedVersion.nextMinorVersion}.0-SNAPSHOT'"
    }

    /**
     * Deploy to a maven repository using the default maven-deploy-plugin.
     * Note that if the pom.xml's version contains {@code -SNAPSHOT}, the artifacts are automatically deployed to the
     * snapshot repository. Otherwise, the artifacts are deployed to the release repo.
     *
     * Make sure to configure repository before calling, using
     * {@link #useRepositoryCredentials(java.util.Map)}.
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
     * {@link #useRepositoryCredentials(java.util.Map)}.
     *
     * If you want to deploy a signed jar, set signature credentials using
     * {@link #setSignatureCredentials(java.lang.String, java.lang.String)}.
     * E.g. {@code mvn.setSignatureCredentials('mavenCentral-secretKey-asc-file','mavenCentral-secretKey-Passphrase')}
     *
     * This can be used to deploy to maven central.
     * The project must adhere to the requirements: http://central.sonatype.org/pages/requirements.html
     * {@code mvn.useRepositoryCredentials([id: ossrh, url: 'https://oss.sonatype.org/', credentialsId:
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
     * {@link #useRepositoryCredentials(java.util.Map)}, where the id parameter must match the one specified in the pom
     * ("YOUR-ID" in the example above) and the url parameter is ignored (taken from pom.xml).
     *
     * If you want to deploy a signed jar, set signature credentials using
     * {@link #setSignatureCredentials(java.lang.String, java.lang.String)}.
     */
    void deploySiteToNexus(String additionalDeployArgs = '') {
        deployToNexusRepository(DeployGoal.SITE, additionalDeployArgs)
    }

    protected void deployToNexusRepository(DeployGoal goal, String additionalDeployArgs = '') {
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
        String deployGoal = goal.create(deploymentRepository, additionalDeployArgs)
        script.echo "created goal $deployGoal"

        script.withCredentials(createRepositoryCredentials(repositories)) {

            mvn "-DskipTests " +
                    // When using nexus staging, we might have to deploy to two different repos. E.g. for maven central:
                    // https://oss.sonatype.org/service/local/staging/deploy/maven2 and
                    // https://oss.sonatype.org/content/repositories/snapshots
                    // However, nexus-staging-maven-plugin does not seem to pick up the -DaltDeploymentRepository parameters
                    // Sonatype won't fix this issue, though: https://issues.sonatype.org/browse/NEXUS-15464
                    // "-DaltDeploymentRepository=${repository.id}::default::${repository.url}/content/repositories/snapshots " +
                    // So: When usin nexus staging (e.g. for maven central), the user will have to specify those in the pom.xml
                    ( (deploymentRepository && deploymentRepository.url) ?
                            "-DaltReleaseDeploymentRepository=${deploymentRepository.id}::default::${deploymentRepository.url}${deploymentRepository.releasesRepository} " +
                            "-DaltSnapshotDeploymentRepository=${deploymentRepository.id}::default::${deploymentRepository.url}${deploymentRepository.snapshotRepository} "
                            : '') +
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
    void writeSettingsXml() {

        // Maven does not provide an option of passing server credentials via command line
        // So, create settings.xml that contains custom properties that can be set via command line (property
        // interpolation) - https://stackoverflow.com/a/28074776/1845976

        settingsXmlPath = "${script.pwd()}/.m2/settings.xml"
        script.echo "Writing $settingsXmlPath"


        script.writeFile file: settingsXmlPath, text: """
<settings>
    <servers>
${String ret="" 
for (int i = 0; i < repositories.size(); i++) {
    def serverId = repositories[i].id
    def serverUsername = "\${env.${usernameProperty}_${i}}"
    def serverPassword = "\${env.${passwordProperty}_${i}}"
    ret +="          <server><id>${serverId}</id><username>${serverUsername}</username><password>${serverPassword}</password></server>\n"
}
ret
}
    </servers>
</settings>"""
    }

    List createRepositoryCredentials(List<Repository> allRepositories) {
        def credentials = []
        for (int i = 0; i < allRepositories.size(); i++) {
            credentials.add(
                    script.usernamePassword(credentialsId: allRepositories[i].credentialsIdUsernameAndPassword,
                            passwordVariable: "${passwordProperty}_${i}",
                            usernameVariable: "${usernameProperty}_${i}")
            )
        }
        return credentials
    }

    // Unfortunately, inner classes cannot be accessed from Jenkinsfile: unable to resolve class Maven.Repository
    // So we just use setters... See setDepoymentRepo()
    static abstract class Repository implements Serializable {

        String id
        String url
        String credentialsIdUsernameAndPassword

        final List mandatoryFields = ['id', 'credentialsIdUsernameAndPassword']

        Repository(String id, String url, String credentialsIdUsernameAndPassword) {
            this.id = id
            this.url = url
            this.credentialsIdUsernameAndPassword = credentialsIdUsernameAndPassword
        }

        abstract String getSnapshotRepository()
        abstract String getReleasesRepository()

        String validateMandatoryFields() {
            for (String fieldKey  : mandatoryFields) {
                // Note: "[]" syntax (and also getProperty()) leads to
                // Scripts not permitted to use staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods getAt
                if (!(this."$fieldKey")) {
                    // We can't access "script" variable here to call script.error directly. So just return a string
                    return "Missing required '${fieldKey}' parameter."
                }
            }
            return ""
        }
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
        REGULAR( { repository, String additionalDeployArgs ->
                // Build sources and javadoc first, because they need to be signed as well.
                // Otherwise we'll run into an NPE here: https://github.com/kohsuke/pgp-maven-plugin/blob/master/src/main/java/org/kohsuke/maven/pgp/PgpMojo.java#L188
                SOURCE_JAVADOC_PACKAGE +
                        // Deploy last to make sure package, source/javadoc jars, signature and potential additional goals are executed first
                        "${additionalDeployArgs} deploy:deploy" }
        ),
        NEXUS_STAGING( { repository, String additionalDeployArgs ->
                def repoId = repository ? repository.id : null
                def repoUrl = repository ? repository.url : null
                SOURCE_JAVADOC_PACKAGE +
                        additionalDeployArgs +
                        // Use nexus-staging-maven-plugin instead of maven-deploy-plugin
                        // https://github.com/sonatype/nexus-maven-plugins/tree/master/staging/maven-plugin#maven2-only-or-explicit-maven3-mode
                        ' org.sonatype.plugins:nexus-staging-maven-plugin:deploy -Dmaven.deploy.skip=true ' +
                        (repoId ? "-DserverId=${repoId} " : '') +
                        (repoUrl ? "-DnexusUrl=${repoUrl} " : '') +
                        '-DautoReleaseAfterClose=true ' }
        ),
        SITE({ repository, String additionalDeployArgs ->
                "${additionalDeployArgs} site:deploy" })

        private static final String SOURCE_JAVADOC_PACKAGE = 'source:jar javadoc:jar package '
        private Closure<String> createGoal

        String create(Repository repository, String additionalDeployArgs) {
            // Making createGoal accessible and calling it directly would require script approval
            createGoal.call(repository, additionalDeployArgs)
        }

        private DeployGoal(Closure createGoal) {
            this.createGoal = createGoal
        }
    }
}
