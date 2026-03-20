#!/bin/bash
awslocal s3 mb s3://cluverse-images
awslocal s3api put-bucket-cors \
  --bucket cluverse-images \
  --cors-configuration '{
    "CORSRules": [
      {
        "AllowedOrigins": [
          "http://localhost:3000",
          "https://cluverse-web.vercel.app"
        ],
        "AllowedMethods": ["GET", "HEAD", "PUT"],
        "AllowedHeaders": ["*"],
        "ExposeHeaders": ["ETag"],
        "MaxAgeSeconds": 3000
      }
    ]
  }'
echo "S3 bucket 'cluverse-images' created."
