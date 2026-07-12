#!/bin/bash
set -euxo pipefail
exec > /var/log/user-data.log 2>&1

# ECS agent를 클러스터에 등록
cat >> /etc/ecs/ecs.config <<EOF
ECS_CLUSTER=${cluster_name}
EOF

# node_exporter — 호스트 CPU steal / T계열 크레딧 소진을 앱 병목과 분리 관측
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
