import com.stages.Stage
import com.tools.Tool
import com.tools.auth.EcrAuthTool
import com.tools.build.DockerBuildTool
import com.tools.publish.DockerPublishTool
import com.tools.release.MavenReleaseTool
import com.util.Jenkins
import com.util.PipelineConfig
import com.util.Versions
import com.util.Git
import com.util.Aws
import com.tools.build.MavenBuildTool
import com.util.Maven
import com.tools.publish.MavenPublishTool

static def containerType(buildType) {
    if(['springboot-11','java11'].contains(buildType)) {
        return 'maven'
    } else{
        return 'docker'
    }

}

def call() {
  
  def config
  def awsEcrUrl
  def awsAccountId
  def pipelineConfig
    String versionClassifier
    String versionString
    int[] version

    Git gitTool = new Git(gitCredentials: 'tenable-git')
  Tool sonarQubeTool
  currentBuild.result = 'SUCCESS'
  
  
  String podLabel = "slave-${UUID.randomUUID().toString()}"
  List<String> deployEnvOptions = PipelineConfig.deployEnvOptions()

  
  pipeline {
      agent {
          kubernetes {
              yaml """
apiVersion: V1
kind: Pod
metadata:
  labels:
    label: ${podLabel}
spec:
 securityContext:
   runAsUSer: 1000
   runAsGroup: 1000
   fsGroup: 1000
 containers:
  - name: git
    image: alpine/git
    volumeMounts:
    - name: workspace
      mountPath: /workspace
    command:
    - cat
    tty: true
  - name: maven
    image: maven:3.6.3-jdk-8
    workingDir: /workdir/server/target/dependency
    volumeMounts:
   - name: m2-repo
     mountPath:/root/.m2
  - name: sonar-scanner
    image: sonar
    command:
      - cat
    tty: true
  - name: docker
    image: 123456.dkr.ecr.us-east1.awazonaws.com/docker-aws-cli:1
    securityContext:
      runAsUSer: 0
      runAsGroup: 0
      fsGroup: 0
   command:
   - cat
   tty: true
   volumeMounts:
   - name: docker-sock
     mountPath: /var/run/docker.sock
 volumes:
   - name: docker-sock
     hostPath:
      path: /var/run/docker.sock
   - name: m2-repo
     hostPath:
      path: /root/.m2
"""
          }
    }
      options {
          buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
      }
      triggers {
          githubPush()
   }   
   parameters {
     choice(name: 'deployEnv', choices: deployEnvOptions, description: 'Build and deploy env')
     choice(name: 'awsRegion', choices: ['us-east1', 'eu-west-2'],description: 'AWS region')
   }
   
   stages {
       stage('Git checkout and load config') {
           steps {
               script {
                   container('jnlp') {
                       config = Jenkins.readConfig(steps, params)
                       pipelineConfig = new PipelineConfig(conig, params)
                       awsAccountId = Aws.getAccountId(params.deployEnv)
                       awsEcrUrl = "${awsAccountId}.dkr.ecr.${params.awsRegion}.amazonaws.com"
                   }
               }
           }

       }

       stage('Build/Compile') {
           steps {
               script {
                   if(pipelineConfig.isJavaBuildType()) {
                       container('maven') {
                           version = Maven.getVersion(steps)
                           versionClassifier = Versions.getClassifier(steps, env.BRANCH_NAME)
                           versionString = ${Versions.getString(version)}-${versionClassifier}
                           steps.echo "Version String: ${VersionString}"
                           Stage buildStage = new Stage()
                           buildStage.add(new MavenBuildTool(
                                   currentVersion: Maven.getRawVersion(steps),
                                   newVersion: versionString,
                                   opts: config.app.mavenBuildParams
                           ))
                            buildStage.execute(steps)
                   }
               }else if(pipelineConfig.isNodeJsBuildType()){
                       container('docker'){
                           packageJSON = readJSON file: './package.json'
                           versionString = packageJSON.version
                           sh 'node -v'
                           if(config.app.registries){
                               sh " echo registry=${config.app.registries.npm.url} > .npmrc"
                           }
                           sh "npm install --force"
                           if(config.app.b uildParams){
                               sh "npm" + config.app.buildParams
                           }
                       }
                     }else {
                       println "no build tasks are  avilable" ${config.app.buildParams}
                   }
                   }
           }
       }
   }

      stage('Unit and Integration Tests') {
          when {expression { (config.app.junit) || (config.app.integrationTestParams) }}

          steps {
              script {
                      if (pipelineConfig.isJavaBuildType() || (config.app.junit)) {
                          container(containerType(config.app.buildType)) {
                              version = Maven.getVersion(steps)
                              versionClassifier = Versions.getClassifier(steps, env.BRANCH_NAME)
                              versionString = ${Versions.getString(version)}-${versionClassifier}
                              steps.echo "Version String: ${VersionString}"
                              Stage buildStage = new Stage()
                              buildStage.add(new MavenBuildTool(
                                      currentVersion: Maven.getRawVersion(steps),
                                      newVersion: versionString,
                                      opts: config.app.junit
                              ))
                              buildStage.execute(steps)
                          }
                      }else if(pipelineConfig.isNodeJsBuildType() && (config.app.integrationTestParams)) {
                          container('docker') {
                              if(config.app.registries) {
                                  sh "echo registry=${config.app.registries.npm.url} > .npmrc"
                              }
                              echo "Integation test with npm"
                              sh 'npm ' + config.app.integrationTestParams
                          }
                  }else {
                          println "no test executed for the build type:" ${config.app.buildType}
                      }
              }
          }
      }

      stage('SonarQube Quality Gate') {
          when {
              expression {
                  return config.app.sonarParams
              }
          }
          steps {
              container('maven') {
                  script {
                      sonarQubeTool = new SonarQubeTool(
                              server: 'SonarQube',
                              params: config.app.sonarParams,
                              branchName: env.BRANCH_NAME
                      )
                  }

              }

          }
      }

      stage('publish Artifacts') {
          steps {
              script {
                  withCredentials([usernamePassword(credentialId: 'tenable', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PWD')]) {
                      if (pipelineConfig.isJavaBuildType()) {
                          container(containerType(config.app.buildType)) {
                              Stage publishStage = new Stage()
                              publishStage.add(new MavenPublishTool(
                                      currentVersion: Maven.getRawVersion(steps),
                                      newVersion: versionString,
                                      opts: config.app.mavenBuildParams
                              ))
                              publishStage.execute(steps)
                          }
                      }
                  }
              }
          }
      }
       stage('CreateImage') {
           steps {
             script {
                container('docker') {
                    Stage buildStage = new Stage()
                    buildStage.add(new DockerBuildTool(
                    awsRegion: params.awsRegion,
                    config: config,
                    awsEcrUrl: awsEcrUrl
               ))
              buildStage.execute(steps)
         }
    }
      }
    }
}

  stage('push docker image') {
    steps {
      container('aws') {
        script {
          Stage publishStage = new Stage()
          publishStage.add(new EcrAuthTool(
            accountNumber: awsAccountId,
            awsRegion: params.awsRegion,
            appName: config.app.name
            ))
            
           publishStage.add(new DockerPublishTool(
            conig: config,
            awsEcrUrl: awsEcrUrl,
            version:versionString
            ))

            publishStage.execute(steps)
        }
      }  
    }
  }

    stage('Create Release') {
        when { expression { return  versionClassifier == 'GA'} }
    }
        steps {
            container(containerType(config.app.buildType)) {
                script {
                    if(pipelineConfig.isJavaBuildType){
                        Stage buildStage = new Stage()
                       buildStage.add(new MavenReleaseTool(
                       version: version,
                       versionString: versionString,
                       releseBranch: env.BRANCH_NAME,
                       currentBuild: currentBuild,
                       workspace: env.WORKSPACE,
                       repoName:env.GIT_REPO_NAME))
                        buildStage.execute(steps)
                    }else{
                        println "release not identified for buildtype ${config.app.buildType}"
                    }
                }
            }

        }
    stage('Trigger Deploy') {
        steps {
            script {
                def jobName = "tenable-deploy/${config.app.name}"
                def jobParameters = [
                        String(name: 'dev', value: params.deployEnv),
                        String(name: 'awsRegion', value: params.awsRegion),
                        String(name: 'imageVersion', value: versionString)
                ]
                try {
                    def triggeredBuild = build job: jobName, parameters: jobParameters
                    echo "Triggered job ${jobName} with build number ${triggeredBuild.number}"
                } catch (err) {
                    println "WARNING: Trigger of downstream job failed" + err
                }
            }


        }
    }




}

