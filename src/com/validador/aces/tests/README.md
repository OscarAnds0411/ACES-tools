# Paquete: tests

## Descripción

Contiene las pruebas unitarias e integración del proyecto.

## Clases

### TASK-035: ModelTests
- **Descripción**: Tests unitarios para modelos
- **Pruebas**: Creación, getters, setters, validaciones básicas
- **Framework**: JUnit 4 o 5
- **Estado**: [TODO]

### TASK-036: ParserTests
- **Descripción**: Tests para parsers de Excel
- **Pruebas**: Lectura correcta, manejo de errores, edge cases
- **Archivos de prueba**: En resources/test-data/
- **Estado**: [TODO]

### TASK-037: ValidationTests
- **Descripción**: Tests para validadores
- **Pruebas**: Validación correcta, manejo de tipos, reglas
- **Cobertura**: Tipos, reglas, catálogos, aplicaciones
- **Estado**: [TODO]

### TASK-038: ComparisonTests
- **Descripción**: Tests para comparadores
- **Pruebas**: Detección de diferencias, análisis de cambios
- **Escenarios**: Adición, eliminación, modificación
- **Estado**: [TODO]

### TASK-039: IntegrationTests
- **Descripción**: Tests de integración end-to-end
- **Pruebas**: Flujo completo: cargar, validar, reportar
- **Estado**: [TODO]

### TASK-040: PerformanceTests
- **Descripción**: Tests de performance
- **Métricas**: Tiempo de parseo, validación, comparación
- **Benchmarks**: Cargas típicas y máximas
- **Estado**: [TODO]

### TASK-041: TestDataBuilder
- **Descripción**: Builder para crear datos de prueba
- **Métodos**: createCatalog(), createApplication(), createValidationResult()
- **Estado**: [TODO]

### TASK-042: MockObjects
- **Descripción**: Mocks para tests
- **Objetos**: MockCatalog, MockParser, MockValidator
- **Utiliza**: Mockito (si se incluye)
- **Estado**: [TODO]

### TASK-043: TestFixtures
- **Descripción**: Datos y configuraciones para tests
- **Archivos**: Excel de prueba, configuraciones
- **Ubicación**: src/test/resources/
- **Estado**: [TODO]

### TASK-044: CoverageReport
- **Descripción**: Generador de reportes de cobertura
- **Herramienta**: JaCoCo (recomendado)
- **Objetivo**: >80% cobertura
- **Estado**: [TODO]

### TASK-045: RegressionTests
- **Descripción**: Tests de regresión
- **Casos**: Bugs arreglados, cambios backward compatible
- **Estado**: [TODO]

### TASK-046: AcceptanceTests
- **Descripción**: Tests de aceptación (escenarios de negocio)
- **Casos**: Flujos de usuario, requisitos funcionales
- **Estado**: [TODO]

### TASK-047: DocTests
- **Descripción**: Tests documentados
- **Formato**: Ejemplos en código y documentación
- **Estado**: [TODO]

## Dependencias

- Depende de: todos los otros paquetes
- Framework de testing: JUnit 4 o 5
- Mock framework: Mockito (opcional pero recomendado)
- Assertion library: AssertJ (opcional)

## Archivos Esperados

```
tests/
├── ModelTests.java
├── ParserTests.java
├── ValidationTests.java
├── ComparisonTests.java
├── IntegrationTests.java
├── PerformanceTests.java
├── TestDataBuilder.java
├── MockObjects.java
├── TestFixtures.java
├── CoverageReport.java
├── RegressionTests.java
├── AcceptanceTests.java
├── DocTests.java
└── README.md

test/resources/
├── test-data/
│   ├── catalog-valid.xlsx
│   ├── catalog-invalid.xlsx
│   ├── application-valid.xlsx
│   └── application-invalid.xlsx
└── fixtures/
    ├── config.properties
    └── test-setup.json
```

## Frameworks Recomendados

### Testing
- **JUnit 5** - Framework base (incluido o agregar dependencia)
- **Mockito** - Creación de mocks (agregar dependencia)
- **AssertJ** - Assertions más expresivas (agregar dependencia)

### Cobertura
- **JaCoCo** - Reporte de cobertura de código

## Patrones de Testing

### Arrange-Act-Assert (AAA)
```
Arrange: Preparar datos
Act: Ejecutar acción
Assert: Verificar resultado
```

### Given-When-Then (GWT)
```
Given: Contexto inicial
When: Acción
Then: Resultado esperado
```

## Cobertura Esperada

- Modelos: 100% (solo getters/setters)
- Parsers: >95% (incluyendo errores)
- Validación: >90% (casos principales)
- Comparación: >85% (lógica compleja)
- Reporting: >80% (formato)
- GUI: >60% (componentes difíciles de testear)

## Tipos de Tests

### Unitarios
- Una clase, sin dependencias externas
- Rápidos, muchos

### Integración
- Varias clases juntas
- Con dependencias
- Menos cantidad que unitarios

### End-to-End
- Flujo completo
- Datos reales (o próximos)
- Pocos, con setup completo

### Performance
- Casos de carga típicos
- Benchmarks
- Métricas de tiempo

## Notas

- Tests claros y mantenibles
- Nombres descriptivos (testShouldValidateCorrectType)
- Sin dependencias entre tests
- Setup y teardown necesarios
- Documentar casos complejos
- Ejecutar regularmente (CI/CD)
