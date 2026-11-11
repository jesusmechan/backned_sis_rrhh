# Scripts locales (macOS)

**No son necesarios para Neon, Render ni Cloudflare Pages.** Solo ayudan a levantar el backend en tu Mac sin depender del IDE.

| Script | Uso |
|--------|-----|
| `start-backend.sh` | Compila si falta el JAR y registra un LaunchAgent en `:8080` |
| `_run-backend-daemon.sh` | Invocado por el LaunchAgent |
| `prep-mobile-login.sh` | Backend + `adb reverse` para Flutter por USB |

En la nube el backend escucha el puerto que asigne **Render** (`PORT`); el front en Pages no usa puerto `5173` (solo HTTPS). En local, `5173` y `8080` siguen siendo los defaults de Vite y Spring.
