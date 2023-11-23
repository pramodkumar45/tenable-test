import com.util.Jenkins
import com.util.Aws
import com.util.PipelineConfig

def call() {
  
  def config
  def awsAccountId
  def namespace
  def globalConfig
  def ingressHostname
  
  
  currentBuild.result = 'SUCCESS'
  
  
  List<String> deployEnvOptions = PipelineConfig.deployEnvOptions()

  
  pipeline {
    agent {
       label ("deploy && region_${params.awsRegion}")
    }
   
   parameters {
     choice(name: 'deployEnv', choices: deployEnvOptions, description: 'Build and deploy env')
     choice(name: 'awsRegion', choices: ['us-east1', 'eu-west-2'],description: 'AWS region')
     choice(name:'imageVersion',description:'the image version')
   }
   
   stages {
      stage('Param Check') {
        
           steps{
              buildDescription "${params.deployEnv} ${params.awsRegion} ${params.imageVersion}"
              script {
                 if(params.imageVersion == null || params.imageVersion.trim() == ''){
                   currrentBuild.result = 'ABORTED'
                   error("image version is not detected and build aborting")
                  }
        }
   }
}
     stage('Git checkout and load config') {
       steps {
          script {
            checkout scm
            config = Jenkins.readConfig(steps,params)
            awsAccountId = Aws.getAccountId(params.deployEnv)

            awsEcrUrl = "${awsAccountId}.dkr.ecr.${params.awsRegion}.amazonaws.com"
            globalConfig = readYaml(text: libraryResource('etc/global.yaml'))

            def higherEnvironment = globalConfig.environmentMap[params.deployEnv]

            def eksClusterName = globalConfig.deploy[params.awsRegion[higherEnvironment]]

            println "clustername" + eksClusterName

            env.KUBE_CONTEXT = eksClusterName
            ingressHostname = "go-app.${params.deployEnv}.com"

          }
          
        }
      }
   }
   

  stage('Prepare') {
       steps {
         dir ("helm-charts") {
         
             script {
               
              sh "helm init --client-only"
              sh "helm repo remove stable"
              sh "helm remove local"
              sh "rm ${config.app.name}/requirements.lock"
              sh "helm dependecny upate ${config.app.name}"
              sh "helm dependecny build  ${config.app.name}"
              sh "helm template --name ${config.app.name} --values=${config.app.name}/${params.deployEnv}-${params.awsRegion}-values.yaml --set  deployment.image=${awsEcrUrl}/${config.app.name} --set deployment.version=${params.imageVersion} --set app.service.dns.ingressHostname=${ingressHostname} > ${WORKSPACE}/${config.app.name}/${params.deployEnv}-${params.awsRegion}-release.yaml"   "
               
             }
         }
    }
               

  stage('dryrun') {
      steps {
         dir (${config.app.name}) {
             script {
               namespace = "test"-${params.deployEnv}
               sh "kubectl config use-context KUBE_CONTEXT"
                def namespaceExists = sh(script: "kubectl get namespace ${namespace}", returnStatus: true) == 0
                    
                    if (namespaceExists) {
                        echo "Namespace ${namespace} exists. Applying resources..."
  
                        sh "kubectl apply --dry-run = true -n ${namespace} -f ."
                    } else {
                        echo "Namespace ${namespace} doesn't exist. Creating and applying resources..."
                        sh "kubectl create namespace ${namespace}"
                        sh "kubectl apply --dry-run = true -n ${namespace} -f ."
                    }
        }
      }  
    }
  }
      

  stage('deploy') 
  {
    steps {
         dir (${config.app.name}) {
         
             script {
                 echo "Deploy  ${config.app.name} in ${params.deployEnv}"
                 sh "kubectl apply -n ${namespace} -f ."
            
        }
      }  
    }
  }


      stage('Trigger Automation') {
          when {
              expression { config.app.integrationAutomationTestUrl && config.app.integrationTestParams }
              }
          steps {
              script {
                  def jobName = '/tenable-automation'
                  def jobParameters = [
                         String(name:'QA', value:params.deployEnv),
                         String(name:'awsRegion',value:params.awsRegion)
                  ]
             try{
                  def triggeredBuild = build job: jobName, parameters: jobParameters
                  echo "Triggered job ${jobName} with build number ${triggeredBuild.number}"
              }catch(err){
                 println "WARNING: Trigger of downstream job failed" + err
             }
          }
      }
   }
 }
      post {
          failure {
              always {
                  emailext (
                          to: 'devteam@test.com',
                          subject: "Pipeline Build Notification failure",
                          body: "The pipeline build has failed for ${config.app.name} in ${params.deployEnv} for version ${params.imageVersion}",
                          mimeType: 'text/html'
                  )
              }
          }
      }
   }
}