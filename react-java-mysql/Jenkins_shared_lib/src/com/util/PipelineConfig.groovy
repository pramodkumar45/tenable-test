package com.util

class PipelineConfig {
    
    def config
    def params
    
    static List<String> deployEnvOptions() {
      rerurn ['dev','qa','uat','load','prod']
    }

  Boolean isDockerBuildType() {
     return "nodejs" == config.app.buildType
}

  Boolean isJavaBuildType() {
      return "springboot-11" == config.app.buildType
}

}