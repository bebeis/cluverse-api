#!/bin/bash
set -euxo pipefail
exec > /var/log/user-data.log 2>&1

# ---------- Redis 설치 (AL2023 기본 저장소의 redis6) ----------
dnf -y install redis6

# SG(redis_sg)로 접근을 제한하므로 인증 없이 VPC 내부 접근 허용.
sed -i 's/^bind .*/bind 0.0.0.0/' /etc/redis6/redis6.conf
sed -i 's/^protected-mode .*/protected-mode no/' /etc/redis6/redis6.conf

systemctl enable --now redis6

# ---------- redis_exporter (9121) ----------
RE_VERSION=1.62.0
curl -fsSL -o /tmp/redis_exporter.tar.gz "https://github.com/oliver006/redis_exporter/releases/download/v$RE_VERSION/redis_exporter-v$RE_VERSION.linux-amd64.tar.gz"
tar -xzf /tmp/redis_exporter.tar.gz -C /tmp
install -m 755 "/tmp/redis_exporter-v$RE_VERSION.linux-amd64/redis_exporter" /usr/local/bin/redis_exporter

cat > /etc/systemd/system/redis_exporter.service <<'EOF'
[Unit]
Description=Prometheus Redis Exporter
After=redis6.service

[Service]
User=nobody
ExecStart=/usr/local/bin/redis_exporter --redis.addr=redis://localhost:6379
Restart=always

[Install]
WantedBy=multi-user.target
EOF

# ---------- node_exporter (9100) ----------
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
systemctl enable --now redis_exporter node_exporter
