# Paquete: validation

## Descripción

Contiene la lógica de validación de atributos ACES contra catálogos.

## Clases

### TASK-013: AttributeValidator
- **Descripción**: Validador base para atributos
- **Métodos**: validate(), validateType(), validateRequired(), validateFormat()
- **Retorna**: ValidationResult con errores y advertencias
- **Estado**: [TODO]

### TASK-014: TypeValidator
- **Descripción**: Validador especializado en tipos de datos
- **Tipos soportados**: STRING, INTEGER, DECIMAL, BOOLEAN, DATE
- **Métodos**: validateString(), validateInteger(), validateDecimal(), validateBoolean(), validateDate()
- **Estado**: [TODO]

### TASK-015: FormatValidator
- **Descripción**: Validador de formatos y patrones
- **Métodos**: validatePattern(), validateLength(), validateRange()
- **Utiliza**: Expresiones regulares
- **Estado**: [TODO]

### TASK-016: RuleValidator
- **Descripción**: Validador de reglas de negocio complejas
- **Métodos**: evaluateRule(), evaluateExpression(), evaluateCondition()
- **Estado**: [TODO]

### TASK-017: CatalogValidator
- **Descripción**: Validador de catálogos completos
- **Métodos**: validateCatalog(), validateProductLines(), validateAttributes()
- **Estado**: [TODO]

### TASK-018: ApplicationValidator
- **Descripción**: Validador de aplicaciones contra catálogos
- **Métodos**: validateApplication(), validateAttributes(), generateReport()
- **Estado**: [TODO]

## Dependencias

- Depende de: models (Attribute, ValidationResult, etc.)
- Utiliza: commons-lang3 para validación de strings

## Archivos Esperados

```
validation/
├── AttributeValidator.java
├── TypeValidator.java
├── FormatValidator.java
├── RuleValidator.java
├── CatalogValidator.java
├── ApplicationValidator.java
└── README.md
```

## Tipos de Validación

### Validación de Tipo
- STRING: longitud, caracteres permitidos
- INTEGER: rango, formato
- DECIMAL: precisión, rango
- BOOLEAN: valores verdadero/falso
- DATE: formato ISO 8601, rango

### Validación de Reglas
- Campos requeridos
- Valores permitidos (whitelist)
- Patrones (regex)
- Rangos numéricos
- Formato de fechas
- Dependencias entre campos

### Validación de Catálogos
- Estructura correcta
- Líneas de producto válidas
- Atributos únicos
- Referencias válidas

## Notas

- Acumular todos los errores (no fallar en el primero)
- Diferenciar entre errores y advertencias
- Mensajes de error claros y útiles
- Incluir información sobre qué validó correctamente
- Performance: validar en paralelo cuando sea posible
