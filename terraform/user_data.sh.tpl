#!/bin/bash
# Terraform fills in the database password. Shell variables use a single dollar sign.
set -euo pipefail
exec > /var/log/hrms-bootstrap.log 2>&1

export DEBIAN_FRONTEND=noninteractive
timedatectl set-timezone Asia/Kolkata || true

apt-get update
apt-get install -y openjdk-17-jdk openjdk-21-jre-headless maven docker.io curl ca-certificates gnupg unzip fontconfig
apt-get install -y docker-compose-v2 || true
systemctl enable --now docker

if ! docker compose version >/dev/null 2>&1; then
  mkdir -p /usr/local/lib/docker/cli-plugins
  curl -fsSL -o /usr/local/lib/docker/cli-plugins/docker-compose \
    https://github.com/docker/compose/releases/download/v2.32.4/docker-compose-linux-x86_64
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

sysctl -w vm.max_map_count=262144
echo 'vm.max_map_count=262144' > /etc/sysctl.d/99-sonarqube.conf

mkdir -p /data
DATA_DEV=""
for _ in $(seq 1 40); do
  DATA_DEV=$(lsblk -b -dn -o NAME,SIZE | awk '$2 > 40000000000 {print "/dev/"$1; exit}')
  if [ -n "$DATA_DEV" ]; then
    break
  fi
  sleep 3
done

if [ -n "$DATA_DEV" ]; then
  if ! blkid "$DATA_DEV" >/dev/null 2>&1; then
    mkfs.ext4 -L hrms-data "$DATA_DEV"
  fi
  UUID=$(blkid -s UUID -o value "$DATA_DEV")
  grep -q "$UUID" /etc/fstab || echo "UUID=$UUID /data ext4 defaults,nofail 0 2" >> /etc/fstab
  mount -a || mount "$DATA_DEV" /data
fi

mkdir -p /data/postgres /data/sonarqube/data /data/sonarqube/logs /data/sonarqube/extensions /opt/hrms
chown -R 999:999 /data/postgres
chown -R 1000:1000 /data/sonarqube

if ! id hrms >/dev/null 2>&1; then
  useradd --system --home /opt/hrms --shell /usr/sbin/nologin hrms
fi
chown hrms:hrms /opt/hrms

install -d -m 755 /etc/hrms
cat > /etc/hrms/hrms.env <<EOF
TZ=Asia/Kolkata
SERVER_PORT=8081
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/hrms
SPRING_DATASOURCE_USERNAME=hrms
SPRING_DATASOURCE_PASSWORD=${db_password}
EOF
chmod 600 /etc/hrms/hrms.env

cat > /opt/hrms/init-db.sql <<EOF
CREATE USER sonar WITH PASSWORD '${db_password}';
CREATE DATABASE sonarqube OWNER sonar;
EOF

cat > /opt/hrms/docker-compose.yml <<EOF
name: salohi
services:
  postgres:
    image: postgres:16
    restart: unless-stopped
    environment:
      POSTGRES_USER: hrms
      POSTGRES_PASSWORD: ${db_password}
      POSTGRES_DB: hrms
    ports:
      - "127.0.0.1:5432:5432"
    volumes:
      - /data/postgres:/var/lib/postgresql/data
      - /opt/hrms/init-db.sql:/docker-entrypoint-initdb.d/01-init.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U hrms -d hrms"]
      interval: 5s
      timeout: 5s
      retries: 30
  sonarqube:
    image: sonarqube:community
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://postgres:5432/sonarqube
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: ${db_password}
      SONAR_ES_BOOTSTRAP_CHECKS_DISABLE: "true"
      SONAR_WEB_JAVAOPTS: "-Xmx512m -Xms128m"
      SONAR_CE_JAVAOPTS: "-Xmx512m -Xms128m"
    ports:
      - "9000:9000"
    volumes:
      - /data/sonarqube/data:/opt/sonarqube/data
      - /data/sonarqube/logs:/opt/sonarqube/logs
      - /data/sonarqube/extensions:/opt/sonarqube/extensions
    ulimits:
      nofile:
        soft: 65536
        hard: 65536
EOF

rm -f /etc/apt/sources.list.d/jenkins.list
if curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2026.key -o /usr/share/keyrings/jenkins-keyring.asc; then
  echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" > /etc/apt/sources.list.d/jenkins.list
  if apt-get update && apt-get install -y jenkins; then
    usermod -aG docker jenkins || true
    mkdir -p /etc/systemd/system/jenkins.service.d
    printf '[Service]\nEnvironment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"\n' > /etc/systemd/system/jenkins.service.d/java.conf
    systemctl daemon-reload
    systemctl enable --now jenkins || true
  else
    rm -f /etc/apt/sources.list.d/jenkins.list
    apt-get update || true
    echo "Jenkins was not installed. Java, Maven, Docker, and the portal are still available."
  fi
else
  echo "Jenkins repository was not reachable. Java, Maven, and Docker were still installed."
fi

docker compose -f /opt/hrms/docker-compose.yml up -d || echo "Containers did not start. Re-run: docker compose -f /opt/hrms/docker-compose.yml up -d"

cat > /etc/systemd/system/hrms.service <<'UNIT'
[Unit]
Description=Salohi IT HRMS
After=docker.service
Wants=docker.service

[Service]
User=hrms
WorkingDirectory=/opt/hrms
EnvironmentFile=/etc/hrms/hrms.env
ExecStart=/usr/lib/jvm/java-17-openjdk-amd64/bin/java -jar /opt/hrms/hrms.jar
Restart=on-failure
RestartSec=15

[Install]
WantedBy=multi-user.target
UNIT

cat > /opt/hrms/deploy-app.sh <<'DEPLOY'
#!/bin/bash
set -euo pipefail
if [ "$#" -ne 1 ]; then
  echo "usage: deploy-app.sh /path/to/hrms.jar" >&2
  exit 1
fi
install -o hrms -g hrms -m 644 "$1" /opt/hrms/hrms.jar
systemctl enable hrms
systemctl restart hrms
systemctl --no-pager --full status hrms
DEPLOY
chmod 755 /opt/hrms/deploy-app.sh
chmod 600 /opt/hrms/docker-compose.yml /opt/hrms/init-db.sql

systemctl enable --now snap.amazon-ssm-agent.amazon-ssm-agent.service || systemctl enable --now amazon-ssm-agent || true

echo "Salohi IT bootstrap finished"
