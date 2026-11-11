#!/usr/bin/env python3
"""Presentación profesional widescreen — Semana 13 Testing · RRHH Andina."""

from pathlib import Path

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.oxml.ns import qn
from pptx.util import Inches, Pt, Emu
from lxml import etree

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "ppt-assets"
OUT = ROOT / "S13_Presentacion_Testing_RRHH_Andina.pptx"

# Paleta corporativa / académica
NAVY = RGBColor(0x0B, 0x1F, 0x3A)
NAVY2 = RGBColor(0x14, 0x36, 0x5C)
GOLD = RGBColor(0xC4, 0x9A, 0x4A)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
OFF = RGBColor(0xF5, 0xF7, 0xFA)
DARK = RGBColor(0x1E, 0x29, 0x3B)
MUTED = RGBColor(0x64, 0x74, 0x8B)
LINE = RGBColor(0xE2, 0xE8, 0xF0)
GREEN = RGBColor(0x05, 0x96, 0x69)
RED = RGBColor(0xDC, 0x26, 0x26)

W = Inches(13.333)
H = Inches(7.5)
TOTAL = 14


def blank(prs):
    return prs.slides.add_slide(prs.slide_layouts[6])


def set_run(p, text, size=18, bold=False, color=DARK, font_name="Calibri"):
    p.clear()
    run = p.add_run()
    run.text = text
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color
    run.font.name = font_name
    return run


def shape_fill(shape, rgb):
    shape.fill.solid()
    shape.fill.fore_color.rgb = rgb
    shape.line.fill.background()


def rect(slide, left, top, width, height, fill):
    s = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, left, top, width, height)
    shape_fill(s, fill)
    return s


def round_rect(slide, left, top, width, height, fill):
    s = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
    shape_fill(s, fill)
    # softer corners
    try:
        s.adjustments[0] = 0.08
    except Exception:
        pass
    return s


def tb(slide, left, top, width, height, text, size=18, bold=False, color=DARK, align=PP_ALIGN.LEFT):
    box = slide.shapes.add_textbox(left, top, width, height)
    tf = box.text_frame
    tf.word_wrap = True
    set_run(tf.paragraphs[0], text, size=size, bold=bold, color=color)
    tf.paragraphs[0].alignment = align
    return box


def bullets(slide, left, top, width, height, items, size=17, color=DARK, spacing=8):
    box = slide.shapes.add_textbox(left, top, width, height)
    tf = box.text_frame
    tf.word_wrap = True
    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        set_run(p, "•  " + item, size=size, color=color)
        p.space_after = Pt(spacing)
        p.alignment = PP_ALIGN.LEFT
    return box


def pic(slide, path, left, top, width=None, height=None):
    path = Path(path)
    if not path.exists():
        return None
    kw = {}
    if width is not None:
        kw["width"] = width
    if height is not None:
        kw["height"] = height
    return slide.shapes.add_picture(str(path), left, top, **kw)


def add_logo(slide, dark=False):
    """Logo UTP pequeño en esquina superior derecha."""
    logo = ASSETS / ("utp_logo_on_dark.png" if dark else "utp_logo.png")
    if not logo.exists():
        logo = ASSETS / "utp_logo.png"
    if logo.exists():
        # Solo los bloques UTP (sin texto largo) → recorte visual vía tamaño chico
        pic(slide, logo, Inches(11.85), Inches(0.12), width=Inches(1.25))


def footer(slide, n):
    rect(slide, 0, Inches(7.15), W, Inches(0.35), OFF)
    tb(slide, Inches(0.45), Inches(7.2), Inches(10), Inches(0.25),
       "UTP Lima Sur  ·  Curso Integrador I  ·  Semana 13 — Pruebas de Software  ·  RRHH Andina",
       size=10, color=MUTED)
    tb(slide, Inches(12.2), Inches(7.2), Inches(0.9), Inches(0.25),
       f"{n:02d}/{TOTAL:02d}", size=10, bold=True, color=NAVY, align=PP_ALIGN.RIGHT)


