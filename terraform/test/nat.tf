# NAT Gateway를 test에 두는 이유:
#   프라이빗 인스턴스들이 user_data로 MySQL/Redis/Docker/exporter를 인터넷에서 설치하므로
#   egress가 필요하다. NAT를 test에 두면 destroy 시 함께 사라져 idle 비용(시간당 요금)이 없다.
#
# 대안(트레이드오프): 모든 소프트웨어를 미리 구워 둔 커스텀 AMI(Packer 등)를 쓰면
#   NAT 없이도 프라이빗 인스턴스를 띄울 수 있다. 반복 테스트가 잦으면 부팅이 수 분 → 수십 초로
#   줄고 NAT 요금도 없어 더 빠르고 싸다. 대신 AMI 빌드 파이프라인 유지 비용이 생긴다.
#   지금은 "백지 계정에서 바로 실행 가능"을 우선해 NAT + user_data 설치를 택했다.
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = { Name = "cluverse-nat-eip" }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = local.public_subnet_ids[0] # 단일 NAT (테스트 용도, AZ 이중화 불필요)

  tags = { Name = "cluverse-nat" }
}

# base가 빈 채로 만들어 둔 프라이빗 라우팅 테이블에 egress route를 추가한다.
# test destroy 시 이 route와 NAT가 함께 사라지고, 라우팅 테이블 자체는 base에 남는다.
resource "aws_route" "private_nat" {
  route_table_id         = local.base.private_route_table_id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.main.id
}
