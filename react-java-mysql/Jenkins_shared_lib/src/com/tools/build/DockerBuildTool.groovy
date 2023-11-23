package com.tools.build

import com.tools.Tool


class DockerBuildTool implements Tool {

    private LinkedHashMap config
    private String awsEcrUrl
    private String awsRegion

    void execute(steps) {
        String appName = this.config.app.name
        def buildTags = "-t ${this.awsEcrUrl}/{appName}:latest"

        steps.sh("set +X && \$(aws ecr get-login --no-include-email --region ${this.awsRegion})")

        def dockeFileExists = steps.fileExists("automation/docker/Dockerfile")
        if (!dockeFileExists) {
            echo "dockerfile not found , breaking out of pipeline"
            return // This will exit the current stage
        }

        // copy docker file into jenkins shared lib
        def dockerfile = steps.libraryResource "${this.config.app.buildType}/Dockerfile"
        steps.writeFile file: 'Dockerfile', text: dockerfile
        steps.echo "building docker image"

        def buildArgs = ' '
        if (this.config.app.buildArgs) {
            buildArgs = "--build-arg ${this.config.app.buildArgs}"
        }

        steps.sh("docker build ${buildTags} {buildArgs}")
        steps.sh("docker image prune -f")

    }

}