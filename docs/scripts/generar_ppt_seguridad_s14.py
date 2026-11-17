#!/usr/bin/env python3
"""Genera la presentación profesional S14 — Pruebas de Seguridad RRHH Andina."""

from __future__ import annotations

from pathlib import Path

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.oxml.ns import nsmap
from pptx.oxml import parse_xml
from pptx.util import Emu, Inches, Pt

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "S14_Presentacion_Seguridad_RRHH_Andina.pptx"
ASSETS = ROOT / "ppt-assets"

# Paleta corporativa oscura + acento teal (evitar púrpura / cream AI-default)
BG = RGBColor(0x0B, 0x1C, 0x2C)
BG_CARD = RGBColor(0x12, 0x2B, 0x40)
BG_ALT = RGBColor(0x0F, 0x24, 0x38)
ACCENT = RGBColor(0x1A, 0xB3, 0xA6)
ACCENT_2 = RGBColor(0xF0, 0xA5, 0x00)
WHITE = RGBColor(0xF5, 0xF7, 0xFA)
MUTED = RGBColor(0x9A, 0xB0, 0xC4)
DANGER = RGBColor(0xE5, 0x4D, 0x4D)
OK = RGBColor(0x3D, 0xD6, 0x8C)
LINE = RGBColor(0x1E, 0x3A, 0x54)

W = Inches(13.333)
H = Inches(7.5)


def _set_run(run, *, size=18, bold=False, color=WHITE, font="Segoe UI"):
    run.font.name = font
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color


def _add_text(tf, text, *, size=18, bold=False, color=WHITE, align=PP_ALIGN.LEFT, space_after=6, font="Segoe UI"):
    p = tf.paragraphs[0] if not tf.paragraphs[0].text else tf.add_paragraph()
    if not tf.paragraphs[0].text and p is tf.paragraphs[0]:
        pass
    else:
        if tf.paragraphs[0].text == "" and len(tf.paragraphs) == 1:
            p = tf.paragraphs[0]
        # handled below
    # Prefer explicit paragraph creation
    return None


def write_para(tf, text, *, size=18, bold=False, color=WHITE, align=PP_ALIGN.LEFT, space_after=8, font="Segoe UI", clear=False):
    if clear:
        tf.clear()
        p = tf.paragraphs[0]
    else:
        if tf.paragraphs[0].text == "":
            p = tf.paragraphs[0]
        else:
            p = tf.add_paragraph()
    p.alignment = align
    p.space_after = Pt(space_after)
    run = p.add_run()
    run.text = text
    _set_run(run, size=size, bold=bold, color=color, font=font)
    return p


def fill_shape(shape, color: RGBColor):
    shape.fill.solid()
    shape.fill.fore_color.rgb = color
    shape.line.fill.background()


def add_bg(slide, color=BG):
    shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, W, H)
    fill_shape(shape, color)
    # send to back
    spTree = slide.shapes._spTree
    sp = shape._element
    spTree.remove(sp)
    spTree.insert(2, sp)
    return shape


def accent_bar(slide, x, y, w=Inches(1.2), h=Inches(0.08)):
    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, y, w, h)
    fill_shape(bar, ACCENT)
    return bar


def card(slide, x, y, w, h, fill=BG_CARD):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
    fill_shape(shape, fill)
    shape.adjustments[0] = 0.08
    return shape


def textbox(slide, x, y, w, h):
    return slide.shapes.add_textbox(x, y, w, h)


def add_footer(slide, page: int, total: int):
    tb = textbox(slide, Inches(0.6), Inches(7.05), Inches(10), Inches(0.3))
    tf = tb.text_frame
    write_para(tf, "UTP Lima Sur  ·  Integrador I  ·  Consultora Contable Andina S.A.C.", size=10, color=MUTED, clear=True)
    tb2 = textbox(slide, Inches(11.2), Inches(7.05), Inches(1.5), Inches(0.3))
    write_para(tb2.text_frame, f"{page} / {total}", size=10, color=MUTED, align=PP_ALIGN.RIGHT, clear=True)


