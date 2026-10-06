pipeline {
    agent any
    stages {
        stage('Test and package') {
            steps {
                sh 'mvn -B -f hrms/pom.xml clean verify'
            }
        }
        stage('SonarQube') {
            when {
                expression { return env.SONAR_TOKEN?.trim() }
            }
            steps {
                sh 'mvn -B -f hrms/pom.xml org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar -Dsonar.host.url=http://127.0.0.1:9000 -Dsonar.token=$SONAR_TOKEN'
            }
        }
    }
}
