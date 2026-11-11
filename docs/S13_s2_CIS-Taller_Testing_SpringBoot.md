# TALLER DE TESTING

**Universidad Tecnológica del Perú Lima Sur**
**Docente:** Magister Ingeniero Marco Antonio Flores Torres

## Aplicación Web Java con Spring Boot y MySQL

### Sistema de Registro de Notas

Curso Integrador de Software I | Carrera: Ingeniería de Software
Universidad Tecnológica del Perú Lima Sur
Docente: Magister Ingeniero Marco Antonio Flores Torres
2026

---

## 📋 Información General del Taller

### Objetivos de Aprendizaje

Al finalizar este taller, el estudiante será capaz de:

- Configurar un proyecto Spring Boot con dependencias de testing.
- Escribir pruebas unitarias con JUnit 5 y Mockito para la capa de servicio.
- Implementar pruebas de integración para repositorios JPA con H2 en memoria.
- Realizar pruebas de la capa web (Controller) con MockMvc.
- Calcular y analizar la cobertura de código con JaCoCo.
- Aplicar buenas prácticas de testing en proyectos de software reales.

### ¿Qué vamos a construir?

Desarrollaremos paso a paso una aplicación Java Web llamada **Sistema de Registro de Notas**. Esta aplicación permitirá gestionar estudiantes, cursos y calificaciones. Sobre ella aplicaremos distintos tipos de pruebas de software.

| Elemento | Detalle |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.2 |
| Base de datos | MySQL 8 (producción) + H2 (testing) |
| Testing unitario | JUnit 5 + Mockito |
| Testing web | Spring MockMvc |
| Cobertura | JaCoCo |
| Gestor de proyectos | Maven |
| IDE recomendado | IntelliJ IDEA o VS Code |

### Tipos de Pruebas que Aplicaremos

**Pirámide de Testing**

1. **Pruebas Unitarias** → Prueban una sola clase o método de forma aislada.
2. **Pruebas de Integración** → Verifican la interacción entre capas (Service + Repository).
3. **Pruebas de Controlador** → Simulan llamadas HTTP a los endpoints REST.

---

## 🏗️ Parte 1: Estructura del Proyecto

### 1.1 Arquitectura de la Aplicación

La aplicación sigue una arquitectura en capas (Layered Architecture), muy común en aplicaciones Spring Boot empresariales:

| Capa | Responsabilidad |
|---|---|
| Controller (`@RestController`) | Recibe peticiones HTTP, valida datos de entrada y devuelve respuestas JSON. |
| Service (`@Service`) | Contiene la lógica de negocio. Es la capa más importante para testear. |
| Repository (`@Repository`) | Accede a la base de datos mediante Spring Data JPA. |
| Entity (`@Entity`) | Representa las tablas de la base de datos como objetos Java. |
| DTO (Data Transfer Object) | Objetos simples para transferir datos entre capas sin exponer entidades. |

### 1.2 Estructura de Carpetas del Proyecto

Así quedará organizado nuestro proyecto Maven en IntelliJ IDEA:

```
registro-notas/
├── src/
│ ├── main/
│ │ ├── java/pe/edu/utp/registronotas/
│ │ │ ├── controller/
│ │ │ │ └── NotaController.java
│ │ │ ├── service/
│ │ │ │ ├── NotaService.java ← Interfaz
│ │ │ │ └── NotaServiceImpl.java ← Implementación
│ │ │ ├── repository/
│ │ │ │ └── NotaRepository.java
│ │ │ ├── entity/
│ │ │ │ ├── Estudiante.java
│ │ │ │ ├── Curso.java
│ │ │ │ └── Nota.java
│ │ │ ├── dto/
│ │ │ │ └── NotaDTO.java
│ │ │ └── RegistroNotasApplication.java
│ │ └── resources/
│ │ └── application.properties
│ └── test/
│ ├── java/pe/edu/utp/registronotas/
│ │ ├── service/
│ │ │ └── NotaServiceTest.java ← Pruebas unitarias
│ │ ├── repository/
│ │ │ └── NotaRepositoryTest.java ← Pruebas de integración
│ │ └── controller/
│ │ └── NotaControllerTest.java ← Pruebas de controlador
│ └── resources/
│ └── application-test.properties
└── pom.xml
```

---

## ⚙️ Parte 2: Configuración del Proyecto

### 2.1 Crear el Proyecto en Spring Initializr

Ingresar a https://start.spring.io y configurar lo siguiente:

| Campo | Valor |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.2.x (última estable) |
| Group | pe.edu.utp |
| Artifact | registro-notas |
| Java version | 17 |
| Packaging | Jar |

**Dependencias a seleccionar:**

- Spring Web (para crear endpoints REST)
- Spring Data JPA (para acceso a base de datos)
- MySQL Driver (conector con MySQL)
- Spring Boot DevTools (recarga automática en desarrollo)
- Validation (validaciones de campos)

