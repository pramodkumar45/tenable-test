package com.tools.publish

import com.tools.Tool


class DockerPublishTool implements Tool {
  
  private LinkedHashMap config
  private String awsEcrUrl
  private String version
  
  void execute(steps) {
       String appName = this.config.app.name
       steps.sh "docker push ${this.awsEcrUrl}/${appName}:${this.version}"
       steps.sh "docker rmi ${this.awsEcrUrl}/${appName}:${this.version}"
  }
}