def header_bar(slide, title, kicker=None):
    rect(slide, 0, 0, W, Inches(1.05), NAVY)
    rect(slide, 0, Inches(1.05), W, Inches(0.06), GOLD)
    if kicker:
        tb(slide, Inches(0.55), Inches(0.12), Inches(10.8), Inches(0.28),
           kicker.upper(), size=11, bold=True, color=GOLD)
        tb(slide, Inches(0.55), Inches(0.42), Inches(10.8), Inches(0.45),
           title, size=24, bold=True, color=WHITE)
    else:
        tb(slide, Inches(0.55), Inches(0.28), Inches(10.8), Inches(0.5),
           title, size=24, bold=True, color=WHITE)
    add_logo(slide, dark=True)


def add_table(slide, left, top, width, height, headers, rows, col_widths=None):
    table_shape = slide.shapes.add_table(len(rows) + 1, len(headers), left, top, width, height)
    table = table_shape.table
    if col_widths:
        for i, w in enumerate(col_widths):
            table.columns[i].width = w
    for c, h in enumerate(headers):
        cell = table.cell(0, c)
        cell.text = h
        cell.fill.solid()
        cell.fill.fore_color.rgb = NAVY
        for p in cell.text_frame.paragraphs:
            for r in p.runs:
                r.font.bold = True
                r.font.size = Pt(12)
                r.font.color.rgb = WHITE
                r.font.name = "Calibri"
            p.alignment = PP_ALIGN.LEFT
        cell.vertical_anchor = MSO_ANCHOR.MIDDLE
    for r_i, row in enumerate(rows, start=1):
        for c, val in enumerate(row):
            cell = table.cell(r_i, c)
            cell.text = str(val)
            cell.fill.solid()
            cell.fill.fore_color.rgb = WHITE if r_i % 2 else OFF
            for p in cell.text_frame.paragraphs:
                for run in p.runs:
                    run.font.size = Pt(12)
                    run.font.color.rgb = GREEN if str(val).upper() in ("PASÓ", "PASS", "OK") else DARK
                    run.font.bold = str(val).upper() in ("PASÓ", "PASS", "OK") or c == 0
                    run.font.name = "Calibri"
            cell.vertical_anchor = MSO_ANCHOR.MIDDLE
    return table_shape


# ───────────────── slides ─────────────────

def slide_cover(prs):
    s = blank(prs)
    rect(s, 0, 0, W, H, NAVY)
    rect(s, Inches(9.2), 0, Inches(4.2), H, NAVY2)
    rect(s, 0, Inches(6.85), W, Inches(0.65), GOLD)
    add_logo(s, dark=True)

    tb(s, Inches(0.7), Inches(0.55), Inches(8), Inches(0.35),
       "UNIVERSIDAD TECNOLÓGICA DEL PERÚ  ·  LIMA SUR", size=13, bold=True, color=GOLD)
    tb(s, Inches(0.7), Inches(1.55), Inches(8.2), Inches(1.3),
       "Sistema de Gestión de\nRecursos Humanos", size=38, bold=True, color=WHITE)
    tb(s, Inches(0.7), Inches(3.1), Inches(8), Inches(0.35),
       "Avance de proyecto · Pruebas de Software (Testing)", size=18, color=WHITE)
    tb(s, Inches(0.7), Inches(3.55), Inches(8), Inches(0.3),
       "Consultora Contable Andina S.A.C.", size=16, bold=True, color=GOLD)

    # Integrantes
    tb(s, Inches(0.7), Inches(4.15), Inches(8), Inches(0.3),
       "Integrantes", size=14, bold=True, color=GOLD)
    bullets(s, Inches(0.7), Inches(4.5), Inches(7.5), Inches(0.9), [
        "Jesús Pantoja",
        "Jesús Mechan",
    ], size=16, color=WHITE, spacing=6)

    round_rect(s, Inches(0.7), Inches(5.55), Inches(7.5), Inches(1.05), NAVY2)
    tb(s, Inches(0.95), Inches(5.7), Inches(7), Inches(0.8),
       "Curso Integrador I: Sistemas – Software  ·  Semana 13\n"
       "Evidencia: matriz de casos + JUnit 5 + Mockito + JaCoCo",
       size=13, color=WHITE)

    tb(s, Inches(0.7), Inches(7.0), Inches(10), Inches(0.3),
       "Ingeniería de Software  ·  2026", size=12, bold=True, color=NAVY)


