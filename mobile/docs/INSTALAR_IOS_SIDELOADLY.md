# Instalar Andina RR. HH. en iPhone (Windows + Sideloadly)

Guía para compañeros que tienen **PC Windows** e **iPhone**.  
No hace falta Mac ni cuenta de desarrollador de pago.

---

## Qué vas a necesitar

| Requisito | Detalle |
|-----------|---------|
| PC | Windows 10 u 11 |
| Cable | USB / Lightning o USB‑C (el del iPhone) |
| iPhone | iOS **15.0** o superior |
| Apple ID | El tuyo (el de iCloud). Solo se usa para firmar la app en tu teléfono |
| Programa | [Sideloadly](https://sideloadly.io/) (gratis) |
| Archivo | `andina-rrhh-ios-v1.0.0.ipa` (~9 MB) |

**API de la app:** `https://rrhh-api-2bgd.onrender.com`  
(ya viene configurada; también se puede cambiar en Perfil → Configuración).

---

## 1. Descargar el IPA

Pídele a Jesús el archivo:

`andina-rrhh-ios-v1.0.0.ipa`

O descárgalo desde el repositorio (cuando esté en GitHub):

`https://github.com/jesusmechan/backned_sis_rrhh/raw/main/mobile/releases/andina-rrhh-ios-v1.0.0.ipa`

Guárdalo en una carpeta fácil de encontrar, por ejemplo `Descargas`.

> **Importante:** este IPA **no se instala abriéndolo en Safari** como un APK de Android. Hay que usar Sideloadly (pasos siguientes).

---

## 2. Instalar Sideloadly en Windows

1. Entra a **[https://sideloadly.io/](https://sideloadly.io/)**.
2. Descarga la versión **Windows**.
3. Ejecuta el instalador y completa la instalación.
4. Si Windows Defender o SmartScreen avisa, elige **Más información** → **Ejecutar de todas formas** (es habitual con este tipo de herramientas).

Opcional pero recomendado: instala también **iTunes** para Windows (versión de la Microsoft Store o la de Apple). Sideloadly lo usa para hablar con el iPhone por USB.

---

## 3. Preparar el iPhone

1. Desbloquea el iPhone.
2. Conéctalo al PC con el cable.
3. Si aparece **¿Confiar en este ordenador?**, pulsa **Confiar** e introduce el código del iPhone.
4. Deja la pantalla encendida durante la instalación.

---

## 4. Instalar la app con Sideloadly

1. Abre **Sideloadly**.
2. Arrastra el archivo `andina-rrhh-ios-v1.0.0.ipa` a la ventana (o usa el botón para seleccionar IPA).
3. En el desplegable de dispositivo, elige tu **iPhone** (debe aparecer el nombre del equipo).
4. En **Apple Account**, escribe tu **Apple ID** (correo de iCloud).
5. Pulsa **Start** / **Iniciar**.
6. Cuando pida la contraseña del Apple ID, introdúcela.
   - Si tienes **autenticación en dos factores**, Apple enviará un código a tus dispositivos; introdúcelo cuando Sideloadly lo pida.
7. Espera a que termine (barra de progreso). Puede tardar varios minutos la primera vez.

Cuando termine, en el iPhone debería aparecer la app **Andina Rrhh**.

---

## 5. Confiar en el certificado (obligatorio la primera vez)

Si al abrir la app sale *“No se puede verificar la integridad de la app”* o similar:

1. En el iPhone ve a **Ajustes**.
2. **General**.
3. **VPN y gestión de dispositivos** (en iOS antiguos: *Perfiles y gestión de dispositivos*).
4. Bajo **App para desarrolladores**, toca tu **Apple ID** / el perfil que aparece.
5. Pulsa **Confiar en “…”** → confirma **Confiar**.
6. Vuelve al escritorio y abre **Andina Rrhh**.

---

## 6. Iniciar sesión (usuarios de prueba)

Contraseña de todos: **`Andina2026`**

| Usuario | Rol |
|---------|-----|
| `juan.espinoza` | EMPLEADO |
| `jesus.pantoja` | JEFE |
| `carla.reyes` | RRHH |
| `jesus.mechan` | ADMIN |

La primera vez el backend en Render puede tardar ~30–60 s en “despertar” (plan free). Si el login falla, espera un minuto y reintenta.

---

## Caducidad: cada ~7 días hay que reinstalar

Con Apple ID **gratis**, Apple hace que la firma caduque aproximadamente a los **7 días**.

**Síntoma:** la app deja de abrir o dice que el desarrollador ya no es de confianza.

**Solución:** repetir solo el **paso 4** (Sideloadly + mismo IPA + mismo Apple ID). No hace falta volver a descargar Sideloadly.

Consejo: anota en el calendario “Renovar Andina RRHH” cada domingo, por ejemplo.

---

## Problemas frecuentes

### Sideloadly no detecta el iPhone
- Prueba otro cable (algunos solo cargan, no transmiten datos).
- Desbloquea el iPhone y pulsa **Confiar**.
- Cierra Sideloadly, abre iTunes/Finder una vez para que Windows reconozca el dispositivo, vuelve a abrir Sideloadly.
- Prueba otro puerto USB del PC.

### Error de Apple ID / “an app is already installed” / límite de apps
- Apple limita cuántas apps puedes firmar con cuenta gratis.
- Borra en el iPhone otras apps instaladas por Sideloadly/AltStore que ya no uses e inténtalo de nuevo.

### “Unable to install” / error de firma
- Comprueba que el IPA no esté corrupto (vuelve a descargarlo).
- Asegúrate de usar tu Apple ID correcto (correo completo).
- Actualiza Sideloadly a la última versión desde sideloadly.io.

### La app se instala pero no conecta al servidor
- Revisa Wi‑Fi / datos móviles.
- En la app: **Perfil → Configuración → Servidor API**  
  Debe ser: `https://rrhh-api-2bgd.onrender.com`  
  (sin barra final).
- Si Render está dormido, espera 1 minuto y reintenta el login.

### ¿Puedo instalar solo desde el iPhone, sin PC?
No con este método. Hace falta Sideloadly en el PC y el cable USB.  
(Alternativas oficiales como TestFlight requieren cuenta Apple Developer de pago.)

---

## Resumen rápido

```
1. Descargar andina-rrhh-ios-v1.0.0.ipa
2. Instalar Sideloadly en Windows
3. Conectar iPhone por USB → Confiar
4. Sideloadly: IPA + Apple ID → Start
5. Ajustes → General → VPN y gestión de dispositivos → Confiar
6. Abrir Andina Rrhh → login de prueba
7. Cada ~7 días: repetir paso 4
```

---

## Datos técnicos (referencia)

| Campo | Valor |
|-------|--------|
| Nombre en el teléfono | Andina Rrhh |
| Bundle ID | `pe.andina.andinaRrhh` |
| Versión | 1.0.0 (build 1) |
| iOS mínimo | 15.0 |
| Arquitectura | arm64 (iPhone modernos) |
| Firma del IPA del repo | Sin firmar (Sideloadly firma al instalar) |
| API embebida | `https://rrhh-api-2bgd.onrender.com` |

Documentación general de la app: [`../README.md`](../README.md).
