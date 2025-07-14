# LAUNCH CONTROL
Repo https://github.com/wesHawkeyeMaszk/launch-control

# Must haves
Terraform
AWS Access Keys for the Account you wish to deploy code to. 


# Project Structure
Modules Directory will host the services you will need for your project. 

# Must have before anything
vpc - RV
ecr - docker registry for aws
    * pipeline to upload docker file + new tags to ECR
        * BOILER plate every Lift service has an example
ecs cluster
s3 bucket
# Completed 7/11


# Terraform Work 
Cloudfront - Wes
FrontEnd
    * CloudFront + S3 Bucket that will host your angular code
        index.html 
            etag - LOA project



BackEnd
    * ALB - Load Balancer
        - Listener 80/443
        - Target Group port driven whatever port your app reads on
            - ECS Service 
                - ECS Task <- Dockerfile info and parameters
                    - ECS Services/Tasks are found inside an ECS Cluster


# Github Actions
1. Backend deployment to ECR - RV
2. Frontend deployment to S3



Examples
ECS Task Definition
https://github.com/RocketPartners/liftck_tf_infra/tree/main/modules/terraform-aws-grafana-pdc-agent

