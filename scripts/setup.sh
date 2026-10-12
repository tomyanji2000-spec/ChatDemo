#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)
TOOLS_DIR=${CHATDEMO_TOOLS_DIR:-/workspace/tools}
mkdir -p "$TOOLS_DIR"
if [ ! -x "$TOOLS_DIR/jdk-21.0.9+10/bin/javac" ]; then
  jdk_tmp=$(mktemp -d)
  jdk_base='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.9%2B10'
  jdk_archive='OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10.tar.gz'
  curl -fsSL "$jdk_base/$jdk_archive" -o "$jdk_tmp/jdk.tar.gz"
  curl -fsSL "$jdk_base/$jdk_archive.sha256.txt" -o "$jdk_tmp/jdk.sha256"
  python3 - "$jdk_tmp" <<'PYJDK'
import hashlib,pathlib,sys
p=pathlib.Path(sys.argv[1])
assert hashlib.sha256((p/'jdk.tar.gz').read_bytes()).hexdigest()==(p/'jdk.sha256').read_text().split()[0], 'JDK checksum mismatch'
PYJDK
  tar -xzf "$jdk_tmp/jdk.tar.gz" -C "$TOOLS_DIR"
  rm -rf "$jdk_tmp"
fi
export JAVA_HOME="$TOOLS_DIR/jdk-21.0.9+10"
export PATH="$JAVA_HOME/bin:$PATH"
if [ -f /etc/ssl/certs/java/cacerts ]; then
  export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
if [ ! -x "$TOOLS_DIR/apache-maven-3.9.11/bin/mvn" ]; then
  tmp_dir=$(mktemp -d)
  trap 'rm -rf "$tmp_dir"' EXIT
  base=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11
  curl -fsSL "$base/apache-maven-3.9.11-bin.tar.gz" -o "$tmp_dir/maven.tar.gz"
  curl -fsSL "$base/apache-maven-3.9.11-bin.tar.gz.sha512" -o "$tmp_dir/maven.sha512"
  python3 - "$tmp_dir" <<'PY'
import hashlib,pathlib,sys
p=pathlib.Path(sys.argv[1])
assert hashlib.sha512((p/'maven.tar.gz').read_bytes()).hexdigest()==(p/'maven.sha512').read_text().strip(), 'Maven checksum mismatch'
PY
  tar -xzf "$tmp_dir/maven.tar.gz" -C "$TOOLS_DIR"
fi
python3 - "$TOOLS_DIR/maven-settings.xml" <<'PY'
import os,sys,urllib.parse,xml.etree.ElementTree as E
root=E.Element('settings')
u=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY') or '')
if u.hostname:
 if u.username or u.password: raise SystemExit('Configure authenticated Maven proxy separately; no credentials are saved by this script.')
 p=E.SubElement(E.SubElement(root,'proxies'),'proxy')
 for k,v in [('id','cloud'),('active','true'),('protocol','http'),('host',u.hostname),('port',str(u.port or 80)),('nonProxyHosts','localhost|127.0.0.1')]: E.SubElement(p,k).text=v
E.ElementTree(root).write(sys.argv[1],encoding='unicode')
PY
cd "$PROJECT_DIR/frontend"
npm ci --cache "$TOOLS_DIR/npm-cache" --no-audit --no-fund
npm run build
cd "$PROJECT_DIR/backend"
"$TOOLS_DIR/apache-maven-3.9.11/bin/mvn" -U -s "$TOOLS_DIR/maven-settings.xml" -Dmaven.repo.local="$TOOLS_DIR/m2" verify
