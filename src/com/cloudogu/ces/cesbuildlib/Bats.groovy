package com.cloudogu.ces.cesbuildlib

/**
 * Bats provides functions to easily execute bats tests (bash scripting tests)
 */
class Bats {
    private script
    private docker

    private static String bats_base_image = "bats_base_image"
    private static String bats_custom_image = "bats_custom_image"
    private static String bats_tag = "bats_tag"
    def defaultSetupConfig = [
        bats_base_image  : "bats/bats",
        bats_custom_image: "cloudogu/bats",
        bats_tag         : "1.12.0"
    ]

    Bats(script, docker) {
        this.script = script
        this.docker = docker
    }

    void checkAndExecuteTests(config = [:]) {
        // Merge default config with the one passed as parameter
        config = defaultSetupConfig << config

        script.echo "Executing bats tests with config:"
        script.echo "${config}"
        def batsImage = docker.build("${config[bats_custom_image]}:${config[bats_tag]}", "--build-arg=BATS_BASE_IMAGE=${config[bats_base_image]} --build-arg=BATS_TAG=${config[bats_tag]} ./build/make/bats")
        try {
            script.sh "mkdir -p target"
            script.sh "mkdir -p testdir"

            batsImage.inside("--entrypoint='' -v ${script.env.WORKSPACE}:/workspace -v ${script.env.WORKSPACE}/testdir:/usr/share/webapps") {
                script.sh "make unit-test-shell-ci"
            }
        } finally {
            script.junit allowEmptyResults: true, testResults: 'target/shell_test_reports/*.xml'
        }
    }
}
