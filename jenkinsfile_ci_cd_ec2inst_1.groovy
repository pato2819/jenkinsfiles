pipeline {
    agent any
    parameters {
        choice(
            name: 'BRANCH',
            choices: ['main', 'test'],
            description: 'Select a branch to build'
        )
        choice(
            name: 'ACTION',
            choices: ['apply', 'destroy'],
            description: 'Select an action to perform'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "${params.BRANCH}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    submoduleCfg: [],
                    userRemoteConfigs: [[url: 'https://github.com/pato2819/EC2-Terraform.git']]
                ])
            }
        }
        
        stage('Terraform Init') {
            steps {
                sh 'terraform init'
            }
        }

        stage('Terraform Plan') {
            steps {
                sh 'terraform plan -out=tfplan'
            }
        }

        stage('Manual Approval') {
            steps {
                script {
                    def authorizedUsers = ['pato canul', 'devops']
                    def deployApproved = false
                    def userInput = input(
                        message: 'Proceed with deploy?',
                        ok: 'Deploy',
                        submitterParameter: 'submitter',
                        submitter: "pato canul,devops",
                        parameters: [
                            [$class: 'ChoiceParameterDefinition',
                             name: 'user',
                             description: 'Authorized user',
                             choices: authorizedUsers.join('\n'),
                             defaultValue: 'pato canul'
                            ]
                        ]
                    )
                    if (userInput && (userInput.get('user') == 'pato canul' || userInput.get('user') == 'devops')) {
                        deployApproved = true
                    } else {
                        echo "You are not authorized to deploy. Please contact the devops team."
                    }
                    env.DEPLOY_APPROVED = deployApproved.toString()
                }
            }
        }

        stage('Terraform Apply or Destroy') {
            when {
                environment name: 'DEPLOY_APPROVED', value: 'true'
            }
            steps {
                script {
                    if (params.ACTION == 'apply') {
                        sh 'terraform apply -auto-approve tfplan'
                    } else if (params.ACTION == 'destroy') {
                        sh 'terraform destroy -auto-approve'
                    } else {
                        echo "Invalid action selected: ${params.ACTION}"
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
        }
    }
}