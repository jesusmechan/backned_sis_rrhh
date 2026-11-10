#!/usr/bin/env bash
# Lanzado por LaunchAgent / osascript; no depende de la sesión del IDE.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG="/tmp/rrhh-backend.log"
PIDFILE="/tmp/rrhh-backend.pid"
cd "$ROOT/backend"
JAR="$(ls -1 target/rrhh-api-*.jar 2>/dev/null | head -1)"
[[ -n "$JAR" && -f "$JAR" ]] || { echo "Falta el JAR. Ejecuta mvn -DskipTests package en backend/" >&2; exit 1; }

if curl -sf -o /dev/null --max-time 2 http://127.0.0.1:8080/api/configuracion/publica; then
  echo "Ya activo"
  exit 0
fi

if [[ -f "$PIDFILE" ]]; then
  old="$(cat "$PIDFILE" 2>/dev/null || true)"
  if [[ -n "${old:-}" ]] && kill -0 "$old" 2>/dev/null; then
    kill "$old" 2>/dev/null || true
    sleep 1
  fi
fi
lsof -tiTCP:8080 -sTCP:LISTEN 2>/dev/null | xargs kill 2>/dev/null || true
sleep 1

JAVA_BIN="$(command -v java)"
if [[ -x "/usr/libexec/java_home" ]]; then
  HOME_JAVA="$("/usr/libexec/java_home" 2>/dev/null || true)/bin/java"
  [[ -x "$HOME_JAVA" ]] && JAVA_BIN="$HOME_JAVA"
fi

: > "$LOG"
nohup "$JAVA_BIN" -jar "$JAR" >> "$LOG" 2>&1 < /dev/null &
echo $! > "$PIDFILE"
exit 0