Hacer clic en **Generate** → descomprimir el ZIP → abrir en IntelliJ IDEA.

### 2.2 Archivo pom.xml — Configuración Maven

El archivo `pom.xml` define las dependencias del proyecto. Agregar manualmente las dependencias de testing que no se incluyen por defecto:

```xml
<!-- =====================================================
 DEPENDENCIAS PRINCIPALES
====================================================== -->
<!-- Spring Boot Starter Web: crea APIs REST -->
<dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- Spring Data JPA: acceso a base de datos -->
<dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<!-- Driver MySQL: conexión a la base de datos -->
<dependency>
 <groupId>com.mysql</groupId>
 <artifactId>mysql-connector-j</artifactId>
 <scope>runtime</scope>
</dependency>
<!-- =====================================================
 DEPENDENCIAS DE TESTING
====================================================== -->
<!-- Spring Boot Test: incluye JUnit 5 + Mockito + AssertJ -->
<dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-test</artifactId>
 <scope>test</scope>
</dependency>
<!-- H2: base de datos en memoria solo para pruebas -->
<dependency>
 <groupId>com.h2database</groupId>
 <artifactId>h2</artifactId>
 <scope>test</scope>
</dependency>
```

Agregar el plugin JaCoCo para cobertura de código:

```xml
<build>
 <plugins>
 <!-- Plugin principal de Spring Boot -->
 <plugin>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-maven-plugin</artifactId>
 </plugin>
 <!-- JaCoCo: genera reporte de cobertura de código -->
 <plugin>
 <groupId>org.jacoco</groupId>
 <artifactId>jacoco-maven-plugin</artifactId>
 <version>0.8.11</version>
 <executions>
 <execution>
 <goals><goal>prepare-agent</goal></goals>
 </execution>
 <execution>
 <id>report</id>
 <phase>test</phase>
 <goals><goal>report</goal></goals>
 </execution>
 </executions>
 </plugin>
 </plugins>
</build>
```

### 2.3 Configuración de Base de Datos

**application.properties** (base de datos MySQL para producción):

```properties
# Conexión a MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/registro_notas
spring.datasource.username=root
spring.datasource.password=tu_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
# Puerto del servidor
server.port=8080
```

**application-test.properties** (base de datos H2 para pruebas):

Crear este archivo en `src/test/resources/` para que las pruebas no afecten la base de datos real:

```properties
# H2: base de datos en memoria (se crea y destruye en cada prueba)
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
# Hibernate crea las tablas automáticamente en H2
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

> 💡 **¿Por qué usamos H2 para las pruebas?**
>
> → No necesitamos tener MySQL instalado para ejecutar las pruebas.
> → Cada prueba comienza con una base de datos limpia (sin datos previos).
> → Las pruebas son más rápidas porque H2 vive en memoria RAM.
> → Así no corremos el riesgo de borrar datos reales por error.

---

## 🗄️ Parte 3: Entidades, Repositorios y Servicios

### 3.1 Entidad Nota.java

La entidad representa la tabla `notas` en la base de datos. Cada campo se convierte en una columna.

```java
package pe.edu.utp.registronotas.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "notas")
public class Nota {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @NotBlank(message = "El nombre del estudiante es obligatorio")
 @Column(nullable = false)
 private String nombreEstudiante;

 @NotBlank(message = "El curso es obligatorio")
 @Column(nullable = false)
 private String curso;

 @NotNull(message = "La nota es obligatoria")
 @Min(value = 0, message = "La nota mínima es 0")
 @Max(value = 20, message = "La nota máxima es 20")
 @Column(nullable = false)
 private Double nota;

 @Column
 private String periodo; // Ejemplo: "2025-I"

 // ─── Constructores ───────────────────────────────
 public Nota() {}

 public Nota(String nombreEstudiante, String curso,
 Double nota, String periodo) {
 this.nombreEstudiante = nombreEstudiante;
 this.curso = curso;
 this.nota = nota;
 this.periodo = periodo;
 }

