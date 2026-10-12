#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../backend"
TOOLS_DIR=${CHATDEMO_TOOLS_DIR:-/workspace/tools}
exec "$TOOLS_DIR/jdk-21.0.9+10/bin/java" -Dspring.web.resources.static-locations=file:../frontend/dist/ -jar target/business-query-0.0.1-SNAPSHOT.jar
