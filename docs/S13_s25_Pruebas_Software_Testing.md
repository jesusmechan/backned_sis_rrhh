# Pruebas de Software

**Unidad 4 · Semana 13 · Sesión 25**
**Curso Integrador I: Sistemas – Software**
*Universidad Tecnológica del Perú (UTP)*

> Fecha de impresión del PDF: 2/11/2026
> Pie de cada diapositiva: Curso Integrador I: Sistemas – Software | Unidad 4 · Semana 13 · Sesión 25 | Mag. Ing. Marco Antonio Flores Torres · UTP Lima Sur

---

## Diapositiva 1 — Portada

### Pruebas de Software

**Software Testing: definición y conceptos clave**

Qué son las pruebas de software y cómo se aplican los conceptos de testing durante el desarrollo de software.

- Carrera: Ingeniería de Software
- Unidad 4: Pruebas, Despliegue & Mantenimiento
- Mag. Ing. Marco Antonio Flores Torres · UTP Lima Sur
- Presencial · 60 minutos

*Encontrar el bug antes que el usuario*

(Ilustración: laptop con `assert ✓`, `@Test`, lupa con insecto y check.)

---

## Diapositiva 2 — Dinámica: Cazadores de bugs

**INICIO · 4 min**

Activa tu ojo de tester: este formulario «funciona»… pero tiene 5 bugs escondidos

**Intranet UTP — Registro de alumno**

| Campo | Valor | Observación mostrada |
|---|---|---|
| Correo institucional | `ana@` | ✓ Correo válido |
| Contraseña | `Utp2026` | (se muestra tal cual) |
| Fecha de nacimiento | `31/02/2005` | ✓ Fecha aceptada |
| Edad calculada | `-3 años` | — |
| Botón | «Ingersar» | — |

### Reto: encuentra los 5 bugs

*en tríos · 2 minutos*

1. **Formen un trío** con los compañeros más cercanos (30 s).
2. **Observen el formulario** y anoten cada bug que encuentren (2 min).
3. **Para cada bug respondan** ¿cómo lo habrían detectado antes de producción? (1 min)
4. **El trío con más bugs comparte** su lista; el resto complementa (30 s).

> *No hay respuestas malas: hoy pensamos como testers. ¡Así empieza el testing!*

---

## Diapositiva 3 — Recuperamos saberes previos

**INICIO · 3 min**

Responde en voz alta: no buscamos la respuesta perfecta, sino lo que ya sabes

| Pregunta | Nivel | Texto |
|---|---|---|
| Pregunta 1 | **Recordar** | ¿Qué haces hoy para saber si tu código «funciona»? |
| Pregunta 2 | **Comprender** | Con tus propias palabras: ¿qué es un bug? |
| Pregunta 3 | **Aplicar** | En tu proyecto integrador, ¿qué funcionalidad te daría más miedo que falle frente al cliente? ¿Por qué? |
| Pregunta 4 | **Desafío** | ¿Qué pasaría si entregamos el proyecto final sin probarlo? |

**Técnica:** lluvia de ideas. Anota en la pizarra las palabras clave que salgan (probar, error, bug, cliente…). Las retomaremos al construir la definición.

---

## Diapositiva 4 — Propósito y logro de la sesión

**UTILIDAD · 2 min**

Lo que aprenderás hoy, cómo lo demostrarás y cómo se evaluará

### Hoy aprenderemos a:

Explicar qué son las pruebas de software e identificar sus conceptos clave (error, defecto, falla, niveles, tipos y técnicas) para diseñar casos de prueba de un requisito del proyecto integrador, justificando las decisiones con los principios del testing.

**¿Por qué es importante?**
Un ingeniero de software no solo entrega código: entrega confianza. Las pruebas demuestran que el sistema hace lo que el cliente pidió.

### Evidencia de aprendizaje

Matriz de 4 casos de prueba de un requisito del proyecto (1 válido, 1 inválido, 2 de valor límite) con nivel y tipo justificados.

### Criterios de evaluación

1. Define las pruebas y distingue error, defecto y falla.
2. Clasifica correctamente el nivel y el tipo de prueba.
3. Aplica partición de equivalencia y valores límite.
4. Redacta casos completos: precondición, pasos, datos y resultado esperado.
5. Justifica sus decisiones con los principios del testing.

