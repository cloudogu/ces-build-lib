package com.cloudogu.ces.cesbuildlib
/**
 * Run maven in a docker container.
 *
 * This can be helpful,
 * * when constant ports are bound during the build that cause port conflicts in concurrent builds.
 *   For example, when running integration tests, unit tests that use infrastructure that binds to ports or
 * * when one maven repo per builds is required
 *   For example when concurrent builds of multi module project install the same snapshot versions.
 *
 * The build are run inside the official maven containers from https://hub.docker.com/_/maven/
 */
class MavenInDocker extends MavenInDockerBase {

    /** The version of the maven docker image to use, e.g. {@code maven:3.5.0-jdk-8} **/
    String mavenImage

    /**
     * @param script the Jenkinsfile instance ({@code this} in Jenkinsfile)
     * @param mavenImage the version of the maven docker image to use, e.g. {@code 3.5.0-jdk-8}
     * @param registryCredentialsId the registryCredentialsId (From Jenkins) to use for authenticating to the registry, if the mavenImage is not public.
     * @param registryUrl the registryUrl to use for getting the image
     */
    MavenInDocker(script, String mavenImage, String registryCredentialsId = null, String registryUrl = null, String jenkinsCredentialsId = "jenkins") {
        super(script)
        this.mavenImage = mavenImage
        this.registryCredentialsId = registryCredentialsId
        this.registryUrl = registryUrl
        this.jenkinsCredentialsId = jenkinsCredentialsId

        this.script.withCredentials([this.script.usernamePassword(credentialsId: this.jenkinsCredentialsId,
            passwordVariable: 'MAVEN_SETTINGS_PASSWORD', usernameVariable: 'MAVEN_SETTINGS_USER')]) {

            // we are creating a maven settings.xml and store it in the m2 folder. this is due to our private nexus repository where mandatory dependencies are stored for our spi's
            String settingsXmlPath = "${this.script.pwd()}/.m2/settings.xml"
            this.script.writeFile file: settingsXmlPath, text: """
    <settings>
        <servers>
          <server>
            <id>ecosystem.cloudogu.com</id>
            <username>${this.script.env.MAVEN_SETTINGS_USER}</username>
            <password><![CDATA[${this.script.env.MAVEN_SETTINGS_PASSWORD}]]></password>
          </server>
        </servers>
    </settings>"""

        }
    }

    @Override
    def call(Closure closure, boolean printStdOut) {
        inDocker(getMavenImage()) {
            sh("mvn ${createCommandLineArgs(closure.call())}", printStdOut)
        }
    }

    //allowing downward compatibility for the old workflow only specifying the tag
    def getMavenImage() {
        return mavenImage.contains(':') ? mavenImage : "maven:${mavenImage}"
    }
}
