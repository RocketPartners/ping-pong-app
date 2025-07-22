data "aws_caller_identity" "current" {}

# Generic ECS Execution Role
resource "aws_iam_role" "ecs_executor" {
  name               = "ECS.executor.${var.service_name}"
  assume_role_policy = data.aws_iam_policy_document.ecs_executor_arp.json
}

data "aws_iam_policy_document" "ecs_executor_arp" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type = "Service"
      identifiers = [
        "ecs-tasks.amazonaws.com",
        "scheduler.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role_policy_attachment" "ecs_executor_managed_policy" {
  role       = aws_iam_role.ecs_executor.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_executor_secret_access" {
  name   = "ecs-executor-permissions"
  policy = data.aws_iam_policy_document.ecs_executor_secret_access.json
  role   = aws_iam_role.ecs_executor.name
}

data "aws_iam_policy_document" "ecs_executor_secret_access" {
  statement {
  sid    = "AllowReadAccessToSecrets"
  effect = "Allow"
  actions = [
    "secretsmanager:GetResourcePolicy",
    "secretsmanager:GetSecretValue",
    "secretsmanager:DescribeSecret",
    "secretsmanager:ListSecretVersionIds",
    "ssm:GetParametersByPath",
    "ssm:GetParameters",
    "ssm:GetParameter",
  ]
  resources = ["*"]
}

  statement {
    sid    = "ListSecrets"
    effect = "Allow"
    actions = [
      "secretsmanager:ListSecrets",
      "ssm:DescribeParameters",
    ]
    resources = ["*"]
  }
}

# Generic ECS Task Role
resource "aws_iam_role" "ecs_task" {
  name               = "ECS.task.${var.service_name}"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_arp.json
}

data "aws_iam_policy_document" "ecs_task_arp" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }
  }
}