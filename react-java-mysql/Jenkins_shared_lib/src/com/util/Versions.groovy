package com.util

class Versions {
    static String getClassifier(steps, String branch) {
        if (branch == 'origin/master' || branch == 'master') {
            return 'GA'
        }else {
            return "${branch.replaceAll('/','-')}-SNAPSHOT"

        }
    }
    static String getString(int[] version) {
        if(version.size()!=3){
            throw new Exception(

                    """ version must contain 3 integers which denotes major,minor and micro
                            """
            )


        }
        return version.join('.')
    }
    static int[] incrementVersion(steps,version,String branch) {
        if (branch.contains('master')){
            version[1]++
            version[2] = 0
    }else if (branch.contains('hotifx')){
            version[2]++
        }else{
            steps.echo "canot increment version"
        }
    }
}
