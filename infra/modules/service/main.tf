locals {
  private_subnet_ids = var.private_subnet_ids
  ecs_cluster_name = "${var.project_name}"
  ecs_cluster_arn = "arn:aws:ecs:us-east-1:231544473980:cluster/${var.project_name}"
  ecr_repository_name = "${var.project_name}-backend"
  ecr_repository_url = "231544473980.dkr.ecr.us-east-1.amazonaws.com/${local.ecr_repository_name}"
}



# ECR Repo, ECS Cluster, Task Def, Service, TG, attach to Listener

# Task Def
resource "aws_ecs_task_definition" "main" {
  family                   = "${var.project_name}-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_executor.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "${var.project_name}-container"
      image     = "${local.ecr_repository_url}:latest"
      essential = true
      environment = [
        {
          name  = "DB_URL"
          value = "jdbc:postgresql://${var.db_host}:5432/${var.db_name}"
        },
        {
          name  = "DB_USERNAME"
          value = "postgres"
        },
        {
          name  = "DB_PASSWORD"
          value = var.db_password
        },
        {
          name  = "FRONTEND_URL"
          value = "https://ping-pong.rcktapp.io"
        },
        {
          name  = "JWT_SECRET"
          value = "your-jwt-secret-key-that-should-be-at-least-64-characters-long-for-hs512-algorithm-compatibility"
        },
        {
          name  = "GOOGLE_CLIENT_ID"
          value = "1038430235167-gcqmuf9q8i009l0e2e6kokhspb4ifca2.apps.googleusercontent.com"
        },
        {
          name  = "GOOGLE_CLIENT_SECRET"
          value = var.google_client_secret
        }
      ]
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "/ecs/${var.project_name}"
          awslogs-region        = "us-east-1"
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

# Service Def
resource "aws_ecs_service" "main" {
  name            = "${var.project_name}-service"
  cluster         = local.ecs_cluster_arn
  task_definition = aws_ecs_task_definition.main.arn
  launch_type     = "FARGATE"
  desired_count   = 1 

  network_configuration {
    subnets          = local.private_subnet_ids
    security_groups  = [var.service_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.service_http.arn
    container_name   = "${var.project_name}-container"
    container_port   = 8080
  }

  depends_on = [
    aws_ecs_task_definition.main,
    aws_lb_target_group.service_http
  ]
}

# Target Group 
resource "aws_lb_target_group" "service_http" {
  name        = "${var.project_name}-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip" 

  health_check {
    protocol = "HTTP"
    path     = "/api/actuator/health"
    matcher  = "200-399"
    interval = 60
    timeout  = 15
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }

  tags = {
    Name    = "${var.project_name}-tg"
    Project = var.project_name
  }
}

# Listener Rule
resource "aws_lb_listener_rule" "host_based" {
  listener_arn = var.alb_listener_arn
  priority     = 3

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.service_http.arn
  }

  condition {
    host_header {
      values = ["ping-pong.rcktapp.io"]
    }
  }
}

# Log Group
resource "aws_cloudwatch_log_group" "ecs_service" {
  name              = "/ecs/${var.project_name}"
  retention_in_days = 14

  tags = {
    Name    = "${var.project_name}-logs"
    Project = var.project_name
  }
}
