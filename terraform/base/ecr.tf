# 이미지 저장소는 destroy 대상이 아니므로 base에 둔다 (test destroy 시에도 이미지 유지).
resource "aws_ecr_repository" "api" {
  name                 = "cluverse-api"
  image_tag_mutability = "MUTABLE"

  tags = { Name = "cluverse-api" }
}

# 부하 테스트용 이미지가 쌓이지 않도록 최근 10개만 유지
resource "aws_ecr_lifecycle_policy" "api" {
  repository = aws_ecr_repository.api.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}