def slide_agenda(prs):
    s = blank(prs)
    header_bar(s, "Agenda", "Contenido")
    items = [
        ("01", "Contexto del proyecto Andina"),
        ("02", "Objetivo del avance de testing"),
        ("03", "Pirámide de pruebas aplicada"),
        ("04", "Requisito RF-AUTH-01 y matriz de casos"),
        ("05", "Automatización (JUnit 5 · Mockito · MockMvc)"),
        ("06", "Evidencia: mvn test y cobertura JaCoCo"),
        ("07", "Cómo verificar las pruebas"),
        ("08", "Conclusiones"),
    ]
    for i, (num, title) in enumerate(items):
        col = i // 4
        row = i % 4
        left = Inches(0.7) + Inches(col * 6.2)
        top = Inches(1.5) + Inches(row * 1.2)
        round_rect(s, left, top, Inches(5.7), Inches(1.0), OFF)
        rect(s, left, top, Inches(0.12), Inches(1.0), GOLD)
        tb(s, left + Inches(0.35), top + Inches(0.28), Inches(1.0), Inches(0.45),
           num, size=22, bold=True, color=GOLD)
        tb(s, left + Inches(1.3), top + Inches(0.32), Inches(4.1), Inches(0.45),
           title, size=16, bold=True, color=NAVY)
    footer(s, 2)


def slide_contexto(prs):
    s = blank(prs)
    header_bar(s, "Contexto del proyecto", "Sistema RRHH Andina")
    bullets(s, Inches(0.6), Inches(1.45), Inches(6.3), Inches(5), [
        "Cliente: Consultora Contable Andina S.A.C.",
        "Frontend React + API REST Spring Boot.",
        "Backend hexagonal: dominio, application, adapters.",
        "Seguridad JWT: POST /api/auth/login.",
        "Base de datos PostgreSQL 16.",
        "Riesgo sin pruebas: acceso indebido y nómina errónea.",
    ], size=16, spacing=12)

    # KPI cards
    cards = [
        ("28", "Pruebas\nautomatizadas"),
        ("0", "Fallos en\nmvn test"),
        ("69%", "Cobertura\nAuthService"),
        ("92%", "Cobertura\nPlanilla"),
    ]
    for i, (val, label) in enumerate(cards):
        top = Inches(1.45) + Inches(i * 1.3)
        round_rect(s, Inches(7.4), top, Inches(5.3), Inches(1.15), OFF)
        tb(s, Inches(7.65), top + Inches(0.2), Inches(1.8), Inches(0.7),
           val, size=28, bold=True, color=NAVY)
        tb(s, Inches(9.5), top + Inches(0.25), Inches(3), Inches(0.7),
           label, size=14, color=MUTED)
    footer(s, 3)


def slide_objetivo(prs):
    s = blank(prs)
    header_bar(s, "Objetivo del avance", "Qué entregamos esta semana")
    cards = [
        ("Diseño", "Matriz de 4 casos de prueba del RF-AUTH-01:\nválido, inválido y 2 valores límite."),
        ("Automatización", "Suite JUnit 5 con Mockito y MockMvc\nsobre AuthService y AuthController."),
        ("Evidencia", "Ejecución mvn test en verde + reporte\nJaCoCo + documentación en /docs."),
    ]
    for i, (t, body) in enumerate(cards):
        left = Inches(0.55) + Inches(i * 4.2)
        round_rect(s, left, Inches(1.7), Inches(3.95), Inches(4.4), OFF)
        rect(s, left, Inches(1.7), Inches(3.95), Inches(0.14), GOLD)
        tb(s, left + Inches(0.3), Inches(2.2), Inches(3.3), Inches(0.5),
           t, size=22, bold=True, color=NAVY)
        tb(s, left + Inches(0.3), Inches(3.0), Inches(3.3), Inches(2.5),
           body, size=15, color=DARK)
    footer(s, 4)


