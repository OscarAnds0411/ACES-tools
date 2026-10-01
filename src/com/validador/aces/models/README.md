# Componente 1: Modelos de Datos (TASK-001 a TASK-007)

## Descripción General

Este paquete contiene los modelos de datos fundamentales del Validador de Atributos ACES. Estos modelos representan la estructura central del sistema y son utilizados por todos los demás componentes.

## Enumeraciones (Enums)

### AttributeType
Define los tipos de datos soportados para atributos:
- `STRING` - Texto libre
- `INTEGER` - Números enteros
- `DECIMAL` - Números decimales
- `BOOLEAN` - Valores booleanos
- `DATE` - Fechas

**Método útil**: `fromString(String value)` - Convierte un string a AttributeType

### ErrorSeverity
Define los niveles de severidad para errores de validación:
- `ERROR` - Error crítico
- `WARNING` - Advertencia
- `INFO` - Información

**Método útil**: `fromString(String value)` - Convierte un string a ErrorSeverity

### AuditAction
Define tipos de acciones de auditoría:
- `VALIDATION` - Validación de datos
- `COMPARISON` - Comparación de atributos
- `EXPORT` - Exportación de datos
- `IMPORT` - Importación de datos

**Método útil**: `fromString(String value)` - Convierte un string a AuditAction

## Clases de Datos

### Catalog (TASK-001)
Representa el catálogo maestro de atributos ACES.

**Propiedades**:
- `id` (String) - Identificador único, obligatorio
- `name` (String) - Nombre del catálogo, obligatorio
- `version` (String) - Versión del catálogo
- `description` (String) - Descripción
- `createdDate` (LocalDateTime) - Fecha de creación (auto-generada si no se proporciona)
- `productLines` (List<ProductLine>) - Colección de líneas de producto

**Métodos principales**:
- `addProductLine(ProductLine)` - Agrega una línea de producto
- `getProductLines()` - Obtiene lista no modificable
- `findProductLineById(String)` - Busca por ID
- `getProductLineCount()` - Cuenta de líneas

### ProductLine (TASK-002)
Representa una línea de producto dentro del catálogo.

**Propiedades**:
- `id` (String) - Identificador único, obligatorio
- `name` (String) - Nombre, obligatorio
- `description` (String) - Descripción
- `attributes` (List<Attribute>) - Atributos de la línea

**Métodos principales**:
- `addAttribute(Attribute)` - Agrega un atributo
- `getAttributes()` - Obtiene lista no modificable
- `findAttributeByName(String)` - Busca por nombre (case-insensitive)
- `findAttributeById(String)` - Busca por ID
- `getAttributeCount()` - Cuenta de atributos

### Attribute (TASK-003)
Representa un atributo individual con reglas de validación.

**Propiedades**:
- `id` (String) - Identificador único, obligatorio
- `name` (String) - Nombre, obligatorio
- `type` (AttributeType) - Tipo de dato, obligatorio
- `required` (boolean) - Si es obligatorio
- `validation` (String) - Expresión de validación (regex u otra)
- `allowedValues` (List<String>) - Valores permitidos
- `description` (String) - Descripción

**Métodos principales**:
- `validateValue(Object)` - Valida un valor según especificaciones
- `addAllowedValue(String)` - Agrega valor permitido
- `getAllowedValues()` - Obtiene lista de valores permitidos

**Lógica de Validación**:
- STRING: Valida contra valores permitidos y expresiones regulares
- INTEGER: Valida tipo y rango (si aplica)
- DECIMAL: Valida tipo y precisión
- BOOLEAN: Acepta true/false, yes/no, 0/1
- DATE: Soporta múltiples formatos (yyyy-MM-dd, dd/MM/yyyy, etc.)

### Application (TASK-004)
Representa una aplicación cuyos atributos serán validados.

**Propiedades**:
- `name` (String) - Nombre, obligatorio
- `version` (String) - Versión
- `data` (Map<String, Object>) - Mapa de atributos

**Métodos principales**:
- `getAttributeValue(String)` - Obtiene valor de atributo
- `setAttributeValue(String, Object)` - Establece valor de atributo
- `getData()` - Obtiene copia del mapa de datos
- `hasAttribute(String)` - Verifica existencia
- `getAttributeCount()` - Cuenta de atributos
- `clearData()` - Limpia todos los datos

### ValidationError (TASK-005)
Representa un error o advertencia de validación.

**Propiedades**:
- `severity` (ErrorSeverity) - Nivel de severidad, obligatorio
- `code` (String) - Código de error, obligatorio
- `message` (String) - Mensaje descriptivo, obligatorio
- `attributeName` (String) - Nombre del atributo
- `productLine` (String) - Línea de producto
- `timestamp` (LocalDateTime) - Timestamp (auto-generado)
- `invalidValue` (Object) - Valor que causó el error

