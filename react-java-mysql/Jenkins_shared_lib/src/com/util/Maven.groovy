package com.util

class Maven {
    static String getRawVersion(steps) {
        return mvn(
                steps,
                'help:evaluate',
                '-Dexpression=project.version - q -DforceStdout',
                true

        )
    }

 static String mvn(steps,String stages,String opts='', boolean returnStdout = false) {
     return steps.sh(script: "maven -s settings.xml ${stages} ${opts}",
             returnStdout: returnStdout
     )

 }
    static int[] getVersion(steps) {
        String currentVersion = getRawVersion(steps)
        retrn Versions.parseVersion(steps,currentVersion)
    }
    static void updateVersion(steps,currentVersion,newVersion) {
        if(currentVersion.equals(newVersion)) {
            steps.echo"skipping update beacuse current and new versions are same" ${newVersion}
        }else{
            steps.echo "updating ${currentVersion} to ${newVersion}"
            steps.sh("perl -i -pe 's#<version>${currentVersion}</version>#<version>${newVersion}</version>' \$(find -name pom.xml)")
        }
    }
    }

