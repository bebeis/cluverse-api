#!/bin/bash
set -euxo pipefail
exec > /var/log/user-data.log 2>&1

# ---------- Docker + compose plugin ----------
dnf -y install docker
systemctl enable --now docker
mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL -o /usr/local/lib/docker/cli-plugins/docker-compose \
  "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64"
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

mkdir -p /opt/monitoring/grafana/provisioning/datasources

# ---------- Prometheus 설정 ----------
# - mysqld/redis exporter는 고정 IP로 static scrape
# - node_exporter는 EC2 SD(tag:NodeExporter=true)로 자동 발견 (ECS ASG 노드 포함)
# - 앱(/actuator/prometheus)은 bridge+동적 호스트 포트라 static 설정이 불가.
#   앱 레벨 메트릭까지 보려면 (1) hostPort를 8080 고정(인스턴스당 1태스크로 제한)하거나
#   (2) ECS 태스크 디스커버리(prometheus-ecs-discovery 등)를 붙일 것. 지금은 exporter 중심 구성.
cat > /opt/monitoring/prometheus.yml <<'EOF'
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: mysqld
    static_configs:
      - targets: ["${mysql_ip}:9104"]

  - job_name: redis
    static_configs:
      - targets: ["${redis_ip}:9121"]

  - job_name: node
    ec2_sd_configs:
      - region: ${aws_region}
        port: 9100
        filters:
          - name: tag:NodeExporter
            values: ["true"]
          - name: instance-state-name
            values: ["running"]
    relabel_configs:
      - source_labels: [__meta_ec2_tag_Name]
        target_label: instance_name
EOF

# ---------- Grafana 데이터소스 자동 등록 ----------
cat > /opt/monitoring/grafana/provisioning/datasources/prometheus.yml <<'EOF'
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://localhost:9090
    isDefault: true
EOF

# ---------- docker compose ----------
# host 네트워크: ec2_sd가 인스턴스 프로파일(IMDS) 자격증명을 그대로 쓰고, 포트 매핑도 단순해진다.
cat > /opt/monitoring/docker-compose.yml <<'EOF'
services:
  prometheus:
    image: prom/prometheus:v2.53.0
    network_mode: host
    restart: unless-stopped
    volumes:
      - /opt/monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prom_data:/prometheus

  grafana:
    image: grafana/grafana:11.1.0
    network_mode: host
    restart: unless-stopped
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin # bastion 터널로만 접근 가능하므로 기본값 유지
    volumes:
      - grafana_data:/var/lib/grafana
      - /opt/monitoring/grafana/provisioning/datasources:/etc/grafana/provisioning/datasources:ro

volumes:
  prom_data:
  grafana_data:
EOF

docker compose -f /opt/monitoring/docker-compose.yml up -d

# ---------- node_exporter (9100, 모니터링 호스트 자신) ----------
NE_VERSION=1.8.2
curl -fsSL -o /tmp/node_exporter.tar.gz "https://github.com/prometheus/node_exporter/releases/download/v$NE_VERSION/node_exporter-$NE_VERSION.linux-amd64.tar.gz"
tar -xzf /tmp/node_exporter.tar.gz -C /tmp
install -m 755 "/tmp/node_exporter-$NE_VERSION.linux-amd64/node_exporter" /usr/local/bin/node_exporter

cat > /etc/systemd/system/node_exporter.service <<'EOF'
[Unit]
Description=Prometheus Node Exporter
After=network.target

[Service]
User=nobody
ExecStart=/usr/local/bin/node_exporter --web.listen-address=:9100
Restart=always

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now node_exporter
