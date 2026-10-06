// Creates the Salohi IT AWS stack with Terraform, then deploys the HRMS jar.
//
// Run this as a Pipeline from SCM job on a Linux agent with Java 17 and Maven.
// Create these Jenkins credentials before the first build:
//   aws-hrms          Username with password. Access key id, secret access key.
//   hrms-db-password  Secret text. 12 to 64 letters and digits. Must match the
//                     password already used if the stack already exists.
//   hrms-deploy-key   SSH username with private key. Username ubuntu. The matching
//                     public key must already be in /home/ubuntu/.ssh/authorized_keys,
//                     or set on the instance when the stack is first created.
//   sonar-token       Secret text. Only required when Run SonarQube is checked.
//
// Terraform state is kept in /var/lib/jenkins/salohi-hrms-terraform, outside the
// workspace. If this Jenkins server should manage the stack that already exists,
// copy that stack's terraform.tfstate into that directory before the first apply.
// A build with no state file stops, unless Allow a new AWS stack is checked.

pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 90, unit: 'MINUTES')
    }

    parameters {
        string(name: 'AWS_REGION', defaultValue: 'ap-south-1', description: 'AWS region')
        string(name: 'ALLOWED_CIDR', defaultValue: '157.35.95.32/32', description: 'Only this CIDR can reach SSH, Jenkins, the portal, and SonarQube')
        string(name: 'INSTANCE_TYPE', defaultValue: 'm7i-flex.large', description: 'EC2 instance type')
        booleanParam(name: 'ALLOW_NEW_STACK', defaultValue: false, description: 'Create a new AWS stack when Jenkins has no Terraform state')
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis after the tests')
    }

    environment {
        TF_IN_AUTOMATION = 'true'
        TF_INPUT = 'false'
        TF_STATE_DIR = '/var/lib/jenkins/salohi-hrms-terraform'
        AWS_DEFAULT_REGION = "${params.AWS_REGION}"
        TF_VAR_aws_region = "${params.AWS_REGION}"
        TF_VAR_allowed_cidr = "${params.ALLOWED_CIDR}"
        TF_VAR_instance_type = "${params.INSTANCE_TYPE}"
        TF_VAR_ssh_public_key = ''
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Terraform apply') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'aws-hrms', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: 'hrms-db-password', variable: 'TF_VAR_db_password')
                ]) {
                    sh '''
                        set -euo pipefail
                        if ! command -v terraform >/dev/null 2>&1; then
                          mkdir -p "$HOME/.local/bin"
                          curl -fsSL -o /tmp/terraform.zip "https://releases.hashicorp.com/terraform/1.9.8/terraform_1.9.8_linux_amd64.zip"
                          unzip -o /tmp/terraform.zip -d "$HOME/.local/bin"
                          chmod +x "$HOME/.local/bin/terraform"
                          export PATH="$HOME/.local/bin:$PATH"
                        fi

                        mkdir -p "$TF_STATE_DIR"
                        if [ ! -f "$TF_STATE_DIR/terraform.tfstate" ] && [ "$ALLOW_NEW_STACK" != "true" ]; then
                          echo "No Terraform state in $TF_STATE_DIR."
                          echo "Copy the existing terraform.tfstate into that directory, or rerun with Allow a new AWS stack checked."
                          exit 1
                        fi
                        if [ -f "$TF_STATE_DIR/terraform.tfstate" ]; then
                          cp "$TF_STATE_DIR/terraform.tfstate" terraform/terraform.tfstate
                        fi

                        terraform -chdir=terraform init -input=false
                        terraform -chdir=terraform plan -input=false -out=tfplan
                        terraform -chdir=terraform apply -input=false tfplan
                        cp terraform/terraform.tfstate "$TF_STATE_DIR/terraform.tfstate"
                        rm -f terraform/tfplan
                    '''
                }
            }
        }

        stage('Read instance address') {
            steps {
                script {
                    env.PUBLIC_IP = sh(script: 'terraform -chdir=terraform output -raw public_ip', returnStdout: true).trim()
                    env.HRMS_URL = sh(script: 'terraform -chdir=terraform output -raw hrms_url', returnStdout: true).trim()
                }
                echo "Instance address is ${env.PUBLIC_IP}"
            }
        }

        stage('Wait for first boot') {
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'hrms-deploy-key', keyFileVariable: 'DEPLOY_KEY', usernameVariable: 'DEPLOY_USER')
                ]) {
                    sh '''
                        set -euo pipefail
                        ready=0
                        for _ in $(seq 1 40); do
                          if ssh -i "$DEPLOY_KEY" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15 "${DEPLOY_USER}@${PUBLIC_IP}" "test -x /opt/hrms/deploy-app.sh"; then
                            ready=1
                            break
                          fi
                          echo "Instance is still installing Java, Docker, and PostgreSQL..."
                          sleep 15
                        done
                        if [ "$ready" -ne 1 ]; then
                          echo "The instance did not finish first boot."
                          exit 1
                        fi
                    '''
                }
            }
        }

        stage('Test and package') {
            steps {
                sh 'mvn -B -f hrms/pom.xml clean verify'
            }
        }

        stage('SonarQube') {
            when {
                expression { return params.RUN_SONAR }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')
                ]) {
                    sh 'mvn -B -f hrms/pom.xml org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar -Dsonar.host.url=http://127.0.0.1:9000 -Dsonar.token="$SONAR_TOKEN"'
                }
            }
        }

        stage('Deploy application') {
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'hrms-deploy-key', keyFileVariable: 'DEPLOY_KEY', usernameVariable: 'DEPLOY_USER')
                ]) {
                    sh '''
                        set -euo pipefail
                        scp -i "$DEPLOY_KEY" -o StrictHostKeyChecking=accept-new hrms/target/hrms.jar "${DEPLOY_USER}@${PUBLIC_IP}:/tmp/hrms.jar"
                        ssh -i "$DEPLOY_KEY" -o StrictHostKeyChecking=accept-new "${DEPLOY_USER}@${PUBLIC_IP}" "sudo /opt/hrms/deploy-app.sh /tmp/hrms.jar"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "Portal: ${env.HRMS_URL}"
        }
    }
}