---

## Diapositiva 5 — Cuando el software no se prueba bien…

**UTILIDAD · 3 min**

Casos reales que cambiaron la forma de entender la calidad del software

| Año / Caso | Cifra | Qué pasó | Lección |
|---|---|---|---|
| **1996 · Ariane 5** | **37 s** | Tras el despegue el cohete se autodestruyó: un número de 64 bits no cabía en una variable de 16 bits (código reutilizado del Ariane 4). | Reutilizar código exige volver a probarlo en el nuevo contexto. |
| **2012 · Knight Capital** | **US$ 440 M** | Perdidos en unos 45 minutos por un despliegue que reactivó código antiguo en sus servidores de trading. | Pruebas de regresión y de despliegue. |
| **2024 · CrowdStrike** | **8,5 M** | De equipos Windows quedaron fuera de servicio por una actualización defectuosa: aeropuertos, bancos y hospitales. | Validar antes de liberar a producción. |
| **1985–87 · Therac-25** | **6 accidentes** | Con sobredosis de radiación causados por una condición de carrera en el software de radioterapia. | El software crítico exige pruebas rigurosas. |

**¿Y tu proyecto?** Un sistema de matrícula sin pruebas puede cobrar dos veces, perder notas o exponer datos personales. Hoy aprendemos a evitarlo.

---

## Diapositiva 6 — ¿Qué son las pruebas de software?

**TRANSFORMACIÓN · 2 min**

Concepto base: qué es, para qué sirve, por qué importa, cuándo y cómo se aplica

### DEFINICIÓN

Conjunto de actividades que evalúan un sistema o componente para verificar que cumple sus requisitos y detectar defectos antes de que lleguen al usuario.

*Basado en el Glosario ISTQB® y el estándar IEEE 829*

| Probar (testing) | ≠ | Depurar (debugging) |
|---|---|---|
| Encuentra la falla: ¿qué está mal? | | Localiza y corrige el defecto: ¿dónde y por qué? |

**Pregunta al aula:** Si un sistema pasa todas sus pruebas, ¿ya no tiene bugs?

- **¿Qué es?** Un proceso sistemático de evaluación, no algo improvisado al final.
- **¿Para qué sirve?** Detectar defectos, verificar requisitos y medir la calidad del producto.
- **¿Por qué es importante?** Reduce costos y riesgos, y genera confianza en el cliente y el equipo.
- **¿Cuándo se utiliza?** Durante todo el ciclo de vida: desde los requisitos hasta el mantenimiento.
- **¿Cómo se aplica?** Con casos de prueba planificados, ejecutados de forma manual o automatizada.

---

## Diapositiva 7 — Error → Defecto → Falla

**TRANSFORMACIÓN · 2 min**

Tres palabras que solemos llamar «bug», pero que en ingeniería significan cosas distintas

| ERROR | → | DEFECTO (bug) | → | FALLA |
|---|---|---|---|---|
| *En la persona* | | *En el código o documento* | | *En la ejecución* |
| Equivocación humana al entender o escribir algo. | | Imperfección que queda en el producto como resultado del error. | | Comportamiento incorrecto que se observa al ejecutar el defecto. |

**Ejemplo: sistema de notas UTP (aprobado con nota ≥ 12)**

| Error | Defecto | Falla |
|---|---|---|
| El programador entiende «mayor que 12». | `if (nota > 12)` | Un alumno con 12 ve: DESAPROBADO |

*Pregunta al aula: ¿todo defecto produce una falla?*

---

## Diapositiva 8 — Verificación vs. Validación

**TRANSFORMACIÓN · 1,5 min**

Dos preguntas distintas que toda estrategia de pruebas debe responder

| Verificación | vs | Validación |
|---|---|---|
| *¿Estamos construyendo el producto correctamente?* | | *¿Estamos construyendo el producto correcto?* |
| • Compara contra especificaciones: SRS, diseño, estándares. | | • Compara contra las necesidades reales del usuario. |
| • Se realiza durante el desarrollo. | | • Se realiza con el usuario o cliente. |
| • Ej.: revisión de código, inspección del SRS, pruebas unitarias. | | • Ej.: prueba de aceptación, demo con el Product Owner, prueba beta. |

