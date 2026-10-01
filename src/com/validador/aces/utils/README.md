# Paquete: utils

## Descripción

Contiene utilidades y clases helper para el proyecto.

## Clases

### Logging
- **Logger**: Wrapper para logging centralizado
- **LoggerFactory**: Factory para crear loggers
- Niveles: DEBUG, INFO, WARN, ERROR

### String Utilities
- **StringUtils**: Utilidades de manipulación de strings
- **Métodos**: isEmpty(), trim(), split(), join(), format()
- Utiliza: commons-lang3

### Date Utilities
- **DateUtils**: Utilidades de manejo de fechas
- **Métodos**: parse(), format(), isBefore(), isAfter()
- Formato: ISO 8601 por defecto

### File Utilities
- **FileUtils**: Utilidades para manejo de archivos
- **Métodos**: readFile(), writeFile(), listFiles()
- Utiliza: commons-io

### Exception Utilities
- **ExceptionUtils**: Manejo de excepciones
- **Métodos**: getStackTrace(), getRootCause(), toString()

### Collection Utilities
- **CollectionUtils**: Utilidades para colecciones
- **Métodos**: isEmpty(), size(), union(), intersection()
- Utiliza: commons-lang3

### Configuration
- **ConfigurationManager**: Gestor de configuración
- **Métodos**: getProperty(), setProperty(), loadProperties()
- Archivo: application.properties

### Constants
- **AcesConstants**: Constantes del proyecto
- **Valores**: Tipos, estados, mensajes de error

## Archivos Esperados

```
utils/
├── Logger.java
├── LoggerFactory.java
├── StringUtils.java
├── DateUtils.java
├── FileUtils.java
├── ExceptionUtils.java
├── CollectionUtils.java
├── ConfigurationManager.java
├── AcesConstants.java
└── README.md
```

## Librerías Utilizadas

- `commons-lang3-3.18.0.jar` - String y collection utilities
- `commons-io-2.20.0.jar` - File utilities

## Convenciones

### Métodos Estáticos
- Todas las utilidades deben ser estáticas
- No instanciar clases de utils
- Considerar clase final con constructor privado

### Null Safety
- Verificar null en parámetros
- Usar Optional cuando sea apropiado
- Lanzar excepciones descriptivas

### Performance
- Caché resultados cuando sea posible
- Lazy loading
- Evitar copies innecesarias

## Ejemplos de Uso

```java
// Logging
Logger logger = LoggerFactory.getLogger(MyClass.class);
logger.info("Mensaje informativo");

// Strings
String result = StringUtils.join(list, ",");

// Dates
LocalDate date = DateUtils.parse("2024-01-15");

// Files
String content = FileUtils.readFile("data.txt");

// Configuration
String value = ConfigurationManager.getProperty("app.name");

// Constants
String type = AcesConstants.ATTRIBUTE_TYPE_STRING;
```

## Notas

- Reutilizar máximo posible
- No reimplementar lo que proporciona commons
- Documentar con JavaDoc
- Incluir tests unitarios
- Mantener simples y enfocadas
