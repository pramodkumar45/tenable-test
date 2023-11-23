package com.tools.release

import com.tools.Tool
import com.util.Git
import com.util.Maven
import com.util.Versions

class MavenReleaseTool implements  Tool{

    int [] version
    String versionString
    Git gitTool
    String releaseBranch
    def currentBuild
    private String workspace
    private String repoName
    private String message = "bumping new version and relase tag"
    void execute(steps) {

        version = Maven.getVersion(steps)
        steps.echo 'current version:' + version
        if(gitTool.tagExists(steps,versionString)) {
            steps.echo "Tag ${versionString} already exists aborting build"
            currentBuild.result = 'ABORTED'
        }
        steps.sh "git checkout ${releaseBranch}"
        int[] newVersion = Versions.incrementVersion(steps,version,releaseBranch)
        String newVersionString = Versions.getString(newVersion)
        steps.sh "git add -u"
        steps.sh "git stash"
        steps.sh "git stash apply || true"
        steps.sh "git commit "
        steps.sh("git push -m ${message}")
        steps.sh("git tag ${newVersionString}")
        steps.sh("git push --tags")
    }
}
