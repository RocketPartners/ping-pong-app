variable "project_name" {
  description = "The name of the project"
  type        = string
}

variable "service_name" {
  description = "The name of the ECS service"
  type        = string
  default     = "ping-pong"
}

variable "service_security_group_id" {
  description = "Security group ID to attach to the ECS service"
  type        = string
  default     = "sg-0104a2c86541c46ab"
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "vpc_id" {
  description = "VPC ID from launch-control infrastructure"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs from launch-control infrastructure"
  type        = list(string)
}

variable "alb_listener_arn" {
  description = "ALB listener ARN from launch-control infrastructure"
  type        = string
}

variable "alb_security_group_id" {
  description = "ALB security group ID from launch-control infrastructure"
  type        = string
}

variable "google_client_secret" {
  description = "Google OAuth client secret"
  type        = string
  sensitive   = true
}

variable "microsoft_client_id" {
  description = "Microsoft OAuth client ID"
  type        = string
}

variable "microsoft_client_secret" {
  description = "Microsoft OAuth client secret"
  type        = string
  sensitive   = true
}