terraform {
  backend "s3" {
    bucket  = "ping-pong-app-terraform-state"
    key     = "terraform.tfstate"
    region  = "us-east-1"
    encrypt = true
  }
}