**No los confundas:**

| QA · Aseguramiento de calidad | QC · Control de calidad | Testing |
|---|---|---|
| Previene defectos mejorando los procesos. *Orientado al proceso* | Detecta defectos revisando el producto. *Orientado al producto* | Actividad del QC que ejecuta y analiza el software. *Ejecuta pruebas* |

---

## Diapositiva 9 — Los 7 principios del testing (ISTQB)

**TRANSFORMACIÓN · 2,5 min**

Reglas de oro que guían cualquier estrategia de pruebas

1. **Muestran la presencia de defectos** — Probar reduce la probabilidad de bugs, pero no demuestra que no existan.
2. **Probar todo es imposible** — Las combinaciones son casi infinitas: priorizamos según el riesgo.
3. **Probar temprano ahorra dinero** — Mientras antes se detecta un defecto, más barato es corregirlo (shift-left).
4. **Los defectos se agrupan** — Pocos módulos concentran la mayoría de los bugs (≈ regla 80/20).
5. **Las pruebas se desgastan** — Repetir siempre las mismas pruebas deja de encontrar bugs: hay que renovarlas.
6. **Dependen del contexto** — No se prueba igual una app bancaria que un videojuego.
7. **La ausencia de errores es una falacia** — Un sistema sin bugs que no resuelve la necesidad del usuario igual fracasa.

**Pregunta al aula:** ¿Qué principio explica el caso Ariane 5? ¿Y la frase «en mi máquina sí funciona»?

---

## Diapositiva 10 — ¿Cuánto cuesta corregir un defecto?

**TRANSFORMACIÓN · 1,5 min**

Mientras más tarde se descubre, más caro resulta corregirlo

**Costo relativo de corregir un defecto según la fase**

| Fase | Costo relativo |
|---|---|
| Requisitos | 1× |
| Diseño | 5× |
| Codificación | 10× |
| Pruebas | 15× |
| Producción | 30× |

(Gráfico de barras; eje Y de 0 a 35.)

### Shift-left testing

Mover las pruebas a la izquierda del ciclo: revisar requisitos, diseñar casos desde el análisis y automatizar desde el primer commit.

### 30× a 100×

más caro corregir en producción que en la fase de requisitos.

*Valores referenciales ampliamente citados en la industria (p. ej., Boehm; IBM). La proporción exacta varía según el proyecto.*

---

## Diapositiva 11 — Niveles de prueba: el Modelo V

**TRANSFORMACIÓN · 2 min**

Cada fase del desarrollo tiene un nivel de prueba que la valida

| FASES DE DESARROLLO | ⟷ | NIVELES DE PRUEBA |
|---|---|---|
| Requisitos del usuario — *Necesidades del negocio* | ⟷ | Prueba de aceptación — *¿Cumple la necesidad del cliente?* |
| Requisitos del sistema — *Especificación (SRS)* | ⟷ | Prueba de sistema — *¿El sistema cumple el SRS?* |
| Diseño de arquitectura — *Módulos y capas* | ⟷ | Prueba de integración — *¿Los módulos se comunican bien?* |
| Diseño detallado — *Clases y métodos* | ⟷ | Prueba unitaria — *¿Cada método funciona aislado?* |
| **Codificación** (vértice de la V) | | |

**Quién ejecuta:** unitaria → desarrollador · integración → desarrollador / QA · sistema → equipo QA · aceptación → cliente o usuario

### Idea clave

Las pruebas se **PLANIFICAN** de izquierda a derecha (al definir cada fase) y se **EJECUTAN** de abajo hacia arriba, desde la prueba unitaria hasta la de aceptación.

---

## Diapositiva 12 — La pirámide de pruebas

**TRANSFORMACIÓN · 2 min**

¿Cuántas pruebas de cada nivel conviene automatizar? (Mike Cohn)

(Pirámide: **E2E** en la punta, **Integración** en el medio, **Unitarias** en la base.)