 // ─── Getters y Setters ───────────────────────────
 public Long getId() { return id; }
 public void setId(Long id) { this.id = id; }
 public String getNombreEstudiante() { return nombreEstudiante; }
 public void setNombreEstudiante(String n) { this.nombreEstudiante = n; }
 public String getCurso() { return curso; }
 public void setCurso(String c) { this.curso = c; }
 public Double getNota() { return nota; }
 public void setNota(Double n) { this.nota = n; }
 public String getPeriodo() { return periodo; }
 public void setPeriodo(String p) { this.periodo = p; }
}
```

### 3.2 Repositorio NotaRepository.java

El repositorio extiende `JpaRepository` y hereda automáticamente métodos como `save()`, `findById()`, `findAll()` y `delete()`. Además, podemos declarar consultas personalizadas:

```java
package pe.edu.utp.registronotas.repository;
import pe.edu.utp.registronotas.entity.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotaRepository extends JpaRepository<Nota, Long> {

 // Spring genera el SQL automáticamente desde el nombre del método
 List<Nota> findByCurso(String curso);
 List<Nota> findByNombreEstudiante(String nombreEstudiante);
 List<Nota> findByNotaGreaterThanEqual(Double notaMinima);

 // Consulta JPQL personalizada
 @Query("SELECT AVG(n.nota) FROM Nota n WHERE n.curso = :curso")
 Double calcularPromedioPorCurso(String curso);
}
```

### 3.3 Servicio NotaService.java (Interfaz)

Definimos la interfaz primero para poder crear mocks fácilmente durante el testing:

```java
package pe.edu.utp.registronotas.service;
import pe.edu.utp.registronotas.entity.Nota;
import java.util.List;
import java.util.Optional;

public interface NotaService {
 Nota guardar(Nota nota);
 List<Nota> listarTodas();
 Optional<Nota> buscarPorId(Long id);
 List<Nota> buscarPorCurso(String curso);
 Nota actualizar(Long id, Nota notaActualizada);
 void eliminar(Long id);
 Double calcularPromedio(String curso);
}
```

### 3.4 Implementación NotaServiceImpl.java

Aquí va la lógica de negocio. Esta clase es la más importante para las pruebas unitarias:

```java
package pe.edu.utp.registronotas.service;
import pe.edu.utp.registronotas.entity.Nota;
import pe.edu.utp.registronotas.repository.NotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NotaServiceImpl implements NotaService {

 @Autowired
 private NotaRepository notaRepository;

 @Override
 public Nota guardar(Nota nota) {
 return notaRepository.save(nota);
 }

 @Override
 public List<Nota> listarTodas() {
 return notaRepository.findAll();
 }

 @Override
 public Optional<Nota> buscarPorId(Long id) {
 return notaRepository.findById(id);
 }

 @Override
 public List<Nota> buscarPorCurso(String curso) {
 return notaRepository.findByCurso(curso);
 }

 @Override
 public Nota actualizar(Long id, Nota notaActualizada) {
 Nota existente = notaRepository.findById(id)
 .orElseThrow(() -> new RuntimeException("Nota no encontrada: " + id));
 existente.setNombreEstudiante(notaActualizada.getNombreEstudiante());
 existente.setCurso(notaActualizada.getCurso());
 existente.setNota(notaActualizada.getNota());
 existente.setPeriodo(notaActualizada.getPeriodo());
 return notaRepository.save(existente);
 }

 @Override
 public void eliminar(Long id) {
 notaRepository.deleteById(id);
 }

 @Override
 public Double calcularPromedio(String curso) {
 Double promedio = notaRepository.calcularPromedioPorCurso(curso);
 return promedio != null ? promedio : 0.0;
 }
}
```

### 3.5 Controlador NotaController.java

El controlador expone los endpoints REST que consumirá el frontend o Postman:

```java
package pe.edu.utp.registronotas.controller;
import pe.edu.utp.registronotas.entity.Nota;
import pe.edu.utp.registronotas.service.NotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notas")
public class NotaController {

 @Autowired
 private NotaService notaService;

 // GET /api/notas → Listar todas
 @GetMapping
 public List<Nota> listar() {
 return notaService.listarTodas();
 }

 // POST /api/notas → Crear nueva nota
 @PostMapping
 public ResponseEntity<Nota> crear(@RequestBody Nota nota) {
 Nota guardada = notaService.guardar(nota);
 return ResponseEntity.ok(guardada);
 }

 // GET /api/notas/{id} → Buscar por ID
 @GetMapping("/{id}")
 public ResponseEntity<Nota> buscar(@PathVariable Long id) {
 return notaService.buscarPorId(id)
 .map(ResponseEntity::ok)
 .orElse(ResponseEntity.notFound().build());
 }

 // PUT /api/notas/{id} → Actualizar
 @PutMapping("/{id}")
 public ResponseEntity<Nota> actualizar(
 @PathVariable Long id,
 @RequestBody Nota nota) {
 Nota actualizada = notaService.actualizar(id, nota);
 return ResponseEntity.ok(actualizada);
 }

 // DELETE /api/notas/{id} → Eliminar
 @DeleteMapping("/{id}")
 public ResponseEntity<Void> eliminar(@PathVariable Long id) {
 notaService.eliminar(id);
 return ResponseEntity.noContent().build();
 }

