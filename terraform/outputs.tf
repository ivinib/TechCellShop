output "alb_endpoint" {
  description = "ALB DNS endpoint"
  value       = aws_lb.main.dns_name
}

output "rds_cluster_endpoint" {
  description = "RDS cluster endpoint"
  value       = aws_rds_cluster.main.endpoint
}

output "rds_cluster_reader_endpoint" {
  description = "RDS cluster reader endpoint"
  value       = aws_rds_cluster.main.reader_endpoint
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.app.name
}

output "rabbitmq_private_ip" {
  description = "RabbitMQ EC2 instance private IP"
  value       = var.deploy_rabbitmq_on_ec2 ? aws_instance.rabbitmq[0].private_ip : "N/A"
}

output "cloudwatch_log_group" {
  description = "CloudWatch log group name"
  value       = aws_cloudwatch_log_group.ecs.name
}