def title_block(slide, eyebrow: str, title: str, subtitle: str | None = None):
    tb = textbox(slide, Inches(0.7), Inches(0.35), Inches(11.5), Inches(0.35))
    write_para(tb.text_frame, eyebrow.upper(), size=11, bold=True, color=ACCENT, clear=True)
    accent_bar(slide, Inches(0.7), Inches(0.72))
    tb2 = textbox(slide, Inches(0.7), Inches(0.9), Inches(12), Inches(0.7))
    write_para(tb2.text_frame, title, size=28, bold=True, color=WHITE, clear=True, font="Segoe UI Semibold")
    if subtitle:
        tb3 = textbox(slide, Inches(0.7), Inches(1.55), Inches(12), Inches(0.4))
        write_para(tb3.text_frame, subtitle, size=14, color=MUTED, clear=True)


def new_slide(prs):
    blank = prs.slide_layouts[6]
    slide = prs.slides.add_slide(blank)
    add_bg(slide)
    return slide


def build():
    prs = Presentation()
    prs.slide_width = W
    prs.slide_height = H
    slides_meta = []

    # --- 1 Carátula ---
    s = new_slide(prs)
    # left accent panel
    panel = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(0.25), H)
    fill_shape(panel, ACCENT)
    logo = ASSETS / "utp_logo_on_dark.png"
    if logo.exists():
        s.shapes.add_picture(str(logo), Inches(0.7), Inches(0.5), height=Inches(0.7))
    tb = textbox(s, Inches(0.7), Inches(2.0), Inches(11), Inches(0.4))
    write_para(tb.text_frame, "UNIDAD 4  ·  SEMANA 14  ·  SESIÓN 27–28", size=14, bold=True, color=ACCENT, clear=True)
    tb = textbox(s, Inches(0.7), Inches(2.5), Inches(12), Inches(1.4))
    write_para(tb.text_frame, "Pruebas de seguridad aplicadas", size=36, bold=True, color=WHITE, clear=True, font="Segoe UI Semibold")
    write_para(tb.text_frame, "al Sistema de Gestión de RR. HH. Andina", size=32, bold=True, color=WHITE, font="Segoe UI Semibold")
    tb = textbox(s, Inches(0.7), Inches(4.3), Inches(11), Inches(0.8))
    write_para(tb.text_frame, "OWASP Top 10:2025  ·  SAST · DAST · SCA · Pentest de API", size=16, color=MUTED, clear=True)
    write_para(tb.text_frame, "Spring Boot 3.4 · JWT · BCrypt 12 · Dependency-Check · ZAP", size=14, color=MUTED)
    tb = textbox(s, Inches(0.7), Inches(5.8), Inches(11), Inches(0.8))
    write_para(tb.text_frame, "Universidad Tecnológica del Perú — UTP Lima Sur", size=13, color=WHITE, clear=True)
    write_para(tb.text_frame, "Integrador I: Sistemas – Software  |  Docente: Mag. Ing. Marco Antonio Flores Torres", size=12, color=MUTED)
    slides_meta.append(s)

    # --- 2 Agenda ---
    s = new_slide(prs)
    title_block(s, "Agenda", "Qué veremos hoy", "Del riesgo teórico a evidencia ejecutable en el proyecto integrador")
    items = [
        ("01", "Contexto y activo en riesgo", "Datos personales y de nómina (Ley N.° 29733)"),
        ("02", "Enfoques SAST · DAST · SCA · Pentest", "Qué prueba qué — y cuándo"),
        ("03", "OWASP Top 10:2025 mapeado al RRHH", "A01, A02, A03, A05, A07, A09, A10"),
        ("04", "Controles implementados + evidencia", "Headers, rate limit, IDOR, JWT, SCA"),
        ("05", "Matriz, resultados y roadmap", "Criterios pasa/falla y mejoras"),
    ]
    for i, (num, title, desc) in enumerate(items):
        y = Inches(2.15) + Inches(i * 0.85)
        card(s, Inches(0.7), y, Inches(12), Inches(0.75))
        n = textbox(s, Inches(0.95), y + Inches(0.15), Inches(0.8), Inches(0.45))
        write_para(n.text_frame, num, size=20, bold=True, color=ACCENT, clear=True)
        t = textbox(s, Inches(1.9), y + Inches(0.08), Inches(10), Inches(0.55))
        write_para(t.text_frame, title, size=16, bold=True, color=WHITE, clear=True)
        write_para(t.text_frame, desc, size=12, color=MUTED, space_after=0)
    slides_meta.append(s)

    # --- 3 Por qué importa ---
    s = new_slide(prs)
    title_block(s, "Utilidad", "¿Por qué probar seguridad en RR. HH.?", "Lo que no pruebas tú, lo probará un atacante")
    stats = [
        ("147 M", "Personas afectadas\nen Equifax 2017", DANGER),
        ("100×", "Costo de corregir\nen producción vs diseño", ACCENT_2),
        ("Ley 29733", "Obliga a proteger\ndatos personales en Perú", ACCENT),
    ]
    for i, (big, small, color) in enumerate(stats):
        x = Inches(0.7) + Inches(i * 4.1)
        card(s, x, Inches(2.3), Inches(3.9), Inches(2.6))
        tb = textbox(s, x + Inches(0.3), Inches(2.6), Inches(3.3), Inches(2.2))
        write_para(tb.text_frame, big, size=28, bold=True, color=color, clear=True)
        write_para(tb.text_frame, small, size=13, color=MUTED)
    tb = textbox(s, Inches(0.7), Inches(5.3), Inches(12), Inches(1.2))
    write_para(tb.text_frame, "Activos del sistema Andina: DNI, remuneraciones, boletas PDF, organigrama, auditoría.", size=14, color=WHITE, clear=True)
    write_para(tb.text_frame, "Riesgo = probabilidad × impacto → priorizamos login, IDOR de personal y planilla.", size=14, color=MUTED)
    slides_meta.append(s)

    # --- 4 Enfoques ---
    s = new_slide(prs)
    title_block(s, "Transformación", "SAST · DAST · SCA · Pentest", "Cuatro lentes, un mismo objetivo: reducir riesgo residual")
    rows = [
        ("SAST", "Código sin ejecutar", "SonarQube", "Temprano / CI"),
        ("DAST", "App en ejecución", "OWASP ZAP", "Staging"),
        ("SCA", "Librerías / CVE", "Dependency-Check", "Cada build"),
        ("Pentest", "Ataque autorizado", "Burp Suite", "Pre-release"),
    ]
    # header
    headers = ["Enfoque", "¿Qué analiza?", "Herramienta", "¿Cuándo?"]
    for i, h in enumerate(headers):
        x = Inches(0.7) + Inches(i * 3.1)
        card(s, x, Inches(2.2), Inches(2.95), Inches(0.55), fill=RGBColor(0x15, 0x3D, 0x52))
        tb = textbox(s, x + Inches(0.15), Inches(2.3), Inches(2.7), Inches(0.4))
        write_para(tb.text_frame, h, size=12, bold=True, color=ACCENT, clear=True)
    for r, row in enumerate(rows):
        for c, val in enumerate(row):
            x = Inches(0.7) + Inches(c * 3.1)
            y = Inches(2.85) + Inches(r * 0.85)
            card(s, x, y, Inches(2.95), Inches(0.75))
            tb = textbox(s, x + Inches(0.15), y + Inches(0.2), Inches(2.7), Inches(0.4))
            write_para(tb.text_frame, val, size=13, bold=(c == 0), color=WHITE if c == 0 else MUTED, clear=True)
    slides_meta.append(s)

    # --- 5 OWASP map ---
    s = new_slide(prs)
    title_block(s, "OWASP Top 10:2025", "Mapeo al proyecto Andina", "Pregunta clave → control concreto en el código")
    mapping = [
        ("A01", "Control de acceso", "EmpleadoScope + @PreAuthorize", "IDOR ficha / planilla"),
        ("A02", "Misconfiguración", "Headers + SWAGGER_ENABLED", "Clickjacking / superficie"),
        ("A03", "Supply chain", "dependency-check-maven", "CVE CVSS ≥ 7"),
        ("A05", "Inyección", "JPA + Bean Validation", "SQLi en login"),
        ("A07", "Autenticación", "JWT 15m + rate limit + BCrypt12", "Fuerza bruta"),
        ("A09", "Logging", "Auditoría LOGIN + RequestTrace", "Intentos fallidos"),
        ("A10", "Excepciones", "GlobalExceptionHandler seguro", "Fuga de stack"),
    ]
    for i, (code, name, ctrl, prueba) in enumerate(mapping):
        col = i % 2
        row = i // 2
        x = Inches(0.7) + Inches(col * 6.2)
        y = Inches(2.15) + Inches(row * 1.05)
        card(s, x, y, Inches(5.95), Inches(0.95))
        tb = textbox(s, x + Inches(0.25), y + Inches(0.15), Inches(5.4), Inches(0.7))
        write_para(tb.text_frame, f"{code}  ·  {name}", size=14, bold=True, color=ACCENT, clear=True)
        write_para(tb.text_frame, f"{ctrl}  →  prueba: {prueba}", size=11, color=MUTED, space_after=0)
    slides_meta.append(s)

    # --- 6 Arquitectura controles ---
    s = new_slide(prs)
    title_block(s, "Arquitectura", "Controles en capas (defense in depth)", "Del perímetro HTTP al dominio de negocio")
    layers = [
        ("1. Perímetro", "RateLimitFilter\nSecurity headers\nCORS"),
        ("2. Autenticación", "JwtAuthFilter\nBCrypt 12\nRefresh rotado"),
        ("3. Autorización", "@PreAuthorize\nAuthorities por rol"),
        ("4. Dominio", "EmpleadoScope\nassertPuedeConsultar"),
        ("5. Datos", "JPA parametrizado\nErrores sin detalle"),
    ]
    for i, (title, body) in enumerate(layers):
        x = Inches(0.55) + Inches(i * 2.5)
        card(s, x, Inches(2.4), Inches(2.35), Inches(3.5))
        bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, Inches(2.4), Inches(2.35), Inches(0.12))
        fill_shape(bar, ACCENT if i % 2 == 0 else ACCENT_2)
        tb = textbox(s, x + Inches(0.15), Inches(2.7), Inches(2.05), Inches(3.0))
        write_para(tb.text_frame, title, size=14, bold=True, color=WHITE, clear=True)
        write_para(tb.text_frame, body, size=12, color=MUTED)
    slides_meta.append(s)

    # --- 7 Matriz ---
    s = new_slide(prs)
    title_block(s, "Práctica", "Matriz de pruebas de seguridad", "Evidencia de aprendizaje — criterios verificables")
    matrix = [
        ("SEC-01", "Login", "A07", "Rate limit", "429 tras cupo"),
        ("SEC-02", "Login", "A05", "SQLi payload", "400 sin token"),
        ("SEC-03", "Personal", "A01", "IDOR ID", "403 ficha ajena"),
        ("SEC-04", "Planilla", "A01", "Rol EMPLEADO", "403 planillas"),
        ("SEC-05", "API", "A02", "Headers", "DENY / nosniff / CSP"),
        ("SEC-06", "JWT", "A07", "Expiry + typ", "≤900s; refresh≠Bearer"),
        ("SEC-07", "Maven", "A03", "SCA", "CVSS≥7 → fail build"),
    ]
    headers = ["ID", "Funcionalidad", "OWASP", "Prueba", "Criterio pasa"]
    widths = [1.3, 2.2, 1.3, 2.6, 4.0]
    x0 = Inches(0.6)
    for i, h in enumerate(headers):
        x = x0 + Inches(sum(widths[:i]))
        card(s, x, Inches(2.15), Inches(widths[i] - 0.08), Inches(0.45), fill=RGBColor(0x15, 0x3D, 0x52))
        tb = textbox(s, x + Inches(0.08), Inches(2.22), Inches(widths[i] - 0.2), Inches(0.35))
        write_para(tb.text_frame, h, size=11, bold=True, color=ACCENT, clear=True)
    for r, row in enumerate(matrix):
        y = Inches(2.7) + Inches(r * 0.55)
        for c, val in enumerate(row):
            x = x0 + Inches(sum(widths[:c]))
            bg = BG_CARD if r % 2 == 0 else BG_ALT
            card(s, x, y, Inches(widths[c] - 0.08), Inches(0.5), fill=bg)
            tb = textbox(s, x + Inches(0.08), y + Inches(0.08), Inches(widths[c] - 0.2), Inches(0.35))
            write_para(tb.text_frame, val, size=11, bold=(c == 0), color=OK if c == 4 else WHITE, clear=True)
    slides_meta.append(s)

    # --- 8 Evidencia IDOR ---
    s = new_slide(prs)
    title_block(s, "Hallazgo / control A01", "IDOR en ficha de personal — remedado y verificado", "Burp/MockMvc: cambiar /api/empleados/{id} no debe filtrar PII ajena")
    card(s, Inches(0.7), Inches(2.2), Inches(5.9), Inches(4.2))
    tb = textbox(s, Inches(0.95), Inches(2.4), Inches(5.4), Inches(3.8))
    write_para(tb.text_frame, "Ataque (caja gris)", size=16, bold=True, color=DANGER, clear=True)
    write_para(tb.text_frame, "1. Login como juan.espinoza (EMPLEADO)", size=13, color=MUTED)
    write_para(tb.text_frame, "2. Obtener idEmpleado de jesus.pantoja", size=13, color=MUTED)
    write_para(tb.text_frame, "3. GET /api/empleados/{idAjeno}", size=13, color=MUTED)
    write_para(tb.text_frame, "", size=8, color=MUTED)
    write_para(tb.text_frame, "Sin control → 200 + DNI/correo", size=13, color=DANGER)
    write_para(tb.text_frame, "Con EmpleadoScope → 403 Forbidden", size=13, color=OK)
    card(s, Inches(6.85), Inches(2.2), Inches(5.9), Inches(4.2))
    tb = textbox(s, Inches(7.1), Inches(2.4), Inches(5.4), Inches(3.8))
    write_para(tb.text_frame, "Evidencia automatizada", size=16, bold=True, color=ACCENT, clear=True)
    write_para(tb.text_frame, "SecurityAccessControlIT", size=13, bold=True, color=WHITE)
    write_para(tb.text_frame, "SEC-A01-04 empleadoNoVeFichaAjena", size=12, color=MUTED)
    write_para(tb.text_frame, "", size=8, color=MUTED)
    write_para(tb.text_frame, "También cubierto:", size=13, bold=True, color=WHITE)
    write_para(tb.text_frame, "• Listado personal sin autoridad → 403", size=12, color=MUTED)
    write_para(tb.text_frame, "• Planillas sin PLANILLA_GESTIONAR → 403", size=12, color=MUTED)
    write_para(tb.text_frame, "• Auditoría sin AUDITORIA_CONSULTAR → 403", size=12, color=MUTED)
    slides_meta.append(s)

    # --- 9 Auth ---
    s = new_slide(prs)
    title_block(s, "A07 Autenticación", "Checklist profesional aplicado al login Andina", "Comparado con el ejemplo de clase (Burp Intruder)")
    checks = [
        ("Rate limit auth", "PASA", "429 + Retry-After", True),
        ("JWT access ≤ 15 min", "PASA", "expiresIn ≤ 900", True),
        ("Refresh no es Bearer API", "PASA", "typ=access requerido", True),
        ("BCrypt (no MD5)", "PASA", "strength 12", True),
        ("Mensaje no enumera usuarios", "PASA", "mismo texto 401", True),
        ("2FA para ADMIN", "PENDIENTE", "mejora fase 2", False),
    ]
    for i, (name, status, detail, ok) in enumerate(checks):
        y = Inches(2.15) + Inches(i * 0.7)
        card(s, Inches(0.7), y, Inches(12), Inches(0.6))
        tb = textbox(s, Inches(0.95), y + Inches(0.12), Inches(5), Inches(0.4))
        write_para(tb.text_frame, name, size=14, bold=True, color=WHITE, clear=True)
        tb2 = textbox(s, Inches(6.2), y + Inches(0.12), Inches(2.2), Inches(0.4))
        write_para(tb2.text_frame, status, size=14, bold=True, color=OK if ok else ACCENT_2, clear=True)
        tb3 = textbox(s, Inches(8.6), y + Inches(0.12), Inches(3.8), Inches(0.4))
        write_para(tb3.text_frame, detail, size=12, color=MUTED, clear=True)
    slides_meta.append(s)

    # --- 10 Injection ---
    s = new_slide(prs)
    title_block(s, "A05 Inyección", "Del payload de clase al resultado en Andina", "Misma técnica del taller — distinto resultado porque el código es seguro")
    card(s, Inches(0.7), Inches(2.2), Inches(6.0), Inches(4.2))
    tb = textbox(s, Inches(0.95), Inches(2.4), Inches(5.5), Inches(3.8))
    write_para(tb.text_frame, "Payload (Burp / clase)", size=15, bold=True, color=ACCENT_2, clear=True)
    write_para(tb.text_frame, "usuario = admin' OR '1'='1", size=14, color=WHITE)
    write_para(tb.text_frame, "password = cualquiera", size=14, color=WHITE)
    write_para(tb.text_frame, "", size=8, color=MUTED)
    write_para(tb.text_frame, "En app vulnerable (concat SQL):", size=13, color=MUTED)
    write_para(tb.text_frame, "→ bypass total del login", size=13, color=DANGER)
    card(s, Inches(6.95), Inches(2.2), Inches(5.8), Inches(4.2))
    tb = textbox(s, Inches(7.2), Inches(2.4), Inches(5.3), Inches(3.8))
    write_para(tb.text_frame, "En RRHH Andina", size=15, bold=True, color=OK, clear=True)
    write_para(tb.text_frame, "1. @Pattern rechaza metacaracteres → 400", size=13, color=MUTED)
    write_para(tb.text_frame, "2. Consulta JPA parametrizada", size=13, color=MUTED)
    write_para(tb.text_frame, "3. Sin stack / sin SQLException al cliente", size=13, color=MUTED)
    write_para(tb.text_frame, "", size=8, color=MUTED)
    write_para(tb.text_frame, "SecurityInjectionIT — 5 casos", size=13, bold=True, color=WHITE)
    write_para(tb.text_frame, "SQLi · UNION · XSS password · meta", size=12, color=MUTED)
    slides_meta.append(s)

    # --- 11 Headers + SCA ---
    s = new_slide(prs)
    title_block(s, "A02 + A03", "Configuración segura y cadena de suministro", "DAST (cabeceras) y SCA (CVE) en el mismo ciclo")
    card(s, Inches(0.7), Inches(2.2), Inches(6.0), Inches(4.2))
    tb = textbox(s, Inches(0.95), Inches(2.4), Inches(5.5), Inches(3.8))
    write_para(tb.text_frame, "Cabeceras (SecurityConfig)", size=15, bold=True, color=ACCENT, clear=True)
    for line in [
        "X-Content-Type-Options: nosniff",
        "X-Frame-Options: DENY",
        "Content-Security-Policy: frame-ancestors 'none'",
        "Referrer-Policy: strict-origin-when-cross-origin",
        "Permissions-Policy: camera=() …",
        "HSTS preload (prod HTTPS)",
        "SWAGGER_ENABLED=false en producción",
    ]:
        write_para(tb.text_frame, "•  " + line, size=12, color=MUTED)
    card(s, Inches(6.95), Inches(2.2), Inches(5.8), Inches(4.2))
    tb = textbox(s, Inches(7.2), Inches(2.4), Inches(5.3), Inches(3.8))
    write_para(tb.text_frame, "SCA — Dependency-Check", size=15, bold=True, color=ACCENT_2, clear=True)
    write_para(tb.text_frame, "mvn org.owasp:dependency-check-maven:check", size=12, color=WHITE)
    write_para(tb.text_frame, "", size=8, color=MUTED)
    write_para(tb.text_frame, "failBuildOnCVSS = 7", size=14, bold=True, color=WHITE)
    write_para(tb.text_frame, "Reporte HTML en target/", size=12, color=MUTED)
    write_para(tb.text_frame, "", size=8, color=MUTED)
    write_para(tb.text_frame, "Lección Equifax: un CVE conocido", size=13, color=MUTED)
    write_para(tb.text_frame, "sin parche = incidente masivo.", size=13, color=MUTED)
    slides_meta.append(s)

    # --- 12 Cómo ejecutar ---
    s = new_slide(prs)
    title_block(s, "Operación", "Cómo reproducir la evidencia", "Comandos del repositorio — cero magia")
    cmds = [
        ("Unitarias + IT seguridad", "cd backend && mvn verify"),
        ("Solo IT de seguridad", "mvn -Dtest=Security*IT verify"),
        ("SCA (CVE)", "mvn -DskipTests org.owasp:dependency-check-maven:check"),
        ("DAST baseline ZAP", "zap-baseline.py -t http://localhost:8080/..."),
        ("Documentación", "docs/MATRIZ_PRUEBAS_SEGURIDAD_SEM14.md"),
    ]
    for i, (label, cmd) in enumerate(cmds):
        y = Inches(2.2) + Inches(i * 0.8)
        card(s, Inches(0.7), y, Inches(12), Inches(0.7))
        tb = textbox(s, Inches(0.95), y + Inches(0.08), Inches(11.5), Inches(0.55))
        write_para(tb.text_frame, label, size=12, bold=True, color=ACCENT, clear=True)
        write_para(tb.text_frame, cmd, size=13, color=WHITE, space_after=0)
    slides_meta.append(s)

    # --- 13 Resultados ---
    s = new_slide(prs)
    title_block(s, "Resultados", "Estado del control — avance 70%", "Resumen ejecutivo para el docente")
    results = [
        ("Autenticación", "Fuerte", OK, "JWT + rate limit + BCrypt12"),
        ("Control de acceso", "Fuerte", OK, "IDOR personal / planilla bloqueados"),
        ("Inyección login", "Mitigado", OK, "Validación + JPA"),
        ("Cabeceras HTTP", "Mitigado", OK, "SecurityHeadersIT en verde"),
        ("SCA", "Automatizado", OK, "Gate CVSS ≥ 7"),
        ("2FA / WAF", "Pendiente", ACCENT_2, "Roadmap post-avance"),
    ]
    for i, (k, v, c, d) in enumerate(results):
        col = i % 3
        row = i // 3
        x = Inches(0.7) + Inches(col * 4.15)
        y = Inches(2.25) + Inches(row * 2.1)
        card(s, x, y, Inches(3.95), Inches(1.9))
        tb = textbox(s, x + Inches(0.25), y + Inches(0.25), Inches(3.45), Inches(1.5))
        write_para(tb.text_frame, k, size=13, color=MUTED, clear=True)
        write_para(tb.text_frame, v, size=22, bold=True, color=c)
        write_para(tb.text_frame, d, size=12, color=WHITE)
    slides_meta.append(s)

    # --- 14 Roadmap ---
    s = new_slide(prs)
    title_block(s, "Mejora continua", "Roadmap de seguridad", "Shift-left: seguridad en todo el ciclo de vida")
    phases = [
        ("Hecho (S14)", ["Headers HTTP", "IT IDOR/SQLi/429", "Swagger apagable", "Matriz + PPT"], OK),
        ("Próximo", ["SonarQube en CI", "ZAP en pipeline", "Policy contraseñas alta", "Alertas login fallido"], ACCENT),
        ("Después", ["2FA admin", "WAF / bot detection", "Bug bounty interno", "ISO 27001 controls"], ACCENT_2),
    ]
    for i, (title, items, color) in enumerate(phases):
        x = Inches(0.7) + Inches(i * 4.15)
        card(s, x, Inches(2.25), Inches(3.95), Inches(4.2))
        bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, Inches(2.25), Inches(3.95), Inches(0.12))
        fill_shape(bar, color)
        tb = textbox(s, x + Inches(0.3), Inches(2.55), Inches(3.4), Inches(3.7))
        write_para(tb.text_frame, title, size=16, bold=True, color=color, clear=True)
        for it in items:
            write_para(tb.text_frame, "→  " + it, size=13, color=MUTED)
    slides_meta.append(s)

    # --- 15 Cierre ---
    s = new_slide(prs)
    panel = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(0.25), H)
    fill_shape(panel, ACCENT)
    tb = textbox(s, Inches(0.9), Inches(2.0), Inches(11.5), Inches(2.5))
    write_para(tb.text_frame, "La seguridad no es un módulo extra.", size=28, bold=True, color=WHITE, clear=True, font="Segoe UI Semibold")
    write_para(tb.text_frame, "Es parte de la calidad del software.", size=28, bold=True, color=ACCENT, font="Segoe UI Semibold")
    tb = textbox(s, Inches(0.9), Inches(4.5), Inches(11.5), Inches(1.5))
    write_para(tb.text_frame, "Proyecto: RR. HH. Andina  ·  Evidencia: MATRIZ_PRUEBAS_SEGURIDAD_SEM14.md + *Security*IT", size=14, color=MUTED, clear=True)
    write_para(tb.text_frame, "Gracias  ·  ¿Preguntas?", size=18, bold=True, color=WHITE)
    slides_meta.append(s)

    total = len(slides_meta)
    for i, slide in enumerate(slides_meta, start=1):
        if i not in (1, total):
            add_footer(slide, i, total)

    prs.save(OUT)
    print(f"OK -> {OUT}")


if __name__ == "__main__":
    build()
