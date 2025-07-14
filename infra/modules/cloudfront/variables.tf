variable "s3_bucket_domain_name" {
  description = "Domain name of the S3 bucket hosting the frontend"
  type        = string
}

variable "s3_bucket_id" {
  description = "ID of the S3 bucket hosting the frontend"
  type        = string
}

variable "project_name" {
  description = "Name of the project"
  type        = string
}

variable "price_class" {
  description = "CloudFront price class"
  type        = string
  default     = "PriceClass_100"  # Cheapest option
}

variable "enable_ipv6" {
  description = "Enable IPv6 for the distribution"
  type        = bool
  default     = true
}

variable "default_ttl" {
  description = "Default TTL for objects in seconds"
  type        = number
  default     = 86400  # 1 day
}

variable "max_ttl" {
  description = "Maximum TTL for objects in seconds"
  type        = number
  default     = 31536000  # 1 year
}

variable "min_ttl" {
  description = "Minimum TTL for objects in seconds"
  type        = number
  default     = 0
}