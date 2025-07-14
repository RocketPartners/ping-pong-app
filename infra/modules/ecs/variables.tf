variable "cluster_name" {
  description = "The name of the ECS cluster"
  type        = string
}

variable "enable_container_insights" {
  description = "Enable CloudWatch Container Insights (adds cost)"
  type        = bool
  default     = false
}

variable "capacity_providers" {
  description = "List of capacity providers to use"
  type        = list(string)
  default     = ["FARGATE", "FARGATE_SPOT"]
}