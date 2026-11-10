#!/usr/bin/env bash
# Arranca el API RRHH en :8080 vía LaunchAgent (sobrevive al cierre del IDE/terminal).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LABEL="pe.andina.rrhh.backend"
PLIST="$HOME/Library/LaunchAgents/${LABEL}.plist"
LOG="/tmp/rrhh-backend.log"
UID_NUM="$(id -u)"

health() {
  curl -sf -o /dev/null --max-time 2 http://127.0.0.1:8080/api/configuracion/publica
}

print_addrs() {
  echo "Backend listo en http://127.0.0.1:8080 (log: $LOG)"
  _lan="$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)"
  if [[ -n "${_lan:-}" ]]; then
    echo "Celular en WiFi: http://${_lan}:8080"
  fi
  echo "USB: adb reverse tcp:8080 tcp:8080 y usa http://127.0.0.1:8080"
}

if health; then
  echo "Backend ya activo en http://127.0.0.1:8080"
  print_addrs
  exit 0
fi

cd "$ROOT/backend"
JAR="$(ls -1 target/rrhh-api-*.jar 2>/dev/null | head -1 || true)"
if [[ -z "$JAR" || ! -f "$JAR" ]]; then
  echo "Compilando backend (primera vez puede tardar)..."
  mvn -q -DskipTests package
  JAR="$(ls -1 target/rrhh-api-*.jar | head -1)"
fi
JAR_ABS="$(cd "$(dirname "$JAR")" && pwd)/$(basename "$JAR")"

JAVA_BIN="$(command -v java)"
if [[ -x /usr/libexec/java_home ]]; then
  _jh="$("/usr/libexec/java_home" 2>/dev/null || true)/bin/java"
  [[ -x "$_jh" ]] && JAVA_BIN="$_jh"
fi

mkdir -p "$HOME/Library/LaunchAgents"
cat > "$PLIST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>${LABEL}</string>
  <key>ProgramArguments</key>
  <array>
    <string>${JAVA_BIN}</string>
    <string>-jar</string>
    <string>${JAR_ABS}</string>
  </array>
  <key>WorkingDirectory</key>
  <string>${ROOT}/backend</string>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>StandardOutPath</key>
  <string>${LOG}</string>
  <key>StandardErrorPath</key>
  <string>${LOG}</string>
  <key>EnvironmentVariables</key>
  <dict>
    <key>PATH</key>
    <string>/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin</string>
  </dict>
</dict>
</plist>
EOF

launchctl bootout "gui/${UID_NUM}/${LABEL}" 2>/dev/null || true
launchctl bootstrap "gui/${UID_NUM}" "$PLIST"
launchctl enable "gui/${UID_NUM}/${LABEL}" 2>/dev/null || true
launchctl kickstart -k "gui/${UID_NUM}/${LABEL}"

for i in $(seq 1 90); do
  if health; then
    print_addrs
    exit 0
  fi
  sleep 1
done
echo "No se pudo levantar el backend. Revisa $LOG"
exit 1
