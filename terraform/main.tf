terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC and Networking
resource "aws_vpc" "techcellshop" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "techcellshop-vpc"
  }
}

resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.techcellshop.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "techcellshop-private-${count.index + 1}"
  }
}

resource "aws_subnet" "public" {
  count             = 2
  vpc_id            = aws_vpc.techcellshop.id
  cidr_block        = var.public_subnet_cidrs[count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "techcellshop-public-${count.index + 1}"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.techcellshop.id

  tags = {
    Name = "techcellshop-igw"
  }
}

resource "aws_nat_gateway" "main" {
  count             = 2
  allocation_id     = aws_eip.nat[count.index].id
  subnet_id         = aws_subnet.public[count.index].id
  depends_on        = [aws_internet_gateway.main]

  tags = {
    Name = "techcellshop-nat-${count.index + 1}"
  }
}

resource "aws_eip" "nat" {
  count  = 2
  domain = "vpc"

  tags = {
    Name = "techcellshop-eip-${count.index + 1}"
  }
}

# Route Tables
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.techcellshop.id

  route {
    cidr_block      = "0.0.0.0/0"
    gateway_id      = aws_internet_gateway.main.id
  }

  tags = {
    Name = "techcellshop-public-rt"
  }
}

resource "aws_route_table" "private" {
  count  = 2
  vpc_id = aws_vpc.techcellshop.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[count.index].id
  }

  tags = {
    Name = "techcellshop-private-rt-${count.index + 1}"
  }
}

resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = 2
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

# Data source for availability zones
data "aws_availability_zones" "available" {
  state = "available"
}

# Security Groups
resource "aws_security_group" "alb" {
  name        = "techcellshop-alb-sg"
  description = "Security group for ALB"
  vpc_id      = aws_vpc.techcellshop.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "techcellshop-alb-sg"
  }
}

resource "aws_security_group" "app" {
  name        = "techcellshop-app-sg"
  description = "Security group for application"
  vpc_id      = aws_vpc.techcellshop.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "techcellshop-app-sg"
  }
}

resource "aws_security_group" "db" {
  name        = "techcellshop-db-sg"
  description = "Security group for database"
  vpc_id      = aws_vpc.techcellshop.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "techcellshop-db-sg"
  }
}

resource "aws_security_group" "rabbitmq" {
  name        = "techcellshop-rabbitmq-sg"
  description = "Security group for RabbitMQ"
  vpc_id      = aws_vpc.techcellshop.id

  ingress {
    from_port       = 5672
    to_port         = 5672
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  ingress {
    from_port       = 15672
    to_port         = 15672
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "techcellshop-rabbitmq-sg"
  }
}

# PostgreSQL RDS
resource "aws_db_subnet_group" "main" {
  name       = "techcellshop-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "techcellshop-db-subnet-group"
  }
}

resource "aws_rds_cluster" "main" {
  cluster_identifier              = "techcellshop-cluster"
  engine                          = "aurora-postgresql"
  engine_version                  = "15.4"
  database_name                   = var.postgres_db_name
  master_username                 = var.postgres_user
  master_password                 = var.postgres_password
  db_subnet_group_name            = aws_db_subnet_group.main.name
  vpc_security_group_ids          = [aws_security_group.db.id]
  backup_retention_period         = 7
  preferred_backup_window         = "03:00-04:00"
  skip_final_snapshot             = var.skip_final_snapshot
  final_snapshot_identifier       = "techcellshop-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"
  enabled_cloudwatch_logs_exports = ["postgresql"]
  storage_encrypted               = true

  tags = {
    Name = "techcellshop-postgres"
  }
}

resource "aws_rds_cluster_instance" "main" {
  count              = var.db_instance_count
  cluster_identifier = aws_rds_cluster.main.id
  instance_class     = var.db_instance_class
  engine              = aws_rds_cluster.main.engine
  engine_version      = aws_rds_cluster.main.engine_version

  performance_insights_enabled = true

  tags = {
    Name = "techcellshop-postgres-${count.index + 1}"
  }
}

# RabbitMQ on EC2
resource "aws_instance" "rabbitmq" {
  count                = var.deploy_rabbitmq_on_ec2 ? 1 : 0
  ami                  = data.aws_ami.amzn2_ami.id
  instance_type        = var.rabbitmq_instance_type
  subnet_id            = aws_subnet.private[0].id
  vpc_security_group_ids = [aws_security_group.rabbitmq.id]
  iam_instance_profile = aws_iam_instance_profile.rabbitmq[0].name

  user_data = base64encode(templatefile("${path.module}/rabbitmq-init.sh", {
    RABBITMQ_USER     = var.rabbitmq_user
    RABBITMQ_PASSWORD = var.rabbitmq_password
  }))

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 30
    delete_on_termination = true
    encrypted             = true
  }

  tags = {
    Name = "techcellshop-rabbitmq"
  }

  depends_on = [aws_nat_gateway.main]
}