**Métodos principales**:
- `isError()` / `isWarning()` / `isInfo()` - Predicados de severidad
- Constructor especial con `invalidValue` para documentar valores problemáticos

### ComparisonResult (TASK-006)
Agrupa los resultados de una comparación entre aplicación y catálogo.

**Propiedades**:
- `applicationName` (String) - Nombre de aplicación, obligatorio
- `catalogName` (String) - Nombre de catálogo, obligatorio
- `totalAttributes` (int) - Total de atributos
- `validAttributes` (int) - Atributos válidos
- `invalidAttributes` (int) - Atributos inválidos
- `errors` (List<ValidationError>) - Errores encontrados
- `warnings` (List<ValidationError>) - Advertencias
- `comparisonDate` (LocalDateTime) - Fecha de comparación

**Métodos principales**:
- `addError(ValidationError)` - Agrega error
- `addWarning(ValidationError)` - Agrega advertencia
- `addErrors(List<ValidationError>)` - Agrega múltiples
- `getErrors()` / `getWarnings()` - Obtiene listas no modificables
- `getCompliancePercentage()` - Porcentaje de cumplimiento (0-100)
- `getErrorCount()` / `getWarningCount()` - Contadores
- `getTotalIssueCount()` - Total de problemas
- `isSuccessful()` - true si no hay errores
- `hasIssues()` - true si hay al menos un problema

### AuditReport (TASK-007)
Encapsula información de auditoría sobre operaciones.

**Propiedades**:
- `timestamp` (LocalDateTime) - Timestamp (auto-generado)
- `user` (String) - Usuario que realizó acción, obligatorio
- `action` (AuditAction) - Tipo de acción, obligatorio
- `details` (String) - Detalles adicionales
- `result` (String) - Resultado de la acción
- `durationMillis` (long) - Duración en milisegundos

**Métodos principales**:
- `toJson()` - Serializa a JSON
- `toLogString()` - Formatea para logging

## Características Comunes

### Validación de Constructores
Todos los modelos validan parámetros críticos en constructores:
- Rechazan valores null o vacíos para campos obligatorios
- Lanzan `IllegalArgumentException` con mensajes claros

### Métodos equals() y hashCode()
Implementados en todos los modelos para:
- Comparación correcta de objetos
- Uso en colecciones (Set, Map)
- Basan en campos clave/identificadores

### Método toString()
Proporciona representación legible para debugging:
- Incluye información relevante sin ser verboso
- Útil para logging

### Serializable
Todos implementan `Serializable` para:
- Persistencia en archivo
- Transmisión por red
- Almacenamiento en caché

## Uso Ejemplo

```java
// Crear catálogo
Catalog catalog = new Catalog("CAT-001", "Catálogo Maestro");

// Crear línea de producto
ProductLine productLine = new ProductLine("PL-001", "Electrónica");

// Crear atributo con validación
Attribute attr = new Attribute("ATTR-001", "Voltaje", AttributeType.INTEGER);
attr.setRequired(true);
attr.setValidation(".*");  // Cualquier integer es válido

// Armar estructura
productLine.addAttribute(attr);
catalog.addProductLine(productLine);

// Crear aplicación para validar
Application app = new Application("App-001", "1.0");
app.setAttributeValue("Voltaje", 220);

// Validar
if (attr.validateValue(app.getAttributeValue("Voltaje"))) {
    System.out.println("Válido");
} else {
    System.out.println("Inválido");
}

// Registrar resultado
ComparisonResult result = new ComparisonResult("App-001", "CAT-001");
result.setTotalAttributes(1);
result.setValidAttributes(1);

System.out.println("Cumplimiento: " + result.getCompliancePercentage() + "%");

// Auditoría
AuditReport audit = new AuditReport("admin@example.com", AuditAction.VALIDATION);
audit.setDetails("Validación manual");
audit.setResult("EXITOSO");
System.out.println(audit.toLogString());
```

## Notas de Implementación

1. **Fechas**: Utilizan `java.time.LocalDateTime` (Java 11+)
2. **Colecciones**: Métodos retornan listas/mapas no modificables para seguridad
3. **Validación**: Se valida en setters y constructores
4. **Excepciones**: Se lanzan específicamente para detectar errores tempranamente
5. **JSON**: `AuditReport` proporciona serialización manual a JSON
6. **Formato de Fechas**: Soporta múltiples formatos automáticamente

## Próximos Pasos

- COMPONENTE 2: Parsers (TASK-008 a TASK-012)
- COMPONENTE 3: Validación (TASK-013 a TASK-018)
