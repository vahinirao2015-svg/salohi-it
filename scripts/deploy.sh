#!/bin/bash
set -euo pipefail
if [ "$#" -ne 2 ]; then
  echo "usage: scripts/deploy.sh HOST /path/to/private-key" >&2
  exit 1
fi
HOST_ADDRESS="$1"
KEY_PATH="$2"
mvn -B -f hrms/pom.xml -DskipTests package
scp -i "$KEY_PATH" -o StrictHostKeyChecking=accept-new hrms/target/hrms.jar "ubuntu@${HOST_ADDRESS}:/tmp/hrms.jar"
ssh -i "$KEY_PATH" -o StrictHostKeyChecking=accept-new "ubuntu@${HOST_ADDRESS}" "sudo /opt/hrms/deploy-app.sh /tmp/hrms.jar"