| Nivel | Proporción | Descripción | Herramientas |
|---|---|---|---|
| **E2E / UI** | ≈ 10 % | Simulan al usuario real recorriendo todo el sistema (navegador → API → BD). Lentas y frágiles. | Selenium · Cypress · Playwright |
| **Integración** | ≈ 20 % | Verifican que capas y componentes trabajen juntos: controller, service, repository y BD. | @SpringBootTest · MockMvc · Testcontainers |
| **Unitarias** | ≈ 70 % | Prueban un método o una clase de forma aislada. Rápidas (milisegundos) y baratas. | JUnit 5 · Mockito · AssertJ |

↑ más lentas y costosas · ↓ más rápidas y baratas

---

## Diapositiva 13 — Tipos de prueba: funcionales y no funcionales

**TRANSFORMACIÓN · 2 min**

Según QUÉ queremos comprobar del sistema

### Funcionales — *¿QUÉ hace el sistema?*

Comprueban que cada función cumple el requisito.

- Registrar un alumno con DNI válido
- Calcular el promedio ponderado
- Iniciar sesión y recuperar la contraseña

### No funcionales — *¿CÓMO lo hace?*

| Tipo | Pregunta | Herramienta |
|---|---|---|
| **Rendimiento** | ¿Responde en menos de 2 s con 500 usuarios? | JMeter |
| **Seguridad** | ¿Resiste SQL Injection o XSS? | OWASP ZAP |
| **Usabilidad** | ¿El usuario completa la tarea sin ayuda? | Pruebas con usuarios |
| **Compatibilidad** | ¿Funciona en Chrome, Firefox y móvil? | BrowserStack |
| **Carga y estrés** | ¿Qué pasa el día de la matrícula masiva? | Gatling · JMeter |
| **Accesibilidad** | ¿Se puede usar con un lector de pantalla? | WAVE · Lighthouse |

### Pruebas asociadas al cambio

- **Confirmación (re-test):** ¿se corrigió el bug?
- **Regresión:** ¿el cambio rompió algo que ya funcionaba?
- **Humo (smoke):** ¿lo básico arranca y funciona?

---

## Diapositiva 14 — Técnicas: caja negra, caja blanca y caja gris

**TRANSFORMACIÓN · 2 min**

Según CUÁNTO conocemos del código al diseñar las pruebas

| Técnica | Rol | Descripción |
|---|---|---|
| **Caja negra** | Tester / QA | Se prueba por entradas y salidas, sin ver el código. Técnicas: partición de equivalencia y valores límite. |
| **Caja blanca** | Desarrollador | Se prueba conociendo el código: caminos, decisiones y cobertura de sentencias. |
| **Caja gris** | QA técnico | Combina ambas: conoce parte de la estructura (API, BD) para diseñar mejores pruebas. |

### Estáticas vs. dinámicas

- **Estática:** Sin ejecutar el código: revisiones, inspecciones y análisis con SonarLint.
- **Dinámica:** Ejecutando el software con datos de prueba y comparando resultados.

### Manuales vs. automatizadas

- **Manual:** Una persona sigue los pasos: exploratorias y de usabilidad.
- **Automatizada:** Un script ejecuta y compara: JUnit, Selenium, dentro de CI/CD.

---

## Diapositiva 15 — Vocabulario esencial del tester

**TRANSFORMACIÓN · 1,5 min**

Términos que usarás en el taller, en tu proyecto y en el trabajo

| Término | Definición |
|---|---|
| **Caso de prueba** | Condiciones, pasos, datos y resultado esperado para verificar un requisito. |
| **Suite de pruebas** | Conjunto de casos agrupados, p. ej., todas las pruebas del módulo Matrícula. |
| **Plan de pruebas** | Documento con alcance, enfoque, recursos, cronograma y criterios de salida. |
| **Resultado esperado** | Lo que el sistema DEBE hacer; se compara con el resultado obtenido. |
| **Aserción (assert)** | Instrucción que verifica el resultado: `assertEquals("APROBADO", r)`. |
| **Cobertura de código** | % del código ejecutado por las pruebas. Alta cobertura ≠ cero bugs. |
| **Mock** | Objeto simulado que reemplaza una dependencia (BD, API) para aislar la prueba. |
| **SUT** | System Under Test: el componente que estamos probando. |
| **Severidad vs. prioridad** | Severidad = impacto técnico; prioridad = urgencia de corregir para el negocio. |

---

