#!/usr/bin/env bash
# Backend + adb reverse para login desde celular USB (http://127.0.0.1:8080)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"

"$ROOT/scripts/start-backend.sh"

if [[ -x "$ADB" ]]; then
  if "$ADB" devices 2>/dev/null | awk 'NR>1 && $2=="device"{found=1} END{exit !found}'; then
    "$ADB" reverse tcp:8080 tcp:8080
    echo "adb reverse tcp:8080 → OK"
    "$ADB" reverse --list
  else
    echo "Sin dispositivo Android por USB. En WiFi usa la IP LAN que muestra start-backend.sh"
  fi
else
  echo "adb no encontrado en $ADB (opcional si usas WiFi + IP LAN)"
fi

echo
echo "Credenciales de prueba: juan.espinoza / Andina2026"
echo "En la app: Servidor http://127.0.0.1:8080 (USB) o la IP LAN (WiFi)"
