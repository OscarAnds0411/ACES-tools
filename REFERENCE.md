# 📖 Referencia Rápida - Validador ACES

## 🚀 Comienza en 3 Segundos

```bash
ant info              # Ver información del proyecto
ant compile           # Compilar código
ant jar               # Empaquetar JAR ejecutable
ant run               # Ejecutar aplicación
```

---

## 📚 Tabla de Documentos

| # | Archivo | Lectura | Para Qué |
|---|---------|---------|----------|
| ⭐ | [START_HERE.md](START_HERE.md) | **2 min** | 👈 Comienza aquí |
| 1️⃣ | [QUICK_START.md](QUICK_START.md) | **5 min** | Primeros pasos |
| 2️⃣ | [README.md](README.md) | **10 min** | Visión general |
| 3️⃣ | [tasks.md](tasks.md) | **30 min** | Plan completo (54 tareas) |
| 4️⃣ | [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) | **20 min** | Detalles técnicos |
| 5️⃣ | [EXCEL_FORMAT_GUIDE.md](EXCEL_FORMAT_GUIDE.md) | **15 min** | Formato de datos |
| 6️⃣ | [INDEX.md](INDEX.md) | **10 min** | Mapa de navegación |
| 7️⃣ | [GENERATED_ARTIFACTS.md](GENERATED_ARTIFACTS.md) | **5 min** | Resumen entrega |

**Tiempo total lectura completa: ~97 minutos**

---

## 🎯 Tareas Principales (54 Total)

### Por Componente

| Componente | Rango | Cantidad | Complejidad |
|-----------|-------|----------|------------|
| 🏗️ Modelos | TASK-001-007 | 7 | Baja |
| 📥 Parsers | TASK-008-012 | 5 | Media |
| ✅ Validadores | TASK-013-018 | 6 | Media |
| 🔄 Comparación | TASK-019-021 | 3 | Media |
| 📊 Reportes | TASK-022-026 | 5 | Media-Alta |
| 🖥️ GUI | TASK-027-034 | 8 | Alta |
| 🧪 Testing | TASK-035-047 | 13 | Media |
| 📖 Documentación | TASK-048-050 | 3 | Baja |
| 📦 Empaquetado | TASK-051-054 | 4 | Baja |

---

## 📋 Tareas por Prioridad

### Required (33 tareas) - MVP Obligatorio
```
TASK-001 a TASK-021  (Modelos, Parsers, Validadores, Comparación)
TASK-022-023         (ReportGenerator, ExcelReportGenerator)
TASK-027-030         (GUI Principal)
TASK-032             (File Dialogs)
TASK-051-054         (Build - empaquetado)
```

### Optional (21 tareas) - Mejoras
```
TASK-007, 018, 021, 024-026 (Auditoría, Exportadores)
TASK-031, 033-034            (Estadísticas avanzadas, Auditoría GUI)
TASK-035-047                 (Todos los tests - para PBT)
TASK-048-050                 (Documentación)
```

---

## 🔧 Comandos Ant

```bash
# Información
ant info            # Mostrar información del proyecto
ant -projecthelp    # Mostrar targets disponibles

# Build
ant clean           # Limpiar binarios
ant compile         # Compilar código
ant jar             # Empaquetar JAR
ant all             # clean + compile + jar + javadoc

# Ejecución
ant run             # Compilar + empaquetar + ejecutar
ant javadoc         # Generar documentación
ant test            # Ejecutar tests (placeholder)
```

---

## 🗂️ Estructura de Directorios

```
src/
└── com/validador/aces/
    ├── models/               # TASK-001-007
    ├── parsers/              # TASK-008-012
    ├── validation/           # TASK-013-018
    ├── comparison/           # TASK-019-021
    ├── reporting/            # TASK-022-026
    ├── gui/                  # TASK-027-034
    ├── tests/                # TASK-035-047
    └── Launcher.java         # TASK-053
```

---

## 📊 Tipos de Dato Soportados

| Tipo | Ejemplo | Validación |
|------|---------|-----------|
| STRING | "Samsung Galaxy" | Longitud, regex |
| INTEGER | 150 | Rango min/max |
| DECIMAL | 899.99 | Rango min/max |
| BOOLEAN | TRUE | TRUE/FALSE/Yes/No |
| DATE | 2024-02-20 | ISO 8601 (YYYY-MM-DD) |

---

## ✅ Validaciones Disponibles

| Validador | Propósito |
|-----------|----------|
| AttributeValidator | Presencia y tipo correcto |
| RangeValidator | Valores min/max |
| EnumValidator | Valores en lista permitida |
| DateFormatValidator | Formato de fecha ISO 8601 |
| CompositeValidator | Ejecuta múltiples validadores |

---

## 📈 Configuración Principal

```properties
# Validación
validation.strictness=NORMAL              # LENIENT, NORMAL, STRICT
validation.max-errors=1000                # Máximo errores antes de parar
validation.case-sensitive=false           # Case-sensitive en enum

# Reporte
report.include-valid-attributes=false     # Incluir válidos en reporte
report.include-statistics=true            # Incluir estadísticas

# GUI
gui.font-size=12                          # Tamaño de fuente
gui.window.width=1024                     # Ancho ventana
gui.window.height=768                     # Alto ventana
```

Ver completo: `src/application.properties`

