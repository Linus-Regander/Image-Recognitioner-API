provider "aws" {
  region = var.region

  access_key = var.local_mode ? "test" : null
  secret_key = var.local_mode ? "test" : null

  skip_credentials_validation = var.local_mode
  skip_requesting_account_id  = var.local_mode
  skip_metadata_api_check     = var.local_mode
  s3_use_path_style           = var.local_mode

  dynamic "endpoints" {
    for_each = var.local_mode ? [1] : []
    content {
      dynamodb = var.local_endpoint
      s3       = var.local_endpoint
    }
  }
}

resource "aws_dynamodb_table" "images" {
  name         = var.dynamodb_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "imageId"

  attribute {
    name = "imageId"
    type = "S"
  }

  attribute {
    name = "contentHash"
    type = "S"
  }

  global_secondary_index {
    name            = "contentHash-index"
    hash_key        = "contentHash"
    projection_type = "ALL"
  }

  point_in_time_recovery {
    enabled = !var.local_mode
  }
}

resource "random_id" "bucket_suffix" {
  byte_length = 4
}

resource "aws_s3_bucket" "images" {
  bucket = var.local_mode ? var.s3_bucket_name : "${var.s3_bucket_name}-${random_id.bucket_suffix.hex}"
}

resource "aws_s3_bucket_public_access_block" "images" {
  bucket                  = aws_s3_bucket.images.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "images" {
  bucket = aws_s3_bucket.images.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}