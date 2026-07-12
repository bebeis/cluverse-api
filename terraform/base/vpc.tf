locals {
  azs           = ["${var.aws_region}a", "${var.aws_region}c"]
  public_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
  private_cidrs = ["10.0.11.0/24", "10.0.12.0/24"]
}

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = { Name = "cluverse-vpc" }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "cluverse-igw" }
}

resource "aws_subnet" "public" {
  count = length(local.public_cidrs)

  vpc_id                  = aws_vpc.main.id
  cidr_block              = local.public_cidrs[count.index]
  availability_zone       = local.azs[count.index]
  map_public_ip_on_launch = true

  tags = { Name = "cluverse-public-${local.azs[count.index]}" }
}

resource "aws_subnet" "private" {
  count = length(local.private_cidrs)

  vpc_id            = aws_vpc.main.id
  cidr_block        = local.private_cidrs[count.index]
  availability_zone = local.azs[count.index]

  tags = { Name = "cluverse-private-${local.azs[count.index]}" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "cluverse-public-rt" }
}

resource "aws_route" "public_internet" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.main.id
}

resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# 프라이빗 라우팅 테이블은 base에서 "빈 채로" 만든다.
# egress(0.0.0.0/0 → NAT) route는 test 스택이 이 테이블 ID를 받아 추가한다.
# NAT를 test에 두는 이유: apply/destroy 반복 시 NAT idle 비용을 없애기 위함.
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "cluverse-private-rt" }
}

resource "aws_route_table_association" "private" {
  count = length(aws_subnet.private)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
