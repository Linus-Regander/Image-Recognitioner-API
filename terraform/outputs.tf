output "dynamodb_table_name" {
  value = aws_dynamodb_table.images.name
}

output "s3_bucket_name" {
  value = aws_s3_bucket.images.bucket
}

output "region" {
  value = var.region
}

output "cognito_user_pool_id" {
  value = one(aws_cognito_user_pool.main[*].id)
}

output "cognito_client_id" {
  value = one(aws_cognito_user_pool_client.api[*].id)
}

output "rekognition_collection_id" {
  value = one(aws_rekognition_collection.faces[*].collection_id)
}

output "app_policy_arn" {
  value = one(aws_iam_policy.app[*].arn)
}