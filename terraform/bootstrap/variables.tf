variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-2"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "accenture"
}

variable "environment" {
  description = "Environment"
  type        = string
  default     = "dev"
}

variable "tfstate_bucket_name" {
  description = "S3 bucket name for remote terraform state"
  type        = string
}

variable "tf_lock_table_name" {
  description = "DynamoDB table name for terraform lock"
  type        = string
  default     = "terraform-locks"
}
