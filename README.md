# Validador de Atributos ACES

Herramienta Java para validar atributos de productos contra catálogos maestros ACES, con interfaz gráfica y generación de reportes en Excel.

## Descripción General

El Validador de Atributos ACES es una aplicación completa que permite:

1. **Cargar catálogos maestros** de atributos desde archivos Excel
2. **Cargar datos de aplicaciones** a ser validadas
3. **Ejecutar validaciones** contra múltiples reglas y criterios
4. **Generar reportes detallados** en formato Excel con estadísticas
5. **Auditar todas las operaciones** de validación y comparación
6. **Analizar compliance** y conformidad de datos

## Estructura de Directorios

```
validadorDeAtributosAces/
├── src/                          # Código fuente Java
│   ├── com/validador/aces/
│   │   ├── models/              # Modelos de datos
│   │   ├── parsers/             # Parsers Excel/XML
│   │   ├── validation/          # Validadores
│   │   ├── comparison/          # Comparación y análisis
│   │   ├── reporting/           # Generadores de reporte
│   │   ├── gui/                 # Interfaz gráfica
│   │   └── Launcher.java        # Punto de entrada
│   └── application.properties   # Configuración
├── lib/                         # Librerías externas (JAR)
├── bin/                         # Archivos compilados (generado)
├── dist/                        # JAR ejecutable (generado)
├── ACES/                        # Archivos de ejemplo
├── catalogoDb/                  # Base de datos de catálogos
├── tasks.md                     # Plan de implementación
├── build.xml                    # Script de compilación Ant
├── IMPLEMENTATION_GUIDE.md      # Guía técnica de implementación
└── README.md                    # Este archivo
```

## Requisitos

- **Java 11 o superior**
- **Apache Ant** (para compilación con build.xml)
- Archivo de catálogo ACES en formato .xlsx
- Archivo de aplicación a validar en formato .xlsx

## Compilación

### Opción 1: Con Apache Ant (Recomendado)

```bash
# Ver información
ant info

# Compilar
ant compile

# Empaquetar JAR
ant jar

# Ejecutar
ant run

# Todo (clean, compile, jar, javadoc)
ant all
```

### Opción 2: Con Java directamente

```bash
# Compilar
javac -cp lib/*:. -d bin src/com/validador/aces/**/*.java

# Ejecutar
java -cp bin:lib/* com.validador.aces.Launcher

# Empaquetar
jar cfm dist/validador-aces.jar manifest.txt -C bin .
```

### Opción 3: Con Visual Studio Code

1. Asegúrate que tienes las extensiones Java instaladas
2. Abre la carpeta del proyecto
3. Usa la vista de "Java Projects" para compilar
4. Ejecuta `App.java` desde el editor

## Ejecución

### Interfaz Gráfica (Recomendado)

```bash
# Desde JAR empaquetado
java -jar dist/validador-aces.jar

# Desde clases compiladas
java -cp bin:lib/* com.validador.aces.Launcher
```

### Línea de Comandos (Futuro)

```bash
java -jar validador-aces.jar \
  --catalog catalogo.xlsx \
  --app aplicacion.xlsx \
  --output reporte.xlsx \
  --strictness NORMAL
```

## Uso

### Flujo de Trabajo Básico

1. **Cargar Catálogo**
   - Haz clic en "File → Open Catalog"
   - Selecciona archivo Excel con catálogo ACES
   - Espera a que se cargue

2. **Cargar Aplicación**
   - Haz clic en "File → Open Application"
   - Selecciona archivo Excel con datos de aplicación
   - Espera a que se cargue

3. **Ejecutar Validación**
   - Haz clic en botón "Validate"
   - Observa progreso de validación
   - Revisa resultados en tabla

4. **Análisis de Resultados**
   - Observa compliance percentage
   - Filtra por severity (Error, Warning, Info)
   - Filtra por ProductLine
   - Revisa estadísticas por tipo

5. **Exportar Reporte**
   - Haz clic en botón "Export Report"
   - Selecciona ubicación y nombre
   - Reporte Excel se genera con múltiples hojas

## Plan de Implementación

Ver archivo `tasks.md` para lista completa de 54 tareas organizadas por componente:

- **TASK-001 a TASK-007**: Modelos de Datos
- **TASK-008 a TASK-012**: Parsers
- **TASK-013 a TASK-018**: Validadores
- **TASK-019 a TASK-021**: Comparación
- **TASK-022 a TASK-026**: Reportes
- **TASK-027 a TASK-034**: GUI
- **TASK-035 a TASK-047**: Testing
- **TASK-051 a TASK-054**: Empaquetado

Cada tarea incluye: Descripción, Criterios de Aceptación, Dependencias, Complejidad y Prioridad.

## Guía Técnica

Ver archivo `IMPLEMENTATION_GUIDE.md` para:

- Estructura de directorios recomendada
- Dependencias externas
- Patrones de diseño a usar
- Enumeraciones y interfaces clave
- Configuración
- Manejo de excepciones
- Logging y testing
- Checklist de implementación

## Características Principales

### v1.0 (MVP)
- ✅ Carga de catálogos Excel
- ✅ Carga de aplicaciones Excel
- ✅ Validadores básicos (atributo, rango, enum, fecha)
- ✅ Interfaz gráfica simple
- ✅ Reporte Excel con estadísticas
- ✅ Auditoría básica

### v1.1 (Planeado)
- 🔲 Property-based testing
- 🔲 Caché de resultados
- 🔲 Más validadores especializados
- 🔲 Estadísticas avanzadas con gráficos

### v2.0 (Futuro)
- 🔲 API REST
- 🔲 Base de datos para historial
- 🔲 Exportación a PDF
- 🔲 Validación distribuida/paralela

## Configuración

El archivo `src/application.properties` contiene:

- Parámetros de validación (strictness, max-errors, etc)
- Configuración de caché
- Opciones de reporte
- Personalización de GUI
- Configuración de auditoría
- Opciones de logging

Edita este archivo para cambiar el comportamiento de la aplicación.

## Librerías Incluidas

- **fastexcel** - Lectura/escritura eficiente de Excel
- **commons-io, commons-lang3** - Utilidades Java
- **aalto-xml, stax2** - Procesamiento XML
- **commons-compress** - Compresión de archivos

## Troubleshooting

### "No class found" al compilar
```bash
ant clean compile
```

### "Cannot find symbol" para clases del proyecto
- Asegúrate de seguir el orden de tareas (respetar dependencias)
- Verifica que el paquete `com.validador.aces` está en `src/`

### Interfaz gráfica no aparece
```bash
java -cp bin:lib/* com.validador.aces.Launcher
```

### Archivo Excel no se carga
- Verifica que es formato .xlsx (no .xls)
- Verifica que el archivo no está corrupto
- Revisa logs en `validador-aces.log`

## Documentación

- `tasks.md` - Plan de implementación detallado
- `IMPLEMENTATION_GUIDE.md` - Guía técnica
- JavaDoc - Ver en `docs/` después de compilar con `ant javadoc`

## Contribución

1. Selecciona una tarea de `tasks.md`
2. Respeta dependencias con otras tareas
3. Sigue estructura de paquetes recomendada
4. Incluye unit tests para nueva funcionalidad
5. Mantén comentarios y documentación actualizados

## Licencia

Interno - ACES Team

## Contacto

Para preguntas sobre implementación, ver `IMPLEMENTATION_GUIDE.md` o revisar comentarios en el código.

---

**Versión**: 1.0.0  
**Estado**: En desarrollo  
**Última actualización**: 2024