 // GET /api/notas/promedio/{curso} → Promedio del curso
 @GetMapping("/promedio/{curso}")
 public ResponseEntity<Double> promedio(@PathVariable String curso) {
 return ResponseEntity.ok(notaService.calcularPromedio(curso));
 }
}
```

---

## 🔬 Parte 4: Pruebas Unitarias con JUnit 5 y Mockito

> ### ¿Qué es una prueba unitaria?
>
> Una prueba unitaria verifica el comportamiento de **UNA** sola unidad de código (método o clase).
> Las dependencias externas (como el repositorio o la BD) se reemplazan por 'mocks' (objetos simulados).
> El objetivo es probar la lógica de negocio de forma rápida y aislada.

### 4.1 Conceptos Clave antes de Empezar

| Concepto | Explicación |
|---|---|
| `@ExtendWith(MockitoExtension.class)` | Activa Mockito para que pueda crear mocks automáticamente. |
| `@Mock` | Crea un objeto simulado (falso) de la clase indicada. |
| `@InjectMocks` | Crea la clase bajo prueba e inyecta los mocks como dependencias. |
| `when(...).thenReturn(...)` | Define qué debe devolver el mock cuando se llame a un método. |
| `verify(...)` | Verifica que un método del mock fue llamado (y cuántas veces). |
| `assertThat(...)` | Verifica que el resultado es el esperado. Parte de AssertJ. |
| `@Test` | Marca un método como caso de prueba. |
| `@BeforeEach` | Se ejecuta antes de cada prueba para preparar datos de prueba. |

### 4.2 NotaServiceTest.java — Pruebas Unitarias del Servicio

Esta es la clase de prueba más importante. Probamos la lógica de negocio sin conectarnos a ninguna base de datos:

```java
package pe.edu.utp.registronotas.service;
import pe.edu.utp.registronotas.entity.Nota;
import pe.edu.utp.registronotas.repository.NotaRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// 1️⃣ Activar Mockito
@ExtendWith(MockitoExtension.class)
class NotaServiceTest {

 // 2️⃣ Crear un mock del repositorio (objeto falso)
 @Mock
 private NotaRepository notaRepository;

 // 3️⃣ Inyectar el mock en el servicio que vamos a probar
 @InjectMocks
 private NotaServiceImpl notaService;

 // Nota de prueba reutilizable
 private Nota notaDePrueba;

