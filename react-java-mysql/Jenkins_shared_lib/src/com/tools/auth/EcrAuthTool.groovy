package com.tools.auth

import com.tools.Tool

class EcrAuthTool implements Tool {

    private String accountNumber
    private String awsRegion
    private String appName

    void execute(steps) {
        String repositoryName = this.appName
        steps.echo "Account Number: {accountNumber}"

        //aws assume role acceskeys might needed

        steps.sh """ 
         \$(aws ecr get-login --no-include-email --region ${this.awsRegion} --registry-ids {accountNumber} | sed 's,https://,,g')
           aws ecr describe-repositories --region ${this.awsRegion} \
             --repository-names ${repositoryName} > /dev/null 2>&1 || 
             aws ecr create-repository --region ${this.awsRegion} --repository-name ${repositoryName}
             
             
       """
    }

}

