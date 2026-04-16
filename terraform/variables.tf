variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}

# Database variables
variable "postgres_db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "techcellshop"
  sensitive   = true
}

variable "postgres_user" {
  description = "PostgreSQL username"
  type        = string
  default     = "techcellshop"
  sensitive   = true
}

variable "postgres_password" {
  description = "PostgreSQL password"
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.small"
}

variable "db_instance_count" {
  description = "Number of RDS instances in the cluster"
  type        = number
  default     = 2
}

variable "skip_final_snapshot" {
  description = "Skip final snapshot on deletion"
  type        = bool
  default     = false
}

# RabbitMQ variables
variable "deploy_rabbitmq_on_ec2" {
  description = "Deploy RabbitMQ on EC2 (true) or use external service (false)"
  type        = bool
  default     = true
}

variable "rabbitmq_instance_type" {
  description = "EC2 instance type for RabbitMQ"
  type        = string
  default     = "t3.small"
}

variable "rabbitmq_user" {
  description = "RabbitMQ username"
  type        = string
  default     = "admin"
  sensitive   = true
}

variable "rabbitmq_password" {
  description = "RabbitMQ password"
  type        = string
  sensitive   = true
}

variable "rabbitmq_vhost" {
  description = "RabbitMQ virtual host"
  type        = string
  default     = "/"
}

variable "rabbitmq_host" {
  description = "RabbitMQ host (used if deploy_rabbitmq_on_ec2 is false)"
  type        = string
  default     = ""
}

# Application variables
variable "docker_image_uri" {
  description = "Docker image URI for the application"
  type        = string
}

variable "ecs_task_cpu" {
  description = "ECS task CPU units"
  type        = string
  default     = "512"
}

variable "ecs_task_memory" {
  description = "ECS task memory in MB"
  type        = string
  default     = "1024"
}

variable "ecs_desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
  default     = 2
}

variable "ecs_max_count" {
  description = "Maximum number of ECS tasks for auto scaling"
  type        = number
  default     = 4
}

# Security variables
variable "jwt_secret" {
  description = "JWT secret key"
  type        = string
  sensitive   = true
}