# tenable-test

1.I have created declarative pipeline for cicd. It has automation folder where the docker and required Jenkins config files inside the folder.

2.jenkins config yaml has  all the required params has to be there to build and deploy the code.

3.Jenkins build groovy and Jenkins deploy groovy useful to call the jenkins shared library and has to be configured in jenkins pipleine to build and deploy the code.

4. jenkins shared library has 2 main file buildpipeline.groovy and deploypipleine.groovy which uses podtemplated to spinup agents and all the required containers for ex maven,node,helm,docker,aws will be aviable from it.
   
5. when the code commits the pipleine triggers and it runs thru all the tages build,test,sonar,create docker image and push to ecr and then starts deploy to dev environemnt using kubernetes helm templates and run the automation tests.
   
6. Once automation is good and it release the code rom master bracnh and creates the tag and creates new docker image by running all the stages.
   
7.Helm templates i have provided only for backend service java and for observability added appdynamics and promtheius configuration in deployment yaml files.

8. Updated appdynamics jar agents in backend docker file and where install the agents and push the metrics to appdynamics but manually endpoints needs to be added to monitor the application.Another option promethues lets say prometheus installed already and apllications geneartes the metrics into given endpoint added in deployment yaml and scrape every 15sec using service monitor yaml and prometheus server url needs to be configured in grafana as a source to import the metrics for an app.
