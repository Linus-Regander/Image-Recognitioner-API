locals {
  # Cognito (and its pre-token Lambda) only exists on real AWS.
  # Locally the app runs with the "local" Spring profile (no auth).
  create_cognito = !var.local_mode
}

data "archive_file" "pre_token_generation" {
  count = local.create_cognito ? 1 : 0

  type        = "zip"
  source_file = "${path.module}/lambda/pre_token_generation.py"
  output_path = "${path.module}/pre_token_generation.zip"
}

resource "aws_iam_role" "pre_token_generation" {
  count = local.create_cognito ? 1 : 0

  name = "${var.project_name}-pre-token-generation"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "pre_token_generation_logs" {
  count = local.create_cognito ? 1 : 0

  role       = aws_iam_role.pre_token_generation[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_lambda_function" "pre_token_generation" {
  count = local.create_cognito ? 1 : 0

  function_name    = "${var.project_name}-pre-token-generation"
  role             = aws_iam_role.pre_token_generation[0].arn
  runtime          = "python3.12"
  handler          = "pre_token_generation.handler"
  filename         = data.archive_file.pre_token_generation[0].output_path
  source_code_hash = data.archive_file.pre_token_generation[0].output_base64sha256
}

resource "local_file" "app_environment" {
  count = local.create_cognito ? 1 : 0

  filename = "${path.module}/cognito.env"
  content  = <<-EOT
    SPRING_PROFILES_ACTIVE=prod
    APP_SECURITY_LOCAL_AUTH_ENABLED=false
    APP_AWS_REGION=${var.region}
    APP_AWS_DYNAMO_DB_TABLE_NAME=${var.dynamodb_table_name}
    APP_AWS_S3_BUCKET_NAME=${aws_s3_bucket.images.bucket}
    APP_AWS_DYNAMO_DB_ENDPOINT=
    APP_AWS_S3_ENDPOINT=
    APP_AWS_S3_PUBLIC_ENDPOINT=
    APP_AWS_REKOGNITION_ENDPOINT=
    COGNITO_USER_POOL_ID=${aws_cognito_user_pool.main[0].id}
    COGNITO_CLIENT_ID=${aws_cognito_user_pool_client.api[0].id}
    COGNITO_ISSUER=https://cognito-idp.${var.region}.amazonaws.com/${aws_cognito_user_pool.main[0].id}
    COGNITO_JWK_SET_URI=https://cognito-idp.${var.region}.amazonaws.com/${aws_cognito_user_pool.main[0].id}/.well-known/jwks.json
  EOT
}

resource "aws_cognito_user_pool" "main" {
  count = local.create_cognito ? 1 : 0

  name                     = "${var.project_name}-users"
  user_pool_tier           = "ESSENTIALS"
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]
  deletion_protection      = "ACTIVE"

  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  password_policy {
    minimum_length    = 12
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = true
  }

  schema {
    name                     = "tenant_id"
    attribute_data_type      = "String"
    developer_only_attribute = false
    mutable                  = true
    required                 = false

    string_attribute_constraints {
      min_length = 1
      max_length = 64
    }
  }

  lambda_config {
    pre_token_generation_config {
      lambda_arn     = aws_lambda_function.pre_token_generation[0].arn
      lambda_version = "V2_0"
    }
  }
}

resource "aws_cognito_user_pool_client" "api" {
  count = local.create_cognito ? 1 : 0

  name            = "${var.project_name}-api"
  user_pool_id    = aws_cognito_user_pool.main[0].id
  generate_secret = false

  explicit_auth_flows = [
    "ALLOW_USER_SRP_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
  ]

  prevent_user_existence_errors = "ENABLED"

  access_token_validity  = 60
  id_token_validity      = 60
  refresh_token_validity = 30

  token_validity_units {
    access_token  = "minutes"
    id_token      = "minutes"
    refresh_token = "days"
  }

  read_attributes  = ["email", "custom:tenant_id"]
  write_attributes = ["email"]
}

resource "aws_lambda_permission" "cognito_invoke" {
  count = local.create_cognito ? 1 : 0

  statement_id  = "AllowCognitoInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.pre_token_generation[0].function_name
  principal     = "cognito-idp.amazonaws.com"
  source_arn    = aws_cognito_user_pool.main[0].arn
}