#!/usr/bin/env python3
"""Genera un dashboard HTML visual de las pruebas de seguridad (Surefire + Failsafe)."""

from __future__ import annotations

import html
import re
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "backend"
SUREFIRE = BACKEND / "target" / "surefire-reports"
FAILSAFE = BACKEND / "target" / "failsafe-reports"
OUT = BACKEND / "target" / "seguridad-report.html"
DOCS_OUT = ROOT / "docs" / "seguridad-report.html"

SEC_CLASSES = {
    "SecurityAccessControlIT": ("A01", "Control de acceso / IDOR"),
    "SecurityInjectionIT": ("A05", "Inyección SQL / XSS"),
    "SecurityHeadersIT": ("A02", "Cabeceras HTTP"),
    "SecurityAuthHardeningIT": ("A07", "JWT / autenticación"),
    "RateLimitFilterTest": ("A07", "Rate limit (fuerza bruta)"),
}


def parse_reports(folder: Path) -> list[dict]:
    results = []
    if not folder.is_dir():
        return results
    for xml_path in sorted(folder.glob("TEST-*.xml")):
        try:
            root = ET.parse(xml_path).getroot()
        except ET.ParseError:
            continue
        suite = root.attrib.get("name", xml_path.stem)
        short = suite.split(".")[-1]
        if short not in SEC_CLASSES and "Security" not in short and "RateLimit" not in short:
            continue
        owasp, label = SEC_CLASSES.get(short, ("—", short))
        for case in root.findall("testcase"):
            name = case.attrib.get("name", "")
            classname = case.attrib.get("classname", suite)
            time_s = case.attrib.get("time", "0")
            failure = case.find("failure")
            error = case.find("error")
            skipped = case.find("skipped")
            if failure is not None:
                status, detail = "FAIL", failure.attrib.get("message") or (failure.text or "")
            elif error is not None:
                status, detail = "ERROR", error.attrib.get("message") or (error.text or "")
            elif skipped is not None:
                status, detail = "SKIP", skipped.attrib.get("message") or ""
            else:
                status, detail = "PASS", ""
            display = ""
            # DisplayName no siempre viene en XML; el método sí
            results.append(
                {
                    "suite": short,
                    "owasp": owasp,
                    "label": label,
                    "method": name,
                    "class": classname,
                    "status": status,
                    "time": time_s,
                    "detail": (detail or "")[:280],
                }
            )
    return results


def build_html(cases: list[dict]) -> str:
    total = len(cases)
    passed = sum(1 for c in cases if c["status"] == "PASS")
    failed = sum(1 for c in cases if c["status"] in ("FAIL", "ERROR"))
    skipped = sum(1 for c in cases if c["status"] == "SKIP")
    stamp = datetime.now().strftime("%Y-%m-%d %H:%M")

    rows = []
    for c in cases:
        badge = {
            "PASS": '<span class="badge pass">PASA</span>',
            "FAIL": '<span class="badge fail">FALLA</span>',
            "ERROR": '<span class="badge fail">ERROR</span>',
            "SKIP": '<span class="badge skip">SKIP</span>',
        }.get(c["status"], c["status"])
        rows.append(
            f"""<tr>
            <td><code>{html.escape(c['owasp'])}</code></td>
            <td>{html.escape(c['label'])}</td>
            <td><code>{html.escape(c['suite'])}</code></td>
            <td>{html.escape(c['method'])}</td>
            <td>{badge}</td>
            <td>{html.escape(c['time'])}s</td>
            <td class="detail">{html.escape(c['detail'])}</td>
            </tr>"""
        )

    empty_note = ""
    if not cases:
        empty_note = """
        <div class="warn">
          <strong>Sin resultados XML aún.</strong>
          Ejecuta en <code>backend</code>:
          <pre>mvn "-Dtest=RateLimitFilterTest,Security*IT" "-Dit.test=Security*IT" verify</pre>
          y vuelve a generar este reporte.
        </div>"""

    return f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>Dashboard pruebas de seguridad — RRHH Andina</title>
