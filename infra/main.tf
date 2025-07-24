terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.1"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Reference launch-control infrastructure via SSM parameters
data "aws_ssm_parameter" "launch_control_vpc_id" {
  name = "/launch-control/vpc/vpc_id"
}

data "aws_ssm_parameter" "launch_control_private_subnet_ids" {
  name = "/launch-control/vpc/private_subnet_ids"
}

data "aws_ssm_parameter" "launch_control_alb_listener_arn" {
  name = "/launch-control/alb/https_listener_arn"
}

# Get ALB security group by name (consistent with launch-control naming)
data "aws_security_group" "launch_control_alb" {
  name = "launch-control-alb-sg"
}

locals {
  # Parse comma-separated subnet IDs from SSM
  private_subnet_ids = split(",", data.aws_ssm_parameter.launch_control_private_subnet_ids.value)
}

# S3 Bucket with basic security
module "s3_bucket" {
  source               = "./modules/s3"
  bucket_name          = "${var.project_name}-assets-${random_id.bucket_suffix.hex}"
  enable_encryption    = true
  block_public_access  = true
  enable_versioning    = true
}

# ECS Cluster with Fargate Spot for cost savings
module "ecs_cluster" {
  source                   = "./modules/ecs"
  cluster_name             = var.project_name
  enable_container_insights = false  # Disable to save costs
  capacity_providers       = ["FARGATE", "FARGATE_SPOT"]
}

# ECR Repository for Backend with lifecycle policy
module "ecr_backend" {
  source                 = "./modules/ecr"
  repository_name        = "${var.project_name}-backend"
  enable_lifecycle_policy = true
  max_image_count        = 5
  enable_scan_on_push    = true
}

# ECR Repository for Frontend with lifecycle policy
module "ecr_frontend" {
  source                 = "./modules/ecr"
  repository_name        = "${var.project_name}-frontend"
  enable_lifecycle_policy = true
  max_image_count        = 5
  enable_scan_on_push    = true
}

# S3 bucket for frontend static hosting
module "s3_frontend" {
  source                   = "./modules/s3"
  bucket_name              = "${var.project_name}-frontend-${random_id.bucket_suffix.hex}"
  enable_website           = true
  enable_cloudfront_policy = true
  block_public_access      = true
}

# CloudFront distribution for frontend
module "cloudfront" {
  source                = "./modules/cloudfront"
  s3_bucket_domain_name = module.s3_frontend.bucket_regional_domain_name
  s3_bucket_id          = module.s3_frontend.bucket_id
  project_name          = var.project_name
  price_class           = "PriceClass_100"  # Cheapest
}

resource "random_id" "bucket_suffix" {
  byte_length = 4
}

# Database password for ping-pong-app
resource "random_password" "db_password" {
  length  = 16
  special = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

# RDS PostgreSQL for ping-pong-app
module "rds_postgres" {
  source = "./modules/rds"
  
  name                   = var.project_name
  postgres_version       = "15.8"
  db_instance_class      = var.db_instance_class
  allocated_storage      = var.db_allocated_storage
  max_allocated_storage  = var.db_max_allocated_storage
  
  database_name          = "pingpongdb"
  master_username        = "postgres"
  master_password        = random_password.db_password.result
  
  backup_retention_period = 1  # Minimum for cost optimization
  skip_final_snapshot     = false
  deletion_protection     = true
  
  performance_insights_enabled = var.enable_performance_insights
  monitoring_interval          = var.db_monitoring_interval
  
  # Use launch-control's VPC
  vpc_id             = data.aws_ssm_parameter.launch_control_vpc_id.value
  private_subnet_ids = local.private_subnet_ids
}

# ECS Service for ping-pong-app using launch-control ALB
module "ping_pong_service" {
  depends_on = [module.ecs_cluster, module.ecr_backend, module.rds_postgres]
  source      = "./modules/service"
  project_name = var.project_name
  db_password  = random_password.db_password.result
  db_host      = module.rds_postgres.db_instance_endpoint
  db_name      = module.rds_postgres.db_instance_name
  
  # Pass launch-control infrastructure references
  vpc_id                = data.aws_ssm_parameter.launch_control_vpc_id.value
  private_subnet_ids    = local.private_subnet_ids
  alb_listener_arn      = data.aws_ssm_parameter.launch_control_alb_listener_arn.value
  alb_security_group_id = data.aws_security_group.launch_control_alb.id
  
  # OAuth secrets
  google_client_secret = var.google_client_secret
}

# Outputs
output "s3_bucket_name" {
  value = module.s3_bucket.bucket_id
}

output "ecs_cluster_name" {
  value = module.ecs_cluster.cluster_name
}

output "ecr_backend_url" {
  value = module.ecr_backend.repository_url
}

output "ecr_frontend_url" {
  value = module.ecr_frontend.repository_url
}

output "frontend_cloudfront_domain" {
  description = "CloudFront distribution domain for frontend"
  value       = module.cloudfront.distribution_domain_name
}

output "frontend_s3_bucket" {
  description = "S3 bucket name for frontend static files"
  value       = module.s3_frontend.bucket_id
}