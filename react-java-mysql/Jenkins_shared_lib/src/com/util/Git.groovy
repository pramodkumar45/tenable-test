package com.util;

public class Git {
    private String GitCredentials;
    private boolean CredIntialized = false;
    void initializeCredentials(steps) {

        if (CredIntialized)
            return;
        steps.sh("git config credential.helper '!f() { echo \"username=\${GIT_USERNAME}\"; echo \"password=\${GIT_PASSWORD}\";};f'")
        steps.sh("git config --global user.email \"sre@gmail.com\"")

        CredIntialized = true;
    }
    String git(steps, String command, String args = '') {
        steps.withCredentials([
                usernamePassword(
                        credentialsId: this.GitCredentials,
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME'
                )
        ]){
            initializeCredentials(steps);
            return steps.sh(script: "git ${command} {args}", returnStdout: true)
        }

    }

}
