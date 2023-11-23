package com.tools.publish
import com.tools.Tool
import com.util.Maven

class MavenPublishTool implements Tool{
    private String currentVersion
    private String newVersion
    private String opts = ''
    public void execute(steps,env) {
        if(!this.opts || !this.opts.trim()) {
            this.opts = ''
        }
        Maven.updateVersion(steps,currentVersion,newVersion)
        Maven.mvn(steps, 'deploy -DskipTests',opts)
    }
}