def slide_piramide(prs):
    s = blank(prs)
    header_bar(s, "Pirámide de testing aplicada", "Más unitarias, menos E2E frágiles")
    levels = [
        (Inches(3.8), Inches(1.5), Inches(5.7), Inches(1.15), OFF, MUTED,
         "Sistema (manual)", "Recorridos docs/PRUEBAS.md · login, bandeja, vacaciones"),
        (Inches(2.7), Inches(2.9), Inches(7.9), Inches(1.25), RGBColor(0xE8, 0xF0, 0xFE), NAVY2,
         "API / Controller", "AuthControllerTest · MockMvc · HTTP 200 / 400 / 401"),
        (Inches(1.5), Inches(4.4), Inches(10.3), Inches(1.55), NAVY, WHITE,
         "Unitarias (base de la pirámide)",
         "AuthServiceTest · LoginRequestValidationTest · PlanillaCalculoPeruDinamicoTest · PoiExcelAdapterTest"),
    ]
    for left, top, w, h, fill, tcolor, title, desc in levels:
        round_rect(s, left, top, w, h, fill)
        tb(s, left + Inches(0.35), top + Inches(0.2), w - Inches(0.5), Inches(0.4),
           title, size=18, bold=True, color=tcolor if fill != NAVY else WHITE)
        tb(s, left + Inches(0.35), top + Inches(0.65), w - Inches(0.5), Inches(0.6),
           desc, size=13, color=WHITE if fill == NAVY else MUTED)
    footer(s, 5)


def slide_requisito(prs):
    s = blank(prs)
    header_bar(s, "Requisito bajo prueba: RF-AUTH-01", "Inicio de sesión con JWT")
    # left panel
    round_rect(s, Inches(0.5), Inches(1.4), Inches(6.0), Inches(5.3), OFF)
    tb(s, Inches(0.8), Inches(1.65), Inches(5.4), Inches(0.4),
       "Definición", size=16, bold=True, color=GOLD)
    tb(s, Inches(0.8), Inches(2.15), Inches(5.4), Inches(1.5),
       "El usuario inicia sesión con nombreUsuario y password. "
       "Si está activo y las credenciales son correctas, el sistema emite accessToken y refreshToken JWT.",
       size=15, color=DARK)
    bullets(s, Inches(0.8), Inches(3.8), Inches(5.4), Inches(2.5), [
        "Capa: AuthService + AuthController",
        "Validación: @Pattern 3–60 caracteres",
        "Técnicas: equivalencia + valores límite",
        "Nivel: unitario + prueba de API",
    ], size=15, spacing=10)

    # right: login visual
    img = ASSETS / "09_login_ui.png"
    if img.exists():
        pic(s, img, Inches(6.8), Inches(1.4), width=Inches(6.1))
    footer(s, 6)


def slide_matriz(prs):
    s = blank(prs)
    header_bar(s, "Matriz de casos de prueba", "1 válido · 1 inválido · 2 valores límite")
    headers = ["ID", "Nivel / Tipo", "Datos de prueba", "Resultado esperado", "Estado"]
    rows = [
        ["CP-AUTH-01", "Unitario · Funcional", "juan.espinoza / Andina2026", "Tokens JWT emitidos", "PASÓ"],
        ["CP-AUTH-02", "Unitario · Funcional", "Contraseña incorrecta", "401 UNAUTHORIZED", "PASÓ"],
        ["CP-AUTH-03", "DTO · Valor límite", "Usuario «ab» (2 chars)", "400 validación", "PASÓ"],
        ["CP-AUTH-04", "DTO · Valor límite", "Usuario 60 caracteres", "Sin violaciones", "PASÓ"],
    ]
    add_table(
        s,
        Inches(0.45), Inches(1.5), Inches(12.4), Inches(3.6),
        headers, rows,
        col_widths=[Inches(1.6), Inches(2.6), Inches(3.3), Inches(3.0), Inches(1.5)],
    )
    tb(s, Inches(0.5), Inches(5.5), Inches(12.2), Inches(1.2),
       "Justificación: detección temprana sin PostgreSQL (puertos mockeados); trazabilidad CP → @DisplayName; "
       "independencia de datos por test. Documento: docs/MATRIZ_PRUEBAS_SEM13.md",
       size=14, color=MUTED)
    footer(s, 7)


