output "vpc_id" {
  description = "The ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "List of public subnet IDs"
  value       = [
    aws_subnet.public_1.id,
    aws_subnet.public_2.id
  ]
}

output "private_subnet_ids" {
  description = "List of private subnet IDs"
  value       = [
    aws_subnet.private_1.id,
    aws_subnet.private_2.id
  ]
}

output "public_subnet_arns" {
  description = "List of public subnet ARNs"
  value       = [
    aws_subnet.public_1.arn,
    aws_subnet.public_2.arn
  ]
}

output "private_subnet_arns" {
  description = "List of private subnet ARNs"
  value       = [
    aws_subnet.private_1.arn,
    aws_subnet.private_2.arn
  ]
}

output "availability_zones" {
  description = "List of Availability Zones used"
  value = [
    aws_subnet.public_1.availability_zone,
    aws_subnet.public_2.availability_zone
  ]
}
