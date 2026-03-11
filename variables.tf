variable "db_password" {
  description = "RDS root user password"
  type        = string
  sensitive   = true
}

variable "s3_bucket_name" {
  description = "S3 bucket name for image uploads (globally unique)"
  type        = string
  default     = "cluverse-images"
}