---

## 🎓 Orden de Implementación Recomendado

```
Día 1: TASK-001-007   (Modelos)              2-4h   ✅ Compilar
Día 2: TASK-008-012   (Parsers)              4-6h   ✅ Compilar
Día 3: TASK-013-018   (Validadores)          4-6h   ✅ Compilar
Día 4: TASK-019-026   (Comparación + Report) 6-9h   ✅ Compilar
Día 5: TASK-027-034   (GUI)                  6-8h   ✅ Ejecutar
Día 6: TASK-035-047   (Testing - opcional)   4-6h   ✅ Tests
Día 7: TASK-048-054   (Docs + Build)         3-5h   ✅ JAR
```

---

## 🧪 Testing Strategy

### Unit Tests (TASK-035-042)
- Parsers: TASK-035-036
- Validadores: TASK-039-042
- **Framework**: JUnit 5 + Mockito

### Property-Based Tests (TASK-037-038, 045, 047)
- Round-trip: TASK-037-038
- Idempotence: TASK-045
- Completeness: TASK-047
- **Framework**: jqwik o QuickCheck

### Integration Tests (TASK-044)
- End-to-end flow
- **Framework**: JUnit 5

---

## 📝 Checklist de Inicio

- [ ] Leer [START_HERE.md](START_HERE.md) (2 min)
- [ ] Leer [QUICK_START.md](QUICK_START.md) (5 min)
- [ ] Ejecutar `java -version` (verifica Java 11+)
- [ ] Ejecutar `ant info` (verifica Ant)
- [ ] Ejecutar `ant compile` (verá errores esperados)
- [ ] Crear estructura en `src/com/validador/aces/`
- [ ] Implementar TASK-001 (Catalog)
- [ ] Ejecutar `ant compile` (debe funcionar)
- [ ] Listo para TASK-002

---

## 🐛 Troubleshooting Rápido

| Problema | Solución |
|----------|----------|
| "javac not found" | Instala JDK (no JRE) |
| "ant not found" | Instala Apache Ant |
| "Cannot find symbol" | Verifica paquetes en estructura |
| "Compilation errors" | Verifica dependencias entre tareas |
| "No class: Launcher" | Espera a TASK-053, mientras compila otros |

---

## 🔗 Links Directos

### Documentación
- [START_HERE.md](START_HERE.md) - 👈 Comienza aquí
- [tasks.md](tasks.md) - Plan de tareas
- [QUICK_START.md](QUICK_START.md) - Guía rápida
- [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) - Técnica

### Configuración
- [build.xml](build.xml) - Compilación
- [application.properties](src/application.properties) - Config app
- [manifest.txt](manifest.txt) - JAR metadata

### Referencia
- [README.md](README.md) - General
- [INDEX.md](INDEX.md) - Índice completo
- [EXCEL_FORMAT_GUIDE.md](EXCEL_FORMAT_GUIDE.md) - Datos
- [REFERENCE.md](REFERENCE.md) - Este archivo

---

## 📊 Estadísticas

| Métrica | Valor |
|---------|-------|
| Tareas totales | 54 |
| Tareas required | 33 |
| Tareas optional | 21 |
| Componentes | 9 |
| Librerías JAR | 8 |
| Documentos | 8 |
| Tamaño documentación | ~75 KB |
| Tiempo estimado MVP | 20-30h |
| Tiempo estimado v1.0 | 28-43h |

---

## 💼 Usuarios del Sistema

| Rol | Interacción Principal |
|-----|----------------------|
| Desarrollador | Implementa tareas, compila |
| Tester | Prepara archivos, valida con GUI |
| QA | Prepara catálogos Excel, verifica reportes |
| DevOps | Maneja build.xml, configuración, deployment |
| DBA | Base de datos (futuro en v2.0) |

---

## 🎯 KPIs de Éxito

- [ ] Todas las 54 tareas completadas
- [ ] 90%+ cobertura de tests
- [ ] JAR ejecutable creado
- [ ] GUI funcional (cargar, validar, exportar)
- [ ] Reportes Excel correctos
- [ ] Zero compilation errors
- [ ] Performance: <2s para validación típica
- [ ] Documentación completa

---

## 📞 Soporte Rápido

| Necesito... | Reviso... | Tarea(s) |
|----------|----------|---------|
| Empezar | QUICK_START.md | - |
| Ver plan | tasks.md | - |
| Entender arquitectura | IMPLEMENTATION_GUIDE.md | - |
| Implementar Parsers | IMPLEMENTATION_GUIDE.md + tasks.md | TASK-008-012 |
| Validadores | IMPLEMENTATION_GUIDE.md + tasks.md | TASK-013-018 |
| GUI | IMPLEMENTATION_GUIDE.md + tasks.md | TASK-027-034 |
| Testing | IMPLEMENTATION_GUIDE.md | TASK-035-047 |
| Compilar | build.xml | - |
| Configurar | application.properties | - |
| Datos Excel | EXCEL_FORMAT_GUIDE.md | - |

---

## 🚀 Próximo Paso

**Ahora abre**: [START_HERE.md](START_HERE.md)

O ejecuta:
```bash
ant info
```

---

**Referencia Rápida v1.0**
**Actualizado**: 2024-02-20
**Proyecto**: Validador de Atributos ACES