# RabbitMQ IAM Role
resource "aws_iam_role" "rabbitmq" {
  count = var.deploy_rabbitmq_on_ec2 ? 1 : 0
  name  = "techcellshop-rabbitmq-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_instance_profile" "rabbitmq" {
  count = var.deploy_rabbitmq_on_ec2 ? 1 : 0
  name  = "techcellshop-rabbitmq-profile"
  role  = aws_iam_role.rabbitmq[0].name
}

resource "aws_iam_role_policy" "rabbitmq_ssm" {
  count  = var.deploy_rabbitmq_on_ec2 ? 1 : 0
  name   = "techcellshop-rabbitmq-ssm"
  role   = aws_iam_role.rabbitmq[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:UpdateInstanceInformation",
          "ssmmessages:AcknowledgeMessage",
          "ssmmessages:GetEndpoint",
          "ssmmessages:GetMessages",
          "ec2messages:AcknowledgeMessage",
          "ec2messages:GetEndpoint",
          "ec2messages:GetMessages"
        ]
        Resource = "*"
      }
    ]
  })
}

# Data source for Amazon Linux 2
data "aws_ami" "amzn2_ami" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# ECS Cluster and Task Definition
resource "aws_ecs_cluster" "main" {
  name = "techcellshop-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name = "techcellshop-cluster"
  }
}

resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/techcellshop"
  retention_in_days = 7

  tags = {
    Name = "techcellshop-ecs-logs"
  }
}

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "techcellshop-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_ecs_task_definition" "app" {
  family                   = "techcellshop"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.ecs_task_cpu
  memory                   = var.ecs_task_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name      = "techcellshop"
      image     = var.docker_image_uri
      essential = true
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]
      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = "prod"
        },
        {
          name  = "SERVER_PORT"
          value = "8080"
        },
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${aws_rds_cluster.main.endpoint}:5432/${var.postgres_db_name}"
        },
        {
          name  = "SPRING_DATASOURCE_USERNAME"
          value = var.postgres_user
        },
        {
          name  = "SPRING_DATASOURCE_DRIVER_CLASS_NAME"
          value = "org.postgresql.Driver"
        },
        {
          name  = "SPRING_JPA_DATABASE_PLATFORM"
          value = "org.hibernate.dialect.PostgreSQLDialect"
        },
        {
          name  = "SPRING_RABBITMQ_HOST"
          value = var.deploy_rabbitmq_on_ec2 ? aws_instance.rabbitmq[0].private_ip : var.rabbitmq_host
        },
        {
          name  = "SPRING_RABBITMQ_PORT"
          value = "5672"
        },
        {
          name  = "SPRING_RABBITMQ_USERNAME"
          value = var.rabbitmq_user
        },
        {
          name  = "SPRING_RABBITMQ_VIRTUAL_HOST"
          value = var.rabbitmq_vhost
        }
      ]
      secrets = [
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.db_password.arn}:password::"
        },
        {
          name      = "SPRING_RABBITMQ_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.rabbitmq_password.arn}:password::"
        },
        {
          name      = "SECURITY_JWT_SECRET"
          valueFrom = "${aws_secretsmanager_secret.jwt_secret.arn}:secret::"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  tags = {
    Name = "techcellshop-task"
  }
}

# ECS Service
resource "aws_ecs_service" "app" {
  name            = "techcellshop-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.ecs_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "techcellshop"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.http]

  tags = {
    Name = "techcellshop-service"
  }
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "techcellshop-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  enable_deletion_protection = false

  tags = {
    Name = "techcellshop-alb"
  }
}

resource "aws_lb_target_group" "app" {
  name        = "techcellshop-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.techcellshop.id
  target_type = "ip"

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    path                = "/actuator/health"
    matcher             = "200"
  }

  tags = {
    Name = "techcellshop-tg"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

# Secrets Manager
resource "aws_secretsmanager_secret" "db_password" {
  name                    = "techcellshop/db-password"
  recovery_window_in_days = 7

  tags = {
    Name = "techcellshop-db-password"
  }
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = jsonencode({
    password = var.postgres_password
  })
}

resource "aws_secretsmanager_secret" "rabbitmq_password" {
  name                    = "techcellshop/rabbitmq-password"
  recovery_window_in_days = 7

  tags = {
    Name = "techcellshop-rabbitmq-password"
  }
}

resource "aws_secretsmanager_secret_version" "rabbitmq_password" {
  secret_id     = aws_secretsmanager_secret.rabbitmq_password.id
  secret_string = jsonencode({
    password = var.rabbitmq_password
  })
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "techcellshop/jwt-secret"
  recovery_window_in_days = 7

  tags = {
    Name = "techcellshop-jwt-secret"
  }
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = jsonencode({
    secret = var.jwt_secret
  })
}

# Auto Scaling
resource "aws_appautoscaling_target" "ecs_target" {
  max_capacity       = var.ecs_max_count
  min_capacity       = var.ecs_desired_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.app.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_policy_cpu" {
  name               = "techcellshop-cpu-autoscaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}

resource "aws_appautoscaling_policy" "ecs_policy_memory" {
  name               = "techcellshop-memory-autoscaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }
    target_value = 80.0
  }
}
