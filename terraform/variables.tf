variable "region" {
  type    = string
  default = "eu-north-1"
}

variable "local_mode" {
  description = "Use LocalStack instead of AWS. Keep local and AWS resources in separate Terraform workspaces."
  type        = bool
  default     = false
}

variable "local_endpoint" {
  type    = string
  default = "http://localhost:4566"
}

variable "dynamodb_table_name" {
  type    = string
  default = "image-metadata"
}

variable "s3_bucket_name" {
  description = "Globally unique S3 bucket name"
  type        = string
  default     = "image-recognitioner"
}

variable "project_name" {
  type    = string
  default = "image-recognitioner"
}

variable "enable_face_collection" {
  description = "Create a Rekognition face collection (only needed for face indexing/search)"
  type        = bool
  default     = false
}