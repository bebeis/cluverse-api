#!/bin/bash
set -euxo pipefail
exec > /var/log/user-data.log 2>&1

# ---------- MySQL 8.0 설치 (AL2023 = EL9 호환, MySQL 공식 repo) ----------
rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql-2023
dnf -y install https://repo.mysql.com/mysql80-community-release-el9.rpm
dnf -y install mysql-community-server

# RDS Performance Insights를 수동 재현하기 위한 설정:
#   slow_query_log + 낮은 long_query_time, performance_schema ON
cat > /etc/my.cnf.d/zz-cluverse.cnf <<'EOF'
[mysqld]
bind-address = 0.0.0.0
slow_query_log = 1
slow_query_log_file = /var/lib/mysql/slow.log
long_query_time = 0.2
log_slow_admin_statements = 1
performance_schema = ON
EOF

systemctl enable --now mysqld

# 기동 대기
for i in $(seq 1 60); do
  mysqladmin ping --silent && break
  sleep 2
done

# 비밀번호는 SSM Parameter Store에서 조회 (user_data 평문 노출 방지).
# node 롤의 AmazonSSMManagedInstanceCore에 ssm:GetParameter 포함.
# set +x: 비밀번호가 /var/log/user-data.log에 트레이스로 남지 않도록 이 구간은 제외
set +x
DB_PASSWORD=""
for i in $(seq 1 12); do
  DB_PASSWORD=$(aws ssm get-parameter \
    --region ${aws_region} \
    --name '${db_password_ssm_key}' \
    --with-decryption \
    --query Parameter.Value \
    --output text) && break
  echo "SSM 비밀번호 조회 재시도 ($i/12)"
  sleep 5
done
test -n "$DB_PASSWORD" || { echo "SSM 비밀번호 조회 실패"; exit 1; }

# RPM 설치 시 root 임시 비밀번호가 로그에 남는다
TMP_PW=$(grep 'temporary password' /var/log/mysqld.log | tail -1 | awk '{print $NF}')

# RPM 설치는 validate_password 컴포넌트를 MEDIUM 정책(특수문자 요구)으로 활성화하므로,
# 영숫자 전용 db_password는 그대로 못 쓴다. 만료 비밀번호 세션에서는 ALTER USER만 허용되니
# 정책을 충족하는 부트스트랩 비밀번호로 먼저 바꾼 뒤 컴포넌트를 제거하고 실제 비밀번호를 설정한다.
BOOT_PW='TmpBoot#2026aA'
mysql --connect-expired-password -uroot -p"$TMP_PW" \
  -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$BOOT_PW';"
# 컴포넌트가 없는 환경이면 이 명령만 실패하고 넘어간다 (테스트 스택 전용 DB라 정책 불필요)
mysql -uroot -p"$BOOT_PW" \
  -e "UNINSTALL COMPONENT 'file://component_validate_password';" || true

# 앱 DB/유저 + exporter 전용 제한 계정 생성
# (비밀번호는 변수 validation으로 영숫자만 허용 → SQL/cnf 이스케이프 불필요)
mysql -uroot -p"$BOOT_PW" <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
CREATE DATABASE IF NOT EXISTS ${db_name} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${db_username}'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON ${db_name}.* TO '${db_username}'@'%';
-- 부하테스트 락 관찰용 (script/view-count/explain/lock-waits.sql — sys.innodb_lock_waits 등)
-- sys 뷰는 SECURITY INVOKER라 하위 p_s SELECT + PROCESS + 내부 함수 EXECUTE가 모두 필요하다
GRANT PROCESS ON *.* TO '${db_username}'@'%';
GRANT SELECT ON performance_schema.* TO '${db_username}'@'%';
GRANT SELECT, EXECUTE ON sys.* TO '${db_username}'@'%';
CREATE USER IF NOT EXISTS 'exporter'@'localhost' IDENTIFIED BY '$DB_PASSWORD' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'localhost';
FLUSH PRIVILEGES;
SQL

# exporter 접속 정보도 같은 비-트레이스 구간에서 기록
cat > /etc/mysqld_exporter.cnf <<EOF
[client]
user = exporter
password = "$DB_PASSWORD"
EOF
chmod 600 /etc/mysqld_exporter.cnf
set -x

# ---------- mysqld_exporter (9104, 제한 권한 exporter 계정 사용) ----------
ME_VERSION=0.15.1
curl -fsSL -o /tmp/mysqld_exporter.tar.gz "https://github.com/prometheus/mysqld_exporter/releases/download/v$ME_VERSION/mysqld_exporter-$ME_VERSION.linux-amd64.tar.gz"
tar -xzf /tmp/mysqld_exporter.tar.gz -C /tmp
install -m 755 "/tmp/mysqld_exporter-$ME_VERSION.linux-amd64/mysqld_exporter" /usr/local/bin/mysqld_exporter

cat > /etc/systemd/system/mysqld_exporter.service <<'EOF'
[Unit]
Description=Prometheus MySQL Exporter
After=mysqld.service

[Service]
ExecStart=/usr/local/bin/mysqld_exporter --config.my-cnf=/etc/mysqld_exporter.cnf --web.listen-address=:9104
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
systemctl enable --now mysqld_exporter node_exporter
