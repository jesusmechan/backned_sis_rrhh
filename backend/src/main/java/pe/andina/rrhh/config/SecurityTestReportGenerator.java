package pe.andina.rrhh.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Genera {@code docs/seguridad-report.html} (y copia en {@code target/}) a partir de los XML
 * reales de Surefire/Failsafe. Se invoca tras {@code mvn test/verify} y al arrancar la API.
 */
public final class SecurityTestReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(SecurityTestReportGenerator.class);

    private static final Map<String, String[]> SEC_CLASSES = Map.of(
            "SecurityAccessControlIT", new String[]{"A01", "Control de acceso / IDOR"},
            "SecurityInjectionIT", new String[]{"A05", "Inyección SQL / XSS"},
            "SecurityHeadersIT", new String[]{"A02", "Cabeceras HTTP"},
            "SecurityAuthHardeningIT", new String[]{"A07", "JWT / autenticación"},
            "RateLimitFilterTest", new String[]{"A07", "Rate limit (fuerza bruta)"}
    );

    private static final Pattern TESTCASE = Pattern.compile(
            "<testcase\\b([^>]*)>(.*?)</testcase>|<testcase\\b([^>]*)/>",
            Pattern.DOTALL);
    private static final Pattern ATTR = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private SecurityTestReportGenerator() {
    }

    public static Path generate() {
        Path backendRoot = resolveBackendRoot();
        Path surefire = backendRoot.resolve("target/surefire-reports");
        Path failsafe = backendRoot.resolve("target/failsafe-reports");
        List<CaseRow> cases = new ArrayList<>();
        cases.addAll(parseReports(surefire));
        cases.addAll(parseReports(failsafe));
        cases = dedupe(cases);
        cases.sort(Comparator
                .comparing((CaseRow c) -> c.owasp)
                .thenComparing(c -> c.suite)
                .thenComparing(c -> c.method));

        String html = buildHtml(cases);
        Path targetOut = backendRoot.resolve("target/seguridad-report.html");
        Path docsOut = backendRoot.getParent().resolve("docs/seguridad-report.html");
        try {
            Files.createDirectories(targetOut.getParent());
            Files.writeString(targetOut, html, StandardCharsets.UTF_8);
            if (docsOut.getParent() != null) {
                Files.createDirectories(docsOut.getParent());
                Files.writeString(docsOut, html, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el reporte de seguridad", e);
        }

        long pass = cases.stream().filter(c -> "PASS".equals(c.status)).count();
        log.info("Reporte de seguridad generado: {} caso(s), {} PASA → {}",
                cases.size(), pass, docsOut.toAbsolutePath().normalize());
        return docsOut;
    }

    public static void main(String[] args) {
        Path out = generate();
        System.out.println("OK -> " + out.toAbsolutePath().normalize());
    }

    private static Path resolveBackendRoot() {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.isDirectory(cwd.resolve("target")) && Files.isRegularFile(cwd.resolve("pom.xml"))) {
            return cwd;
        }
        Path nested = cwd.resolve("backend");
        if (Files.isRegularFile(nested.resolve("pom.xml"))) {
            return nested;
        }
        return cwd;
    }

    private static List<CaseRow> parseReports(Path folder) {
        List<CaseRow> results = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            return results;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "TEST-*.xml")) {
            for (Path xmlPath : stream) {
                String content = Files.readString(xmlPath, StandardCharsets.UTF_8);
                String suiteAttr = firstAttr(content, "testsuite", "name");
                String shortName = suiteAttr.contains(".")
                        ? suiteAttr.substring(suiteAttr.lastIndexOf('.') + 1)
                        : suiteAttr;
                if (!SEC_CLASSES.containsKey(shortName)
                        && !shortName.contains("Security")
                        && !shortName.contains("RateLimit")) {
                    continue;
                }
                String[] meta = SEC_CLASSES.getOrDefault(shortName, new String[]{"—", shortName});
                Matcher m = TESTCASE.matcher(content);
                while (m.find()) {
                    String attrs = m.group(1) != null ? m.group(1) : m.group(3);
                    String body = m.group(2) != null ? m.group(2) : "";
                    Map<String, String> map = attrs(attrs);
                    String status;
                    String detail = "";
                    if (body.contains("<failure")) {
                        status = "FAIL";
                        detail = extractMessage(body, "failure");
                    } else if (body.contains("<error")) {
                        status = "ERROR";
                        detail = extractMessage(body, "error");
                    } else if (body.contains("<skipped")) {
                        status = "SKIP";
                        detail = extractMessage(body, "skipped");
                    } else {
                        status = "PASS";
                    }
                    results.add(new CaseRow(
                            shortName,
                            meta[0],
                            meta[1],
                            map.getOrDefault("name", ""),
                            map.getOrDefault("classname", suiteAttr),
                            status,
                            map.getOrDefault("time", "0"),
                            detail.length() > 280 ? detail.substring(0, 280) : detail
                    ));
                }
            }
        } catch (IOException e) {
            log.warn("No se pudieron leer reportes en {}: {}", folder, e.getMessage());
        }
        return results;
    }

    private static String firstAttr(String xml, String tag, String attr) {
        Pattern p = Pattern.compile("<" + tag + "\\b([^>]*)>", Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        if (!m.find()) {
            return "";
        }
        return attrs(m.group(1)).getOrDefault(attr, "");
    }

    private static Map<String, String> attrs(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null) {
            return map;
        }
        Matcher m = ATTR.matcher(raw);
        while (m.find()) {
            map.put(m.group(1), m.group(2));
        }
        return map;
    }

    private static String extractMessage(String body, String tag) {
        Matcher m = Pattern.compile("<" + tag + "\\b[^>]*message=\"([^\"]*)\"", Pattern.DOTALL).matcher(body);
        return m.find() ? unescapeXml(m.group(1)) : "";
    }

    private static String unescapeXml(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
    }

    private static List<CaseRow> dedupe(List<CaseRow> cases) {
        Map<String, CaseRow> unique = new LinkedHashMap<>();
        for (CaseRow c : cases) {
            unique.putIfAbsent(c.suite + "#" + c.method, c);
        }
        return new ArrayList<>(unique.values());
    }

    private static String buildHtml(List<CaseRow> cases) {
        int total = cases.size();
        long passed = cases.stream().filter(c -> "PASS".equals(c.status)).count();
        long failed = cases.stream().filter(c -> "FAIL".equals(c.status) || "ERROR".equals(c.status)).count();
        long skipped = cases.stream().filter(c -> "SKIP".equals(c.status)).count();
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder rows = new StringBuilder();
        for (CaseRow c : cases) {
            String badge = switch (c.status) {
                case "PASS" -> "<span class=\"badge pass\">PASA</span>";
                case "FAIL" -> "<span class=\"badge fail\">FALLA</span>";
                case "ERROR" -> "<span class=\"badge fail\">ERROR</span>";
                case "SKIP" -> "<span class=\"badge skip\">SKIP</span>";
                default -> esc(c.status);
            };
            rows.append("<tr>")
                    .append("<td><code>").append(esc(c.owasp)).append("</code></td>")
                    .append("<td>").append(esc(c.label)).append("</td>")
                    .append("<td><code>").append(esc(c.suite)).append("</code></td>")
                    .append("<td>").append(esc(c.method)).append("</td>")
                    .append("<td>").append(badge).append("</td>")
                    .append("<td>").append(esc(c.time)).append("s</td>")
                    .append("<td class=\"detail\">").append(esc(c.detail)).append("</td>")
                    .append("</tr>\n");
        }

        String emptyNote = "";
        if (cases.isEmpty()) {
            emptyNote = """
                    <div class="warn">
                      <strong>Sin resultados XML aún.</strong>
                      Ejecuta <code>mvn verify</code> en <code>backend</code> y vuelve a abrir este archivo
                      (o reinicia la API: al arrancar regenera el reporte si hay XML).
                    </div>
                    """;
        }

        String bodyRows = rows.length() == 0
                ? "<tr><td colspan=\"7\">Sin casos. Ejecuta mvn verify.</td></tr>"
                : rows.toString();

        return String.format(Locale.ROOT, """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1"/>
                <title>Dashboard pruebas de seguridad — RRHH Andina</title>
                <style>
                  :root {
                    --bg: #0b1c2c; --card: #122b40; --text: #f5f7fa; --muted: #9ab0c4;
                    --accent: #1ab3a6; --ok: #3dd68c; --fail: #e54d4d; --warn: #f0a500;
                  }
                  * { box-sizing: border-box; }
                  body {
                    margin: 0; font-family: "Segoe UI", system-ui, sans-serif;
                    background: linear-gradient(160deg, #0b1c2c 0%%, #0f2438 50%%, #122b40 100%%);
                    color: var(--text); min-height: 100vh; padding: 2rem;
                  }
                  h1 { font-size: 1.75rem; margin: 0 0 .25rem; }
                  .sub { color: var(--muted); margin-bottom: 1.5rem; }
                  .accent { width: 4rem; height: .25rem; background: var(--accent); border-radius: 2px; margin-bottom: 1rem; }
                  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }
                  .card {
                    background: var(--card); border-radius: 12px; padding: 1.1rem 1.25rem;
                    border: 1px solid #1e3a54;
                  }
                  .card .n { font-size: 2rem; font-weight: 700; }
                  .card .l { color: var(--muted); font-size: .85rem; }
                  .ok { color: var(--ok); } .bad { color: var(--fail); } .warnc { color: var(--warn); }
                  table { width: 100%%; border-collapse: collapse; background: var(--card); border-radius: 12px; overflow: hidden; }
                  th, td { padding: .75rem .9rem; text-align: left; border-bottom: 1px solid #1e3a54; font-size: .9rem; vertical-align: top; }
                  th { background: #153d52; color: var(--accent); font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; }
                  tr:last-child td { border-bottom: none; }
                  code { font-family: Consolas, monospace; font-size: .85em; }
                  .badge {
                    display: inline-block; padding: .2rem .55rem; border-radius: 999px;
                    font-size: .75rem; font-weight: 700;
                  }
                  .pass { background: rgba(61,214,140,.15); color: var(--ok); }
                  .fail { background: rgba(229,77,77,.18); color: var(--fail); }
                  .skip { background: rgba(240,165,0,.15); color: var(--warn); }
                  .detail { color: var(--muted); max-width: 280px; font-size: .8rem; }
                  .warn {
                    background: rgba(240,165,0,.12); border: 1px solid rgba(240,165,0,.35);
                    padding: 1rem 1.25rem; border-radius: 10px; margin-bottom: 1rem;
                  }
                  footer { margin-top: 2rem; color: var(--muted); font-size: .8rem; }
                </style>
                </head>
                <body>
                  <div class="accent"></div>
                  <h1>Pruebas de seguridad — visual</h1>
                  <p class="sub">Sistema RR. HH. Andina · OWASP Top 10:2025 · Generado %s (datos de la última corrida Maven)</p>
                  <div class="grid">
                    <div class="card"><div class="n">%d</div><div class="l">Casos seguridad</div></div>
                    <div class="card"><div class="n ok">%d</div><div class="l">Pasan</div></div>
                    <div class="card"><div class="n bad">%d</div><div class="l">Fallan / error</div></div>
                    <div class="card"><div class="n warnc">%d</div><div class="l">Omitidos</div></div>
                  </div>
                  %s
                  <table>
                    <thead>
                      <tr>
                        <th>OWASP</th><th>Área</th><th>Suite</th><th>Método</th><th>Estado</th><th>Tiempo</th><th>Detalle</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                    </tbody>
                  </table>
                  <footer>Regeneración automática: <code>mvn test</code> / <code>mvn verify</code> y al arrancar la API · %s</footer>
                </body>
                </html>
                """,
                esc(stamp),
                total,
                passed,
                failed,
                skipped,
                emptyNote,
                bodyRows,
                esc(stamp)
        );
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record CaseRow(
            String suite,
            String owasp,
            String label,
            String method,
            String clazz,
            String status,
            String time,
            String detail
    ) {
    }
}
