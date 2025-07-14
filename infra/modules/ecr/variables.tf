variable "repository_name" {
  description = "The name of the ECR repository"
  type        = string
}

variable "force_delete" {
  description = "If true, will delete the repository even if it contains images"
  type        = bool
  default     = true
}

variable "enable_lifecycle_policy" {
  description = "Enable lifecycle policy to control storage costs"
  type        = bool
  default     = true
}

variable "max_image_count" {
  description = "Maximum number of images to keep"
  type        = number
  default     = 5
}

variable "enable_scan_on_push" {
  description = "Enable image scanning on push (uses free tier)"
  type        = bool
  default     = true
}