## Diapositiva 16 — Ciclo de vida de las pruebas (STLC)

**TRANSFORMACIÓN · 1,5 min**

Las pruebas también se planifican, diseñan, ejecutan y cierran

| # | Etapa | Descripción | Entregable |
|---|---|---|---|
| 1 | **Planificación** | Define alcance, riesgos, recursos y cronograma. | Plan de pruebas |
| 2 | **Análisis** | Estudia los requisitos y decide QUÉ probar. | Condiciones de prueba |
| 3 | **Diseño** *(destacada)* | Define CÓMO probar: casos y datos de prueba. | Casos de prueba |
| 4 | **Implementación** | Prepara el entorno, los datos y los scripts. | Entorno y scripts |
| 5 | **Ejecución** | Ejecuta, compara resultados y reporta defectos. | Reporte de defectos |
| 6 | **Cierre** | Evalúa criterios de salida y lecciones aprendidas. | Informe de cierre |

**En nuestro curso:** hoy practicamos el análisis y el diseño (casos de prueba). En la sesión 26 haremos la implementación y la ejecución con JUnit 5 → avance del proyecto al 60 %.

---

## Diapositiva 17 — Aplicación profesional: el testing en tu carrera

**TRANSFORMACIÓN · 1,5 min**

Cómo usa este conocimiento un(a) ingeniero(a) de software

### ¿Quién prueba en un equipo de software?

- **QA Analyst:** Diseña y ejecuta casos manuales y exploratorios; reporta defectos.
- **QA Automation Engineer:** Automatiza la regresión con Selenium, Cypress o REST Assured.
- **Desarrollador(a):** Escribe pruebas unitarias y de integración; aplica TDD.
- **DevOps:** Ejecuta las pruebas automáticamente en cada commit (CI/CD).

### ¿Dónde lo aplicarás?

- **Banca móvil:** Transferencias, pagos con QR y límites diarios por cliente.
- **Facturación electrónica:** Validación del RUC y cálculo del IGV (18 %) en cada comprobante.
- **E-commerce y delivery:** Carrito, stock, pasarela de pagos y tiempos de respuesta en campañas.
- **Tu proyecto integrador:** Cada requisito del SRS debe tener al menos un caso de prueba.

---

## Diapositiva 18 — Ejemplo 1 · Anatomía de un caso de prueba

**TRANSFORMACIÓN · 1 min**

Comprensión básica: cómo se ve un caso de prueba bien redactado

| Campo | Contenido |
|---|---|
| **ID** | `CP-LOGIN-01` |
| **Título** | Inicio de sesión con credenciales válidas |
| **Requisito** | RF-01: El alumno inicia sesión con su código y contraseña |
| **Precondición** | El alumno U20231234 está registrado y activo |
| **Pasos** | 1. Abrir /login 2. Ingresar código y contraseña 3. Clic en «Ingresar» |
| **Datos de prueba** | Código: U20231234 · Contraseña: Clave#2026 |
| **Resultado esperado** | Se muestra el panel principal con el nombre del alumno |
| **Resultado obtenido** | Se muestra el panel principal con el nombre del alumno |
| **Estado** | **PASÓ ✔** |

### Cómo leerlo paso a paso

1. **Identifica:** cada caso tiene un ID y se traza a un requisito.
2. **Prepara:** precondición y datos permiten repetir la prueba.
3. **Ejecuta:** pasos concretos que cualquiera puede seguir.
4. **Compara:** resultado esperado vs. obtenido → PASÓ o FALLÓ.

**Pregunta al aula:** ¿Cómo redactarías CP-LOGIN-02 con contraseña incorrecta?

---

## Diapositiva 19 — Ejemplo 2 · Partición de equivalencia y valores límite

**TRANSFORMACIÓN · 2 min**

Aplicación práctica: diseñar pocos casos que encuentren muchos defectos

**Problema · RF-07:** El sistema registra notas de 0 a 20 y muestra APROBADO si la nota es mayor o igual a 12 (nota mínima aprobatoria UTP).

### Datos: clases de equivalencia

| ✖ Inválida baja | Desaprobado (0 – 11) | Aprobado (12 – 20) | ✖ Inválida alta |
|---|---|---|---|
| ≤ -1 | 0 … 11 | 12 … 20 | ≥ 21 |

