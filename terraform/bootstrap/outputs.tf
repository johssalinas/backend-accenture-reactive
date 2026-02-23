output "tfstate_bucket_name" {
  description = "Terraform state S3 bucket"
  value       = aws_s3_bucket.tfstate.bucket
}

output "tf_lock_table_name" {
  description = "Terraform lock DynamoDB table"
  value       = aws_dynamodb_table.tf_lock.name
}

output "tfstate_kms_key_arn" {
  description = "KMS key used for tfstate"
  value       = aws_kms_key.tfstate.arn
}
