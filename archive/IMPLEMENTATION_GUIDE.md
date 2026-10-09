# Guía de Implementación - Validador de Atributos ACES

## 1. ESTRUCTURA DE DIRECTORIOS RECOMENDADA

```
src/
├── com/
│   └── validador/
│       └── aces/
│           ├── models/           # TASK-001 a TASK-007
│           │   ├── Attribute.java
│           │   ├── Catalog.java
│           │   ├── Application.java
│           │   ├── ProductLine.java
│           │   ├── ValidationError.java
│           │   ├── ComparisonResult.java
│           │   └── AuditReport.java
│           ├── parsers/          # TASK-008 a TASK-012
│           │   ├── CatalogParser.java
│           │   ├── ExcelCatalogParser.java
│           │   ├── ApplicationParser.java
│           │   ├── ExcelApplicationParser.java
│           │   └── ValidationSchema.java
│           ├── validation/       # TASK-013 a TASK-018
│           │   ├── Validator.java
│           │   ├── AttributeValidator.java
│           │   ├── RangeValidator.java
│           │   ├── EnumValidator.java
│           │   ├── CompositeValidator.java
│           │   └── DateFormatValidator.java
│           ├── comparison/       # TASK-019 a TASK-021
│           │   ├── Comparator.java
│           │   ├── ComplianceCalculator.java
│           │   └── ComparisonCache.java
│           ├── reporting/        # TASK-022 a TASK-026
│           │   ├── ReportGenerator.java
│           │   ├── ExcelReportGenerator.java
│           │   ├── ReportPrinter.java
│           │   ├── ExcelExporter.java
│           │   └── ReportFilter.java
│           ├── gui/              # TASK-027 a TASK-034
│           │   ├── MainWindow.java
│           │   ├── CatalogLoadPanel.java
│           │   ├── ApplicationLoadPanel.java
│           │   ├── ValidationPanel.java
│           │   ├── StatisticsPanel.java
│           │   ├── AuditPanel.java
│           │   └── dialogs/
│           │       ├── FileDialog.java
│           │       └── ConfigDialog.java
│           ├── tests/            # TASK-035 a TASK-047
│           │   ├── parsers/
│           │   ├── validators/
│           │   ├── comparison/
│           │   └── reporting/
│           └── Launcher.java     # TASK-053
└── resources/
    ├── application.properties
    └── icons/
```

## 2. DEPENDENCIAS EXTERNAS (ya en lib/)

- **aalto-xml-1.4.0.jar** - Parsing XML
- **commons-compress-1.28.0.jar** - Compresión
- **commons-io-2.20.0.jar** - Utilidades I/O
- **commons-lang3-3.18.0.jar** - Utilidades string/lang
- **fastexcel-0.20.2.jar** - Escritura Excel
- **fastexcel-reader-0.20.2.jar** - Lectura Excel
- **opczip-1.2.0.jar** - Compresión ZIP
- **stax2-api-4.2.2.jar** - Parsing XML

## 3. PATRONES DE DISEÑO A USAR

### Strategy Pattern
- `Validator` (múltiples estrategias de validación)
- `ReportGenerator` (múltiples formatos de reporte)
- `CatalogParser` (múltiples fuentes de catálogo)

### Chain of Responsibility
- `CompositeValidator` encadena múltiples validadores

### Builder Pattern
- Para construcción de objetos complejos (ComparisonResult)

### Singleton Pattern
- `ComparisonCache` (opcional)

### Observer Pattern
- GUI panels observan cambios de estado

## 4. ENUMERACIONES CLAVE

```java
// Tipo de dato de atributo
enum AttributeType {
    STRING, INTEGER, DECIMAL, BOOLEAN, DATE
}

// Severidad de error
enum ErrorSeverity {
    ERROR, WARNING, INFO
}

// Acciones de auditoría
enum AuditAction {
    VALIDATION, COMPARISON, EXPORT, IMPORT
}

// Nivel de strictness
enum ValidationType {
    LENIENT, NORMAL, STRICT
}
```

## 5. INTERFACES CLAVE

```java
// Parser genérico
interface Parser<T> {
    T parse(File file) throws ParseException;
}

// Validador genérico
interface IValidator {
    List<ValidationError> validate(Application app, Catalog catalog);
}

// Generador de reporte
interface IReportGenerator {
    void generate(ComparisonResult result, File outputFile) throws IOException;
}
```

## 6. CONFIGURACIÓN RECOMENDADA