Valores límite (▲): **-1, 0, 11, 12, 20, 21** · ✖ = clase inválida

### Procedimiento

1. Identifica las clases: 1 inválida baja, 2 válidas y 1 inválida alta.
2. Toma un valor representativo de cada clase válida.
3. Agrega los valores límite: en el borde y a cada lado.
4. Interpreta: si el código usa `>` en vez de `>=`, el caso con 12 falla.

### Solución: casos de prueba

| CP | Nota | Resultado esperado |
|---|---|---|
| CP-01 | -1 | Error: fuera de rango |
| CP-02 | 0 | DESAPROBADO |
| CP-03 | 11 | DESAPROBADO |
| **CP-04** | **12** | **APROBADO** |
| CP-05 | 20 | APROBADO |
| CP-06 | 21 | Error: fuera de rango |
| CP-07 | 15 | APROBADO (representativo) |

*7 casos bien elegidos en lugar de probar los 21 valores uno por uno.*

---

## Diapositiva 20 — Ejemplo 3 · Caso profesional: automatizamos con JUnit 5

**TRANSFORMACIÓN · 2 min**

El equipo implementó NotaService en Spring Boot: convertimos los casos del Ejemplo 2 en pruebas automáticas

### Código bajo prueba (SUT) — contiene un defecto

```java
public String evaluar(int nota) {
    if (nota < 0 || nota > 20)
        throw new IllegalArgumentException("Nota inválida");
    return (nota > 12) ? "APROBADO" : "DESAPROBADO"; // defecto
}
```

### Prueba automatizada — patrón AAA

```java
@Test
@DisplayName("Nota 12 debe ser APROBADO (valor límite)")
void notaDoceEsAprobado() {
    int nota = 12;                          // Arrange
    String r = service.evaluar(nota);       // Act
    assertEquals("APROBADO", r);            // Assert
}
@Test
void notaVeintiunoLanzaError() {
    assertThrows(IllegalArgumentException.class,
            () -> service.evaluar(21));
}
```

### Patrón AAA

- **Arrange** — Preparar los datos de entrada.
- **Act** — Ejecutar el método bajo prueba.
- **Assert** — Comparar obtenido vs. esperado.

### Resultado en IntelliJ IDEA

- ✖ `notaDoceEsAprobado`
  - expected: `<APROBADO>`
  - but was: `<DESAPROBADO>`
- ✔ `notaVeintiunoLanzaError`

**La prueba detectó el defecto antes de producción.** Corregimos a `nota >= 12` y todo queda en verde.

---

## Diapositiva 21 — Actividad: diseñamos casos de prueba para nuestro proyecto

**PRÁCTICA · 11 min**

Objetivo: aplicar niveles, tipos y técnicas de prueba a un requisito real de tu proyecto integrador

- **Modalidad:** Equipos del proyecto
- **Tiempo:** 11 minutos
- **Evidencia:** Matriz de 4 CP

### Instrucciones

| # | Instrucción | Tiempo |
|---|---|---|
| 1 | **Elijan un requisito funcional** de su SRS (p. ej., registrar pedido). | 1′ |
| 2 | **Indiquen el nivel y el tipo** de prueba que corresponde. | 2′ |
| 3 | **Apliquen partición de equivalencia** y valores límite al dato principal. | 3′ |
| 4 | **Redacten 4 casos:** 1 válido, 1 inválido y 2 de valor límite. | 4′ |
| 5 | **Un vocero presenta un caso** en 30 segundos al aula. | 1′ |

### Plantilla de la matriz (cópiala en Excel o Word)

| ID | Requisito | Nivel / tipo | Datos de entrada | Resultado esperado |
|---|---|---|---|---|
| CP-01 | *RF-07 Registrar nota* | *Unitaria · funcional* | *nota = 12* | *APROBADO* |
| CP-02 | | | | |
| CP-03 | | | | |
| CP-04 | | | | |

### Elige cómo explicarla

- **Texto** — Matriz en Excel o Word
- **Ilustración** — Pirámide de tu proyecto en draw.io
- **Voz** — Audio de 60 s explicando un caso
- **Video** — Screencast de 2 min

---