def slide_auto(prs):
    s = blank(prs)
    header_bar(s, "Automatización en el código", "Patrón AAA · trazabilidad a la matriz")
    img = ASSETS / "07_codigo_auth_test.png"
    if img.exists():
        pic(s, img, Inches(0.5), Inches(1.3), width=Inches(8.0))
    round_rect(s, Inches(8.8), Inches(1.4), Inches(4.1), Inches(5.2), OFF)
    tb(s, Inches(9.1), Inches(1.7), Inches(3.6), Inches(0.4),
       "Clases creadas", size=16, bold=True, color=GOLD)
    bullets(s, Inches(9.1), Inches(2.3), Inches(3.6), Inches(4), [
        "AuthServiceTest",
        "LoginRequestValidationTest",
        "AuthControllerTest",
        "PlanillaCalculoPeru… (previo)",
        "PoiExcelAdapterTest (previo)",
        "JaCoCo en pom.xml",
    ], size=15, spacing=12)
    footer(s, 8)


def slide_evidencia_mvn(prs):
    s = blank(prs)
    header_bar(s, "Evidencia de ejecución", "mvn test → BUILD SUCCESS")
    img = ASSETS / "02_mvn_test.png"
    if img.exists():
        pic(s, img, Inches(0.45), Inches(1.25), width=Inches(7.6))
    round_rect(s, Inches(8.3), Inches(1.4), Inches(4.6), Inches(5.2), OFF)
    tb(s, Inches(8.6), Inches(1.7), Inches(4.1), Inches(0.4),
       "Resultado", size=16, bold=True, color=GOLD)
    stats = [("28", "tests ejecutados"), ("0", "fallos / errores"), ("5", "clases de prueba"), ("100%", "casos CP-AUTH")]
    for i, (v, lab) in enumerate(stats):
        top = Inches(2.4) + Inches(i * 0.95)
        tb(s, Inches(8.6), top, Inches(4), Inches(0.4), v, size=26, bold=True, color=NAVY)
        tb(s, Inches(8.6), top + Inches(0.4), Inches(4), Inches(0.3), lab, size=13, color=MUTED)
    footer(s, 9)


def slide_jacoco(prs):
    s = blank(prs)
    header_bar(s, "Cobertura JaCoCo (en el navegador)", "http://127.0.0.1:8765/…/jacoco/")
    img = ASSETS / "jacoco_browser.png"
    if img.exists():
        pic(s, img, Inches(0.35), Inches(1.2), width=Inches(8.3))
    round_rect(s, Inches(8.9), Inches(1.35), Inches(4.0), Inches(5.2), OFF)
    tb(s, Inches(9.15), Inches(1.6), Inches(3.5), Inches(0.4),
       "Clases prioritarias", size=15, bold=True, color=GOLD)
    for i, (name, pct) in enumerate([
        ("AuthService", "69%"),
        ("PlanillaCalculo…", "92%"),
        ("Pension (inner)", "100%"),
    ]):
        top = Inches(2.3) + Inches(i * 1.1)
        tb(s, Inches(9.15), top, Inches(3.5), Inches(0.35), name, size=14, bold=True, color=NAVY)
        tb(s, Inches(9.15), top + Inches(0.35), Inches(3.5), Inches(0.35),
           f"Instrucciones: {pct}", size=18, bold=True, color=GREEN)
    tb(s, Inches(9.15), Inches(5.7), Inches(3.5), Inches(0.7),
       "La cobertura global es baja a propósito: se priorizó el requisito del avance.",
       size=12, color=MUTED)
    footer(s, 10)


def slide_jacoco_code(prs):
    s = blank(prs)
    header_bar(s, "JaCoCo sobre AuthService", "Verde = cubierto por tests · Rojo = pendiente")
    img = ASSETS / "jacoco_authservice.png"
    if img.exists():
        pic(s, img, Inches(0.7), Inches(1.2), width=Inches(11.9))
    footer(s, 11)


