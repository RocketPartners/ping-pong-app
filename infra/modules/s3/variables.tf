variable "bucket_name" {
  description = "The name of the S3 bucket"
  type        = string
}

variable "force_destroy" {
  description = "Allow Terraform to destroy the bucket even if it contains objects"
  type        = bool
  default     = true
}

variable "enable_versioning" {
  description = "Enable S3 bucket versioning"
  type        = bool
  default     = false
}

variable "enable_encryption" {
  description = "Enable S3 bucket encryption"
  type        = bool
  default     = true
}

variable "block_public_access" {
  description = "Block public access to the bucket"
  type        = bool
  default     = true
}

variable "enable_website" {
  description = "Enable S3 website hosting"
  type        = bool
  default     = false
}

variable "enable_cloudfront_policy" {
  description = "Enable CloudFront access policy"
  type        = bool
  default     = false
}

variable "cloudfront_distribution_arn" {
  description = "CloudFront distribution ARN for bucket policy"
  type        = string
  default     = ""
}