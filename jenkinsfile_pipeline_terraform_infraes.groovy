pipeline {
  agent any
  environment {
    AWS_DEFAULT_REGION = 'us-west-2'
  }
  stages {
    stage('Checkout') {
      steps {
        checkout scm: git(
          branches: [[name: '*/main']],
          extensions: [],
          userRemoteConfigs: [[credentialsId: 'jenkins_instance', url: 'https://github.com/pato2819/ci-cd-terraform.git']]
        )
      }
    }
    stage('Terraform Init') {
      steps {
        dir('terraform') {
          sh 'terraform init'
        }
      }
    }
    stage('Terraform Apply') {
      steps {
        dir('terraform') {
          sh 'terraform apply -auto-approve'
        }
      }
    }
  }
}
