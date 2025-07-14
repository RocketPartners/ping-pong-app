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