 // 4️⃣ Preparar datos antes de cada prueba
 @BeforeEach
 void setUp() {
 notaDePrueba = new Nota("Ana García", "Programación", 16.5, "2025-I");
 notaDePrueba.setId(1L);
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 1: Guardar una nota correctamente
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe guardar una nota y retornar la nota guardada")
 void cuandoGuardaNota_debeRetornarNotaGuardada() {
 // GIVEN (preparación): el repositorio simulará guardar y retornar la nota
 when(notaRepository.save(any(Nota.class))).thenReturn(notaDePrueba);

 // WHEN (ejecución): llamamos al método del servicio
 Nota resultado = notaService.guardar(notaDePrueba);

 // THEN (verificación): comprobamos que el resultado es correcto
 assertThat(resultado).isNotNull();
 assertThat(resultado.getNombreEstudiante()).isEqualTo("Ana García");
 assertThat(resultado.getNota()).isEqualTo(16.5);

 // Verificar que save() fue llamado exactamente 1 vez
 verify(notaRepository, times(1)).save(any(Nota.class));
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 2: Listar todas las notas
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe retornar la lista de todas las notas")
 void cuandoListaTodas_debeRetornarListaDeNotas() {
 // GIVEN
 Nota nota2 = new Nota("Luis Torres", "Programación", 14.0, "2025-I");
 List<Nota> notas = Arrays.asList(notaDePrueba, nota2);
 when(notaRepository.findAll()).thenReturn(notas);

 // WHEN
 List<Nota> resultado = notaService.listarTodas();

 // THEN
 assertThat(resultado).hasSize(2);
 assertThat(resultado).extracting(Nota::getNombreEstudiante)
 .containsExactly("Ana García", "Luis Torres");
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 3: Buscar por ID existente
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe retornar la nota cuando el ID existe")
 void cuandoBuscaPorIdExistente_debeRetornarNota() {
 // GIVEN
 when(notaRepository.findById(1L)).thenReturn(Optional.of(notaDePrueba));

 // WHEN
 Optional<Nota> resultado = notaService.buscarPorId(1L);

 // THEN
 assertThat(resultado).isPresent();
 assertThat(resultado.get().getCurso()).isEqualTo("Programación");
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 4: Buscar por ID que NO existe
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe retornar Optional vacío cuando el ID no existe")
 void cuandoBuscaPorIdInexistente_debeRetornarVacio() {
 // GIVEN
 when(notaRepository.findById(99L)).thenReturn(Optional.empty());

 // WHEN
 Optional<Nota> resultado = notaService.buscarPorId(99L);

 // THEN
 assertThat(resultado).isEmpty();
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 5: Actualizar debe lanzar excepción si no existe
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe lanzar excepción al actualizar con ID inexistente")
 void cuandoActualizaIdInexistente_debeLanzarExcepcion() {
 // GIVEN
 when(notaRepository.findById(99L)).thenReturn(Optional.empty());

 // WHEN + THEN: verificamos que lanza la excepción correcta
 assertThatThrownBy(() -> notaService.actualizar(99L, notaDePrueba))
 .isInstanceOf(RuntimeException.class)
 .hasMessageContaining("Nota no encontrada");
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 6: Calcular promedio de un curso
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe calcular el promedio del curso correctamente")
 void cuandoCalculaPromedio_debeRetornarPromedioCorrecto() {
 // GIVEN
 when(notaRepository.calcularPromedioPorCurso("Programación")).thenReturn(15.25);

 // WHEN
 Double promedio = notaService.calcularPromedio("Programación");

 // THEN
 assertThat(promedio).isEqualTo(15.25);
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 7: Eliminar nota
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe llamar a deleteById al eliminar una nota")
 void cuandoElimina_debeInvocarDeleteById() {
 // GIVEN
 doNothing().when(notaRepository).deleteById(1L);

 // WHEN
 notaService.eliminar(1L);

 // THEN
 verify(notaRepository, times(1)).deleteById(1L);
 }
}
```

### 4.3 Cómo Ejecutar las Pruebas

**1. Desde IntelliJ IDEA**
Hacer clic derecho sobre la clase `NotaServiceTest` → Run 'NotaServiceTest'. O usar el ícono ▶ verde junto a `@Test`.

**2. Desde la terminal (Maven)**
Ejecutar: `mvn test` → Ejecuta todas las pruebas del proyecto.

**3. Interpretar los resultados**
Verde ✅ = prueba pasó. Rojo ❌ = prueba falló (ver el mensaje de error para corregir).

---

## 🔗 Parte 5: Pruebas de Integración con @DataJpaTest

> ### ¿Qué es una prueba de integración?
>
> Prueba la interacción REAL entre el repositorio y la base de datos (H2 en memoria).
> A diferencia de las pruebas unitarias, aquí sí se ejecutan consultas SQL reales.
> Verificamos que nuestras queries de Spring Data JPA funcionan correctamente.

### 5.1 NotaRepositoryTest.java

Usamos `@DataJpaTest` que configura automáticamente H2, JPA y los repositorios. Solo carga el contexto mínimo necesario para la capa de datos:

```java
package pe.edu.utp.registronotas.repository;
import pe.edu.utp.registronotas.entity.Nota;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

// Carga solo el contexto JPA (no carga controllers ni services)
@DataJpaTest
@ActiveProfiles("test") // usa application-test.properties
class NotaRepositoryTest {

 @Autowired
 private NotaRepository notaRepository;

 private Nota nota1, nota2;

 @BeforeEach
 void setUp() {
 // Limpiar y preparar datos antes de cada prueba
 notaRepository.deleteAll();
 nota1 = notaRepository.save(
 new Nota("Ana García", "Programación", 18.0, "2025-I"));
 nota2 = notaRepository.save(
 new Nota("Luis Torres", "Programación", 14.0, "2025-I"));
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 1: Guardar y recuperar nota por ID
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe guardar una nota y encontrarla por ID")
 void debeGuardarYEncontrarPorId() {
 Optional<Nota> encontrada = notaRepository.findById(nota1.getId());

 assertThat(encontrada).isPresent();
 assertThat(encontrada.get().getNombreEstudiante()).isEqualTo("Ana García");
 assertThat(encontrada.get().getNota()).isEqualTo(18.0);
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 2: Buscar por curso
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe retornar las notas filtradas por curso")
 void debeBuscarPorCurso() {
 List<Nota> notas = notaRepository.findByCurso("Programación");

 assertThat(notas).hasSize(2);
 assertThat(notas).allMatch(n -> n.getCurso().equals("Programación"));
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 3: Calcular promedio con @Query
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe calcular el promedio del curso correctamente")
 void debeCalcularPromedioPorCurso() {
 Double promedio = notaRepository.calcularPromedioPorCurso("Programación");

 // (18.0 + 14.0) / 2 = 16.0
 assertThat(promedio).isEqualTo(16.0);
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 4: findAll retorna todas las notas
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe listar todas las notas guardadas")
 void debeListarTodasLasNotas() {
 List<Nota> todas = notaRepository.findAll();
 assertThat(todas).hasSize(2);
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 5: Eliminar nota
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe eliminar la nota correctamente")
 void debeEliminarNota() {
 notaRepository.deleteById(nota1.getId());

 Optional<Nota> eliminada = notaRepository.findById(nota1.getId());
 assertThat(eliminada).isEmpty();
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 6: Notas aprobadas (>=11)
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("Debe retornar solo notas aprobadas")
 void debeRetornarNotasAprobadas() {
 // Agregar nota desaprobada
 notaRepository.save(new Nota("Pedro", "Programación", 8.0, "2025-I"));

 List<Nota> aprobadas = notaRepository.findByNotaGreaterThanEqual(11.0);

 assertThat(aprobadas).hasSize(2); // Ana(18) y Luis(14) aprueban
 assertThat(aprobadas).allMatch(n -> n.getNota() >= 11.0);
 }
}
```

---

## 🌐 Parte 6: Pruebas del Controlador con MockMvc

> ### ¿Qué es MockMvc?
>
> MockMvc permite simular peticiones HTTP (GET, POST, PUT, DELETE) al controlador sin necesidad de levantar un servidor real. Es como tener un 'cliente HTTP falso' dentro de las pruebas.
> Usamos `@WebMvcTest` que carga solo la capa web (Controller) y mockeamos el Service.

### 6.1 NotaControllerTest.java

```java
package pe.edu.utp.registronotas.controller;
import pe.edu.utp.registronotas.entity.Nota;
import pe.edu.utp.registronotas.service.NotaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static
org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Solo carga la capa web (Controller)
@WebMvcTest(NotaController.class)
class NotaControllerTest {

 @Autowired
 private MockMvc mockMvc; // Cliente HTTP simulado

 @MockBean
 private NotaService notaService; // Mock del servicio

 @Autowired
 private ObjectMapper objectMapper; // Para convertir objetos a JSON

 private Nota notaDePrueba;

 @BeforeEach
 void setUp() {
 notaDePrueba = new Nota("Ana García", "Programación", 16.5, "2025-I");
 notaDePrueba.setId(1L);
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 1: GET /api/notas → devuelve lista JSON
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("GET /api/notas debe retornar lista de notas")
 void getTodasLasNotas_debeRetornar200ConLista() throws Exception {
 // GIVEN: el service devuelve la lista
 when(notaService.listarTodas()).thenReturn(List.of(notaDePrueba));

 // WHEN + THEN: simular GET y verificar respuesta
 mockMvc.perform(get("/api/notas")
 .contentType(MediaType.APPLICATION_JSON))
 .andExpect(status().isOk()) // código HTTP 200
 .andExpect(jsonPath("$.size()").value(1)) // 1 elemento en el array
 .andExpect(jsonPath("$[0].nombreEstudiante").value("Ana García"))
 .andExpect(jsonPath("$[0].nota").value(16.5));
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 2: GET /api/notas/{id} → nota existente
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("GET /api/notas/1 debe retornar la nota con ID 1")
 void getNotaPorId_debeRetornar200() throws Exception {
 when(notaService.buscarPorId(1L)).thenReturn(Optional.of(notaDePrueba));

 mockMvc.perform(get("/api/notas/1"))
 .andExpect(status().isOk())
 .andExpect(jsonPath("$.nombreEstudiante").value("Ana García"))
 .andExpect(jsonPath("$.curso").value("Programación"));
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 3: GET /api/notas/{id} → ID no existe → 404
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("GET /api/notas/99 debe retornar 404 si no existe")
 void getNotaPorId_noExiste_debeRetornar404() throws Exception {
 when(notaService.buscarPorId(99L)).thenReturn(Optional.empty());

 mockMvc.perform(get("/api/notas/99"))
 .andExpect(status().isNotFound()); // código HTTP 404
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 4: POST /api/notas → crear nueva nota
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("POST /api/notas debe crear nota y retornar 200")
 void postNota_debeCrearYRetornar200() throws Exception {
 when(notaService.guardar(any(Nota.class))).thenReturn(notaDePrueba);

 // Convertir el objeto Java a JSON
 String jsonNota = objectMapper.writeValueAsString(notaDePrueba);

 mockMvc.perform(post("/api/notas")
 .contentType(MediaType.APPLICATION_JSON)
 .content(jsonNota))
 .andExpect(status().isOk())
 .andExpect(jsonPath("$.nombreEstudiante").value("Ana García"));
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 5: DELETE /api/notas/{id} → eliminar nota
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("DELETE /api/notas/1 debe retornar 204 No Content")
 void deleteNota_debeRetornar204() throws Exception {
 doNothing().when(notaService).eliminar(1L);

 mockMvc.perform(delete("/api/notas/1"))
 .andExpect(status().isNoContent()); // código HTTP 204
 }

 // ═══════════════════════════════════════════════════
 // PRUEBA 6: GET /api/notas/promedio/{curso}
 // ═══════════════════════════════════════════════════
 @Test
 @DisplayName("GET /api/notas/promedio debe retornar el promedio del curso")
 void getPromedio_debeRetornarPromedioCorrecto() throws Exception {
 when(notaService.calcularPromedio("Programación")).thenReturn(15.5);

 mockMvc.perform(get("/api/notas/promedio/Programación"))
 .andExpect(status().isOk())
 .andExpect(jsonPath("$").value(15.5));
 }
}
```

### 6.2 Tabla de Métodos MockMvc más Usados

| Método MockMvc | ¿Para qué sirve? |
|---|---|
| `get("/url")` | Simula una petición HTTP GET |
| `post("/url")` | Simula una petición HTTP POST |
| `put("/url")` | Simula una petición HTTP PUT |
| `delete("/url")` | Simula una petición HTTP DELETE |
| `status().isOk()` | Verifica que el código de respuesta es 200 |
| `status().isNotFound()` | Verifica que el código de respuesta es 404 |
| `status().isNoContent()` | Verifica que el código de respuesta es 204 |
| `jsonPath("$.campo")` | Extrae y verifica un campo del JSON de respuesta |
| `.content(json)` | Envía un cuerpo JSON en la petición |
| `.contentType(APPLICATION_JSON)` | Indica que enviamos/esperamos JSON |

---

## 📊 Parte 7: Cobertura de Código con JaCoCo

> ### ¿Qué es la cobertura de código?
>
> La cobertura mide qué porcentaje del código fuente es ejecutado por las pruebas.
> JaCoCo genera un reporte HTML detallado mostrando qué líneas fueron probadas (verde) y cuáles no fueron cubiertas por ninguna prueba (rojo).
> Una buena cobertura mínima para proyectos académicos es del 70-80%.

### 7.1 Generar el Reporte de Cobertura

**1. Ejecutar las pruebas con Maven**
En la terminal del proyecto: `mvn clean test`

**2. Ubicar el reporte generado**
JaCoCo genera el reporte en: `target/site/jacoco/index.html`

**3. Abrir el reporte**
Abrir el archivo `index.html` en cualquier navegador web.

**4. Interpretar los resultados**
Verde = código cubierto. Rojo = código NO cubierto. Amarillo = parcialmente cubierto.

### 7.2 Cómo Leer el Reporte JaCoCo

| Métrica | Significado |
|---|---|
| Instructions Coverage | % de instrucciones Java ejecutadas por las pruebas. |
| Branch Coverage | % de ramificaciones (if/else, switch) probadas. |
| Line Coverage | % de líneas de código ejecutadas. |
| Method Coverage | % de métodos invocados durante las pruebas. |
| Class Coverage | % de clases cargadas durante las pruebas. |

### 7.3 Configurar Umbral Mínimo de Cobertura (Opcional)

Podemos configurar Maven para que FALLE la compilación si la cobertura es menor al 70%:

```xml
<!-- Agregar dentro del plugin jacoco-maven-plugin en pom.xml -->
<execution>
 <id>check-cobertura</id>
 <phase>verify</phase>
 <goals><goal>check</goal></goals>
 <configuration>
 <rules>
 <rule>
 <element>CLASS</element>
 <limits>
 <limit>
 <counter>LINE</counter>
 <value>COVEREDRATIO</value>
 <minimum>0.70</minimum> <!-- mínimo 70% cobertura -->
 </limit>
 </limits>
 </rule>
 </rules>
 </configuration>
</execution>
```

---

## ✏️ Parte 8: Actividades Propuestas

Aplicar los conocimientos del taller completando las siguientes actividades. Ordenadas de menor a mayor dificultad:

### 🟢 Nivel Básico — Pruebas Unitarias

*Completar antes de pasar al siguiente nivel*

1. Ejecutar todas las pruebas del proyecto tal como están. Verificar que todas pasan (✅ verde).
2. Agregar un caso de prueba en `NotaServiceTest` para verificar que `buscarPorCurso()` retorna la lista correcta cuando el repositorio devuelve 3 notas del mismo curso.
3. Agregar un caso de prueba que verifique que `calcularPromedio()` retorna 0.0 cuando el repositorio retorna null (curso sin notas).
4. En `NotaServiceTest`, verificar que al llamar a `eliminar(id)`, el repositorio invoca `deleteById` exactamente una vez con ese mismo ID.

### 🟡 Nivel Intermedio — Pruebas de Integración y Controlador

*Requiere haber completado el nivel básico*

5. En `NotaRepositoryTest`: agregar una prueba que verifique que `findByNombreEstudiante('Ana García')` retorna solo las notas de ese estudiante.
6. En `NotaRepositoryTest`: agregar una prueba que verifique que el promedio es 0.0 cuando se busca un curso que no existe.
7. En `NotaControllerTest`: agregar una prueba para `PUT /api/notas/{id}` que verifique que al actualizar, se retorna la nota actualizada con los nuevos valores.
8. En `NotaControllerTest`: agregar una prueba para `GET /api/notas/promedio/{curso}` donde el servicio retorna 0.0 y verificar que la respuesta es 0.0.

### 🔴 Nivel Avanzado — Integración y Cobertura

*Desafío para estudiantes que quieran ir más lejos*

9. Generar el reporte JaCoCo con `mvn clean test`. Identificar qué método o clase tiene menor cobertura y escribir una prueba que cubra esas líneas rojas.
10. Agregar una nueva entidad `Estudiante` con campos: id, nombre, email, codigo. Crear su repositorio y service. Escribir al menos 4 pruebas unitarias y 3 pruebas de integración para esta nueva entidad.
11. Crear una clase `@ControllerAdvice` que maneje excepciones y retorne mensajes JSON de error. Escribir pruebas MockMvc que verifiquen que al buscar un ID inexistente, el controlador retorna un JSON con el mensaje de error correcto.

---

## 🚨 Parte 9: Errores Comunes y Soluciones

| Error frecuente | Causa y solución |
|---|---|
| `NullPointerException` en el mock | Olvidaste agregar `@ExtendWith(MockitoExtension.class)` en la clase de prueba. Agrégala sobre la clase. |
| `UnnecessaryStubbingException` | Definiste un `when(...)` que nunca se usa en esa prueba. Elimínalo o muévelo al `setUp()` si es común. |
| `No qualifying bean of type 'NotaRepository'` | En `@WebMvcTest` solo se carga la capa web. El repositorio debe ser `@MockBean`, no `@Autowired`. |
| H2 table not found | El `application-test.properties` no está en `src/test/resources/` o falta `@ActiveProfiles('test')`. |
| `AssertionError: expected 2 but was 1` | Los datos de `setUp()` se guardaron en prueba anterior. Asegúrate de llamar `deleteAll()` en `@BeforeEach`. |
| JSONPath error: campo no encontrado | El nombre del campo en `jsonPath()` no coincide con el nombre del getter de la entidad (Java usa camelCase). |
| `verify()` falla aunque el método fue llamado | Verifica que el argumento en `verify()` coincide exactamente con el usado en el método. Usa `any()` si es variable. |

### Buenas Prácticas de Testing

- Cada método de prueba debe probar **UNA** sola cosa.
- Usar `@DisplayName` con una descripción clara en español: 'Debe retornar 404 cuando el ID no existe'.
- Seguir el patrón **GIVEN-WHEN-THEN** (preparación, ejecución, verificación) con comentarios.
- Mantener independencia: cada prueba debe poder ejecutarse sola sin depender de otra.
- No probar getters/setters triviales, enfocar las pruebas en lógica de negocio real.
- Nombrar las pruebas con el patrón: `cuandoCondicion_debeResultado()`.

---

## ✅ Parte 10: Resumen y Checklist Final

### Flujo Completo del Taller

**1. Configurar el proyecto**
Spring Initializr + dependencias en `pom.xml` + properties para test con H2.

**2. Crear el código de producción**
Entidad Nota → NotaRepository → NotaService → NotaController.

**3. Escribir pruebas unitarias**
`NotaServiceTest` con JUnit 5 + Mockito. Sin BD real. Rápido.

**4. Escribir pruebas de integración**
`NotaRepositoryTest` con `@DataJpaTest` + H2 en memoria. Prueba queries reales.

**5. Escribir pruebas del controlador**
`NotaControllerTest` con `@WebMvcTest` + MockMvc. Prueba endpoints HTTP.

**6. Generar reporte de cobertura**
`mvn clean test` → abrir `target/site/jacoco/index.html`.

### Checklist de Entregable

Verificar antes de entregar el proyecto:

- ☐ `pom.xml` tiene las dependencias de testing y el plugin JaCoCo.
- ☐ `application-test.properties` existe en `src/test/resources/`.
- ☐ `NotaServiceTest` tiene al menos 7 casos de prueba.
- ☐ `NotaRepositoryTest` tiene al menos 5 casos de prueba con `@DataJpaTest`.
- ☐ `NotaControllerTest` tiene al menos 6 casos de prueba con MockMvc.
- ☐ Todas las pruebas pasan en verde al ejecutar `mvn test`.
- ☐ El reporte JaCoCo muestra cobertura >= 70%.
- ☐ Las pruebas siguen el patrón GIVEN-WHEN-THEN con comentarios.

### Recursos Adicionales

- Documentación oficial JUnit 5: https://junit.org/junit5/docs/current/user-guide/
- Documentación Mockito: https://javadoc.io/doc/org.mockito/mockito-core/latest/
- Spring Boot Testing: https://docs.spring.io/springboot/docs/current/reference/html/features.html#features.testing
- JaCoCo Plugin Maven: https://www.jacoco.org/jacoco/trunk/doc/maven.html

---

*Universidad Tecnológica del Perú Lima Sur — Docente: Magister Ingeniero Marco Antonio Flores Torres*