**application.properties**:
```properties
# Validación
validation.strictness=NORMAL
validation.case-sensitive=false
validation.max-errors=1000

# Cache
cache.enabled=true
cache.max-size=100

# Reporte
report.include-valid-attributes=false
report.date-format=yyyy-MM-dd HH:mm:ss

# GUI
gui.remember-last-path=true
gui.show-statistics=true
gui.font-size=12
```

## 7. MANEJO DE EXCEPCIONES

**Crear excepciones personalizadas**:
```java
class ParseException extends Exception { }
class ValidationException extends Exception { }
class ReportException extends Exception { }
```

## 8. LOGGING

Usar `java.util.logging` o `Log4j`:
```java
private static final Logger LOGGER = Logger.getLogger(ClassName.class.getName());
```

## 9. TESTING - HERRAMIENTAS RECOMENDADAS

- **JUnit 4** o **JUnit 5** para unit tests
- **TestNG** para más control
- **Mockito** para mocks
- **QuickCheck for Java** o **jqwik** para property-based testing

**Ejemplo Property Test**:
```java
@RunWith(QuickTheories.class)
public class RoundTripTest {
    @Test
    public void catalogRoundTrip(@From(CatalogGenerator.class) Catalog catalog) {
        // Serializar a Excel
        // Parsear de nuevo
        // Verificar igualdad
    }
}
```

## 10. COMPILACIÓN

### Con Ant (build.xml)
```bash
ant clean compile    # Compilar
ant test             # Ejecutar tests
ant jar              # Empaquetar JAR
ant run              # Ejecutar aplicación
```

### Con Java directamente
```bash
# Compilar
javac -cp lib/*:. -d bin src/com/validador/aces/**/*.java

# Ejecutar
java -cp bin:lib/* com.validador.aces.Launcher

# Empaquetar
jar cfm validador-aces.jar manifest.txt -C bin .
```

## 11. EJECUCIÓN

```bash
# Interfaz gráfica
java -jar validador-aces.jar

# Línea de comandos (opcional)
java -jar validador-aces.jar --catalog catalogo.xlsx --app aplicacion.xlsx --output reporte.xlsx
```

## 12. CHECKLIST DE IMPLEMENTACIÓN

### Fase 1: Modelos y Parsers (TASK-001 a TASK-012)
- [ ] Todos los modelos implementados
- [ ] Parsers funcionan con archivos de ejemplo
- [ ] Tests básicos pasan

### Fase 2: Validación (TASK-013 a TASK-018)
- [ ] Todos los validadores implementados
- [ ] CompositeValidator ejecuta cadena correctamente
- [ ] Errores se generan apropiadamente

### Fase 3: Comparación y Reportes (TASK-019 a TASK-026)
- [ ] Comparador ejecuta flujo completo
- [ ] Compliance calculator da resultados correctos
- [ ] Reportes Excel se generan correctamente

### Fase 4: GUI (TASK-027 a TASK-034)
- [ ] MainWindow inicia sin errores
- [ ] Carga de catálogo funciona
- [ ] Carga de aplicación funciona
- [ ] Validación se ejecuta desde GUI
- [ ] Resultados se muestran correctamente

### Fase 5: Testing Completo (TASK-035 a TASK-047)
- [ ] 90%+ cobertura de código
- [ ] Tests de integración pasan
- [ ] Property tests pasan 100+ iteraciones

### Fase 6: Build (TASK-051 a TASK-054)
- [ ] JAR se empaqueta correctamente
- [ ] JAR es ejecutable
- [ ] Todas las dependencias se incluyen

## 13. OPTIMIZACIONES FUTURAS

1. **Caché de resultados** (TASK-021)
2. **Parsing paralelo** para archivos grandes
3. **Exportación a PDF** además de Excel
4. **API REST** para integración con otros sistemas
5. **Base de datos** para historial de auditoría
6. **Validación asíncrona** en GUI para no bloquear UI

## 14. VERSIONADO

```
Versión 1.0 MVP:
- Modelos de datos
- Parsers Excel
- Validadores básicos
- GUI simple
- Reporte Excel

Versión 1.1:
- Property-based testing
- Caché
- Más validadores
- Estadísticas avanzadas

Versión 2.0:
- API REST
- Base de datos
- Exportación múltiple
- Validación distribuida
```

---

**Nota**: Seguir el orden de tasks respetando dependencias es crítico para evitar errores de compilación.