def slide_como(prs):
    s = blank(prs)
    header_bar(s, "Cómo comprobar el testing", "Pasos para el equipo y el docente")
    steps = [
        ("1", "Terminal", "cd backend && mvn test"),
        ("2", "Resultado", "Tests run: 28 · Failures: 0 · BUILD SUCCESS"),
        ("3", "JaCoCo web", "Abrir target/site/jacoco/index.html\n(servido en :8765)"),
        ("4", "IDE", "Clic derecho en src/test → Run Tests"),
    ]
    for i, (n, title, body) in enumerate(steps):
        left = Inches(0.45) + Inches(i * 3.2)
        round_rect(s, left, Inches(1.7), Inches(3.0), Inches(4.5), OFF)
        oval = s.shapes.add_shape(MSO_SHAPE.OVAL, left + Inches(1.05), Inches(2.1), Inches(0.9), Inches(0.9))
        shape_fill(oval, GOLD)
        tf = oval.text_frame
        tf.paragraphs[0].alignment = PP_ALIGN.CENTER
        set_run(tf.paragraphs[0], n, size=22, bold=True, color=NAVY)
        tb(s, left + Inches(0.2), Inches(3.3), Inches(2.6), Inches(0.4),
           title, size=16, bold=True, color=NAVY, align=PP_ALIGN.CENTER)
        tb(s, left + Inches(0.2), Inches(3.9), Inches(2.6), Inches(1.8),
           body, size=13, color=MUTED, align=PP_ALIGN.CENTER)
    footer(s, 12)


def slide_conclusiones(prs):
    s = blank(prs)
    header_bar(s, "Conclusiones", "Lo que demostramos con este avance")

    conclusions = [
        ("Las pruebas confirman el requisito",
         "El RF-AUTH-01 (inicio de sesión) se verificó con casos válidos, inválidos y de valor límite; los 4 casos de la matriz pasaron en automatización."),
        ("El testing reduce el riesgo del negocio",
         "Un fallo en autenticación o en planilla impacta datos sensibles y nómina. Detectar defectos antes de producción genera confianza al cliente Andina."),
        ("Diseñar antes de automatizar aporta calidad",
         "Partición de equivalencia y valores límite evitaron probar «a ciegas»: con pocos casos cubrimos las clases críticas del login."),
        ("La arquitectura hexagonal facilita el testing",
         "Al mockear puertos pudimos unitariar AuthService sin PostgreSQL ni JWT real, manteniendo pruebas rápidas e independientes."),
    ]
    for i, (title, body) in enumerate(conclusions):
        col = i % 2
        row = i // 2
        left = Inches(0.5) + Inches(col * 6.4)
        top = Inches(1.4) + Inches(row * 2.6)
        round_rect(s, left, top, Inches(6.1), Inches(2.35), OFF)
        rect(s, left, top, Inches(0.12), Inches(2.35), GOLD)
        tb(s, left + Inches(0.35), top + Inches(0.3), Inches(5.5), Inches(0.5),
           title, size=16, bold=True, color=NAVY)
        tb(s, left + Inches(0.35), top + Inches(0.9), Inches(5.5), Inches(1.2),
           body, size=13, color=DARK)
    footer(s, 13)


def slide_cierre(prs):
    s = blank(prs)
    rect(s, 0, 0, W, H, NAVY)
    add_logo(s, dark=True)
    tb(s, Inches(0.8), Inches(3.1), Inches(11.7), Inches(1.0),
       "Gracias", size=54, bold=True, color=WHITE, align=PP_ALIGN.CENTER)


def main():
    prs = Presentation()
    prs.slide_width = W
    prs.slide_height = H

    slide_cover(prs)
    slide_agenda(prs)
    slide_contexto(prs)
    slide_objetivo(prs)
    slide_piramide(prs)
    slide_requisito(prs)
    slide_matriz(prs)
    slide_auto(prs)
    slide_evidencia_mvn(prs)
    slide_jacoco(prs)
    slide_jacoco_code(prs)
    slide_como(prs)
    slide_conclusiones(prs)
    slide_cierre(prs)

    try:
        prs.save(OUT)
        print(f"OK → {OUT}")
    except PermissionError:
        alt = OUT.with_name("S13_Presentacion_Testing_RRHH_Andina_NUEVA.pptx")
        prs.save(alt)
        print(f"Archivo bloqueado (ciérralo en PowerPoint). Guardado en: {alt}")


if __name__ == "__main__":
    main()
