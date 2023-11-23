package com.util

class Jenkins {

  static def readConfig(steps, params) {

    def jenkinsConfig = "automation/jenkins/jenkins-config.yaml"
    steps.echo "checking for config" + jenkinsConfig
    def config = steps.readYaml file: jenkinsConfig
    steps.echo "config yaml" + config

    return config
  }
}
