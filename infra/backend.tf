terraform {
  backend "s3" {
    bucket         = "ping-pong-app-terraform-state"
    key            = "terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "launch_control_terraform_state"
    encrypt        = true
  }
}