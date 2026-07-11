output "bastion_public_ip" {
  description = "Bastion EIP"
  value       = aws_eip.bastion.public_ip
}

output "nat_public_ip" {
  description = "NAT Gateway 공인 IP"
  value       = aws_eip.nat.public_ip
}

output "mysql_private_ip" {
  description = "MySQL 고정 사설 IP"
  value       = aws_instance.mysql.private_ip
}

output "redis_private_ip" {
  description = "Redis 고정 사설 IP"
  value       = aws_instance.redis.private_ip
}

output "monitoring_private_ip" {
  description = "Prometheus/Grafana 사설 IP"
  value       = aws_instance.monitoring.private_ip
}

output "ecs_cluster_name" {
  description = "ECS 클러스터 이름"
  value       = aws_ecs_cluster.main.name
}

# 로컬 → bastion → 프라이빗 리소스 SSH 터널.
# 로컬에서 실행 후: Grafana http://localhost:3000, Prometheus http://localhost:9090,
# MySQL localhost:3306, Redis localhost:6379 로 접근.
output "ssh_tunnel_command" {
  description = "Grafana/Prometheus/MySQL/Redis 터널 명령 (ssh_public_key를 넣었을 때)"
  value = join(" ", [
    "ssh -N",
    "-L 3000:${aws_instance.monitoring.private_ip}:3000",
    "-L 9090:${aws_instance.monitoring.private_ip}:9090",
    "-L 3306:${aws_instance.mysql.private_ip}:3306",
    "-L 6379:${aws_instance.redis.private_ip}:6379",
    "ec2-user@${aws_eip.bastion.public_ip}",
  ])
}

# SSM Session Manager 대안 — SSH 키/22 포트 없이 포트포워딩 (bastion 경유 불필요)
output "ssm_port_forward_examples" {
  description = "SSM 포트포워딩 예시 (인스턴스에 직접 연결)"
  value = {
    grafana    = "aws ssm start-session --target ${aws_instance.monitoring.id} --document-name AWS-StartPortForwardingSession --parameters '{\"portNumber\":[\"3000\"],\"localPortNumber\":[\"3000\"]}'"
    prometheus = "aws ssm start-session --target ${aws_instance.monitoring.id} --document-name AWS-StartPortForwardingSession --parameters '{\"portNumber\":[\"9090\"],\"localPortNumber\":[\"9090\"]}'"
    mysql      = "aws ssm start-session --target ${aws_instance.bastion.id} --document-name AWS-StartPortForwardingSessionToRemoteHost --parameters '{\"host\":[\"${aws_instance.mysql.private_ip}\"],\"portNumber\":[\"3306\"],\"localPortNumber\":[\"3306\"]}'"
    redis      = "aws ssm start-session --target ${aws_instance.bastion.id} --document-name AWS-StartPortForwardingSessionToRemoteHost --parameters '{\"host\":[\"${aws_instance.redis.private_ip}\"],\"portNumber\":[\"6379\"],\"localPortNumber\":[\"6379\"]}'"
  }
}
