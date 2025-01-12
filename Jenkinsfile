pipeline {
    agent { label 'ec2-devita-back' }

    environment {
        ECR_REGISTRY = '860195224276.dkr.ecr.ap-northeast-2.amazonaws.com'
        ECR_REPO_NAME = 'devita_ecr'
        IMAGE_TAG = 'latest_back'
        AWS_REGION = 'ap-northeast-2'
        AWS_CREDENTIALS = credentials('AwsCredentials')
        INSTANCE_ID = 'i-086704d461847e1a4'

        AWS_S3_BUCKET = 'devita-env'
        S3_ENV_FILE = 'application.yaml'
        LOCAL_ENV_FILE = 'src/main/resources'
    }
    stages {
        stage('Check for Previous Builds') {
            steps {
                script {
                    // 현재 Job에서 실행 중인 이전 빌드를 가져옴
                    def job = Jenkins.instance.getItemByFullName(env.JOB_NAME)
                    def currentBuildNumber = currentBuild.number
                    
                    // 이전 빌드가 실행 중이면 중단시킴
                    job.builds.each { build ->
                        if (build.isBuilding() && build.number < currentBuildNumber) {
                            echo "Cancelling build #${build.number}"
                            build.doStop()  // 이전 빌드를 중단
                        }
                    }
                }
            }
        }
        stage('Checkout') {
            steps {
                script {
                    git branch: 'develop', url: 'https://github.com/KTB-FinalProject-Team1/Devita_Backend', credentialsId: "githubAccessToken"
                }
            }
        }
        stage('Login to ECR') {
            steps {
                script {
                    sh '''
                    export AWS_ACCESS_KEY_ID=$(echo $AWS_CREDENTIALS | cut -d':' -f1)
                    export AWS_SECRET_ACCESS_KEY=$(echo $AWS_CREDENTIALS | cut -d':' -f2)
                    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
                    '''
                }
            }
        }
        stage('Download from S3') {
            steps {
                script {
                    sh '''
                    echo $AWS_ACCESS_KEY_ID
                    echo $AWS_CREDENTIALS
                    aws s3 cp s3://$AWS_S3_BUCKET/$S3_ENV_FILE $LOCAL_ENV_FILE --region $AWS_REGION
                    '''
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    sh '''
                    docker build -t $ECR_REPO_NAME:$IMAGE_TAG .
                    docker tag $ECR_REPO_NAME:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPO_NAME:$IMAGE_TAG
                    '''
                }
            }
        }
        stage('Push to ECR') {
            steps {
                script {
                    sh '''
                    docker push $ECR_REGISTRY/$ECR_REPO_NAME:$IMAGE_TAG
                    '''
                }
            }
        }
        stage('Start Deploy') {
            steps {
                script {
                    build job: 'cd_pipeline'
                    
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
