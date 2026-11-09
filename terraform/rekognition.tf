locals {
  create_app_policy = !var.local_mode
}

# Optional: only needed for face indexing/search, not for moderation or labels
resource "aws_rekognition_collection" "faces" {
  count = var.enable_face_collection ? 1 : 0

  collection_id = "${var.project_name}-faces"
}

# Permissions for whatever runs the app (ECS task role, EC2 role, etc.).
# Real AWS only: LocalStack doesn't enforce IAM.
data "aws_iam_policy_document" "app" {
  count = local.create_app_policy ? 1 : 0

  # Rekognition's detect APIs don't support resource-level permissions
  statement {
    sid       = "RekognitionDetect"
    actions   = ["rekognition:DetectModerationLabels", "rekognition:DetectLabels"]
    resources = ["*"]
  }

  # Rekognition reads S3 objects with the caller's credentials,
  # so the app needs s3:GetObject as well
  statement {
    sid       = "ImageObjects"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["${aws_s3_bucket.images.arn}/*"]
  }

  statement {
    sid = "ImageMetadata"
    actions = [
      "dynamodb:GetItem",
      "dynamodb:PutItem",
      "dynamodb:DeleteItem",
      "dynamodb:Query",
      "dynamodb:Scan",
    ]
    resources = [
      aws_dynamodb_table.images.arn,
      "${aws_dynamodb_table.images.arn}/index/*",
    ]
  }

  dynamic "statement" {
    for_each = var.enable_face_collection ? [1] : []
    content {
      sid       = "FaceCollection"
      actions   = ["rekognition:IndexFaces", "rekognition:SearchFacesByImage", "rekognition:DeleteFaces"]
      resources = [aws_rekognition_collection.faces[0].arn]
    }
  }
}

resource "aws_iam_policy" "app" {
  count = local.create_app_policy ? 1 : 0

  name   = "${var.project_name}-app"
  policy = data.aws_iam_policy_document.app[0].json
}