<style>
  :root {{
    --bg: #0b1c2c; --card: #122b40; --text: #f5f7fa; --muted: #9ab0c4;
    --accent: #1ab3a6; --ok: #3dd68c; --fail: #e54d4d; --warn: #f0a500;
  }}
  * {{ box-sizing: border-box; }}
  body {{
    margin: 0; font-family: "Segoe UI", system-ui, sans-serif;
    background: linear-gradient(160deg, #0b1c2c 0%, #0f2438 50%, #122b40 100%);
    color: var(--text); min-height: 100vh; padding: 2rem;
  }}
  h1 {{ font-size: 1.75rem; margin: 0 0 .25rem; }}
  .sub {{ color: var(--muted); margin-bottom: 1.5rem; }}
  .accent {{ width: 4rem; height: .25rem; background: var(--accent); border-radius: 2px; margin-bottom: 1rem; }}
  .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }}
  .card {{
    background: var(--card); border-radius: 12px; padding: 1.1rem 1.25rem;
    border: 1px solid #1e3a54;
  }}
  .card .n {{ font-size: 2rem; font-weight: 700; }}
  .card .l {{ color: var(--muted); font-size: .85rem; }}
  .ok {{ color: var(--ok); }} .bad {{ color: var(--fail); }} .warnc {{ color: var(--warn); }}
  table {{ width: 100%; border-collapse: collapse; background: var(--card); border-radius: 12px; overflow: hidden; }}
  th, td {{ padding: .75rem .9rem; text-align: left; border-bottom: 1px solid #1e3a54; font-size: .9rem; vertical-align: top; }}
  th {{ background: #153d52; color: var(--accent); font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; }}
  tr:last-child td {{ border-bottom: none; }}
  code {{ font-family: Consolas, monospace; font-size: .85em; }}
  .badge {{
    display: inline-block; padding: .2rem .55rem; border-radius: 999px;
    font-size: .75rem; font-weight: 700;
  }}
  .pass {{ background: rgba(61,214,140,.15); color: var(--ok); }}
  .fail {{ background: rgba(229,77,77,.18); color: var(--fail); }}
  .skip {{ background: rgba(240,165,0,.15); color: var(--warn); }}
  .detail {{ color: var(--muted); max-width: 280px; font-size: .8rem; }}
  .warn {{
    background: rgba(240,165,0,.12); border: 1px solid rgba(240,165,0,.35);
    padding: 1rem 1.25rem; border-radius: 10px; margin-bottom: 1rem;
  }}
  pre {{ background: #0b1c2c; padding: .75rem 1rem; border-radius: 8px; overflow-x: auto; }}
  .howto {{ margin-top: 1.5rem; }}
  .howto ol {{ color: var(--muted); line-height: 1.55; }}
  footer {{ margin-top: 2rem; color: var(--muted); font-size: .8rem; }}
  a {{ color: var(--accent); }}
</style>
</head>
<body>
  <div class="accent"></div>
  <h1>Pruebas de seguridad — visual</h1>
  <p class="sub">Sistema RR. HH. Andina · OWASP Top 10:2025 · Generado {html.escape(stamp)}</p>

  <div class="grid">
    <div class="card"><div class="n">{total}</div><div class="l">Casos seguridad</div></div>
    <div class="card"><div class="n ok">{passed}</div><div class="l">Pasan</div></div>
    <div class="card"><div class="n bad">{failed}</div><div class="l">Fallan / error</div></div>
    <div class="card"><div class="n warnc">{skipped}</div><div class="l">Omitidos</div></div>
  </div>

  {empty_note}

  <table>
    <thead>
      <tr>
        <th>OWASP</th><th>Área</th><th>Suite</th><th>Método</th><th>Estado</th><th>Tiempo</th><th>Detalle</th>
      </tr>
    </thead>
    <tbody>
      {''.join(rows) if rows else '<tr><td colspan="7">Sin casos. Ejecuta mvn verify y regenera.</td></tr>'}
    </tbody>
  </table>

  <div class="card howto">
    <strong>Otras vistas visuales</strong>
    <ol>
      <li><b>IDE:</b> en Cursor/IntelliJ abre una clase <code>*Security*IT</code> → Run Test → panel verde/rojo por método.</li>
      <li><b>Swagger:</b> con la API arriba, <a href="http://127.0.0.1:8080/swagger-ui.html">http://127.0.0.1:8080/swagger-ui.html</a> — prueba SQLi/IDOR y mira el código HTTP.</li>
      <li><b>JaCoCo:</b> <code>backend/target/site/jacoco/index.html</code> (cobertura de código).</li>
      <li><b>Guía demo:</b> <code>docs/COMO_DEMOSTRAR_SEGURIDAD_SEM14.md</code></li>
    </ol>
  </div>

  <footer>RRHH Andina · Semana 14 · Regenerar: <code>python docs/scripts/generar_reporte_seguridad.py</code></footer>
</body>
</html>
"""


def main() -> None:
    cases = parse_reports(SUREFIRE) + parse_reports(FAILSAFE)
    # dedupe by suite+method
    seen = set()
    unique = []
    for c in cases:
        key = (c["suite"], c["method"])
        if key in seen:
            continue
        seen.add(key)
        unique.append(c)
    unique.sort(key=lambda x: (x["owasp"], x["suite"], x["method"]))
    content = build_html(unique)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(content, encoding="utf-8")
    DOCS_OUT.write_text(content, encoding="utf-8")
    print(f"OK -> {OUT}")
    print(f"OK -> {DOCS_OUT}")
    print(f"Casos: {len(unique)} | PASS: {sum(1 for c in unique if c['status']=='PASS')}")


if __name__ == "__main__":
    main()