## Diapositiva 22 — Ejercicios de aplicación por nivel

**PRÁCTICA · 1 min**

Desarrollo autónomo: se revisan al inicio de la sesión 26 (Taller de testing)

### Ejercicio 1 — Básico

**Comprensión**

Clasifica cada situación como error, defecto o falla:

- a) El analista entendió mal la regla del IGV.
- b) El código calcula total * 0.18 en lugar de subtotal * 0.18.
- c) La boleta muestra al cliente un IGV mayor al correcto.

**Entrega:** Respuesta breve y justificada

### Ejercicio 2 — Intermedio

**Aplicación**

Requisito: «La edad para registrarse en la app debe estar entre 18 y 65 años». Identifica las clases de equivalencia y los valores límite, y diseña 6 casos de prueba con su resultado esperado.

**Entrega:** Matriz de 6 casos de prueba

### Ejercicio 3 — Profesional

**Análisis y propuesta**

Una app de delivery en Lima lanzará pagos con QR en 2 semanas. Propón una estrategia de pruebas: niveles (pirámide), 2 pruebas no funcionales clave, qué automatizarías y con qué herramientas Java. Justifica con 2 principios del testing.

**Entrega:** Estrategia en 1 página

---

## Diapositiva 23 — ¿Qué aprendimos hoy?

**CIERRE · 1,5 min**

Síntesis de la sesión: cinco ideas para tu proyecto y tu carrera

- ✔ Qué son las pruebas de software y por qué probar ≠ depurar.
- ✔ La cadena error → defecto → falla y la diferencia entre verificar y validar.
- ✔ Los 7 principios que guían cualquier estrategia de pruebas.
- ✔ Niveles (modelo V, pirámide), tipos y técnicas de caja negra y blanca.
- ✔ Cómo diseñar y automatizar casos de prueba con valores límite y JUnit 5.

> **Probar no es desconfiar del código: es demostrar que funciona.**

**3 ideas para llevar**

- Probar temprano
- Probar con criterio (riesgo)
- Automatizar lo repetible

---

## Diapositiva 24 — Comprobamos nuestro aprendizaje

**CIERRE · 2,5 min**

Responde de forma individual en 2 minutos; luego comparamos las respuestas

- **P1 · Comprensión:** ¿Cuál es la diferencia entre error, defecto y falla? Ejemplifícalo con el sistema de notas.
- **P2 · Aplicación:** Requisito: «el descuento aplica a compras desde S/ 100 hasta S/ 500». ¿Qué valores límite probarías y por qué?
- **P3 · Análisis:** Tu equipo tiene poco tiempo: ¿por qué conviene tener más pruebas unitarias que E2E? Justifica con la pirámide y un principio del testing.

---

## Diapositiva 25 — Reflexión, retroalimentación y conclusión

**CIERRE · 1,5 min**

Construimos juntos las conclusiones de la sesión

### Reflexión

¿En qué situación real de tu futura profesión podrías utilizar lo aprendido hoy?

Conocimiento ↓ Aplicación ↓ Profesión

### Retroalimentación

Semáforo del aprendizaje: completa una frase en un post-it o en el foro.

- Lo tengo claro…
- Aún me cuesta…
- Necesito ayuda con…

### Conclusión

Las pruebas de software son la evidencia de la calidad: detectan defectos a tiempo, reducen costos y generan confianza. Como ingeniero(a), diseñarás y automatizarás pruebas desde el primer requisito de tu proyecto.

---

## Diapositiva 26 — Cierre y próxima sesión

### Un buen ingeniero no solo escribe código que funciona: demuestra que funciona.

```java
@Test
void tuProgreso() {
    assertTrue(practica > excusas);
}
```

*Escribe tu primer caso de prueba esta semana: cada prueba que agregas hoy es un problema que evitas mañana.*

### ¡Gracias por tu participación!

Tu esfuerzo de hoy es la base de tu proyecto. Sigue asistiendo y participando: cada sesión suma a tu formación como ingeniero(a) de software.

**Próxima sesión 26 — Taller de testing**

Avance del proyecto al 60 % demostrando el uso de pruebas de software.

**Trae:** tu proyecto Spring Boot en IntelliJ, JDK 17+ y la matriz de casos de hoy.
