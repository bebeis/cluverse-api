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

variable "s3_allowed_origins" {
  description = "Allowed origins for browser-based S3 uploads"
  type        = list(string)
  default = [
    "https://cluverse-web.vercel.app",
    "http://localhost:3000"
  ]
}
