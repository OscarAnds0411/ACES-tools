# Quick Start - Validador ACES

## ⚡ 5 Minutos para Empezar

### 1. Verifica Java 11+
```bash
java -version
# Debe mostrar "java version 11" o superior
```

### 2. Verifica Apache Ant (Opcional pero Recomendado)
```bash
ant -version
# Debe mostrar "Apache Ant version 1.10.x" o superior
```

### 3. Ver información del Proyecto
```bash
ant info
```

### 4. Compila el Código
```bash
ant compile
# Si no tienes Ant, usa:
# javac -cp lib/*:. -d bin src/com/validador/aces/**/*.java
```

### 5. Ejecuta la Aplicación
```bash
ant run
# O ejecuta directamente:
# java -cp bin:lib/* com.validador.aces.Launcher
```

---

## 📚 Documentos Clave

1. **tasks.md** (54 tareas)
   - Plan detallado de implementación
   - Dependencias entre tareas
   - Criterios de aceptación

2. **IMPLEMENTATION_GUIDE.md** (Guía Técnica)
   - Estructura de directorios
   - Patrones de diseño
   - Configuración

3. **EXCEL_FORMAT_GUIDE.md** (Formato de Datos)
   - Estructura esperada de catálogos
   - Estructura esperada de aplicaciones
   - Ejemplos

4. **README.md** (Documentación General)
   - Descripción del proyecto
   - Características
   - Troubleshooting

---

## 🔨 Comandos Build Útiles

```bash
# Compilar código
ant compile

# Empaquetar JAR
ant jar

# Compilar + Empaquetar + Ejecutar
ant run

# Generar documentación JavaDoc
ant javadoc

# Limpiar binarios
ant clean

# Ver todos los targets
ant info
```

---

## 📂 Estructura Básica

```
project/
├── src/
│   └── com/validador/aces/     ← Tu código aquí
│       ├── models/             (Modelos: TASK-001-007)
│       ├── parsers/            (Parsers: TASK-008-012)
│       ├── validation/         (Validadores: TASK-013-018)
│       ├── comparison/         (Comparación: TASK-019-021)
│       ├── reporting/          (Reportes: TASK-022-026)
│       ├── gui/                (GUI: TASK-027-034)
│       ├── tests/              (Tests: TASK-035-047)
│       └── Launcher.java       (Entrada: TASK-053)
├── lib/                        (Librerías)
├── bin/                        (Compilado - generado)
├── dist/                       (JAR - generado)
└── tasks.md                    (Lista de tareas)
```

---

## 🎯 Flujo de Trabajo

### Para Implementadores

1. Abre `tasks.md`
2. Selecciona TASK-001 (o cualquiera disponible)
3. Lee descripción, criterios de aceptación, dependencias
4. Verifica que todas las dependencias están completas
5. Implementa según el criterio
6. Ejecuta `ant compile` para verificar
7. Marca tarea como completada
8. Repite con siguiente tarea

### Para Testers

1. Lee `IMPLEMENTATION_GUIDE.md` section "Herramientas de Testing"
2. Revisa `tasks.md` sección "TESTING" (TASK-035+)
3. Ejecuta tests con framework correspondiente
4. Reporta bugs o inicia nuevas tareas

### Para QA

1. Prepara archivos Excel según `EXCEL_FORMAT_GUIDE.md`
2. Usa GUI para validar archivos
3. Verifica que reportes generados sean correctos
4. Reporta cualquier discrepancia

---

## 📋 Checklist de Inicio

- [ ] Leer este archivo (QUICK_START.md)
- [ ] Ejecutar `ant info` (verificar Ant está instalado)
- [ ] Leer `README.md` (5 minutos)
- [ ] Leer `tasks.md` primeras 50 líneas (5 minutos)
- [ ] Leer `IMPLEMENTATION_GUIDE.md` índice (5 minutos)
- [ ] Crear estructura de directorios en `src/com/validador/aces/`
- [ ] Ejecutar `ant compile` (debe fallar gracefully)
- [ ] Comenzar TASK-001

---

## 🚀 Primeras 3 Tareas

### TASK-001: Crear modelo `Catalog`
```
Archivo: src/com/validador/aces/models/Catalog.java
Propiedades: id, name, version, description, createdDate, productLines
Métodos: getters, setters, addProductLine()
Verificar: ant compile
```

### TASK-002: Crear modelo `ProductLine`
```
Archivo: src/com/validador/aces/models/ProductLine.java
Propiedades: id, name, description, attributes
Métodos: getters, setters, addAttribute(), findAttribute()
Verificar: ant compile
```

### TASK-003: Crear modelo `Attribute`
```
Archivo: src/com/validador/aces/models/Attribute.java
Propiedades: id, name, type, required, validation, allowedValues, description
Enums: AttributeType (STRING, INTEGER, DECIMAL, BOOLEAN, DATE)
Métodos: getters, setters, validateValue()
Verificar: ant compile
```

---

## 💡 Tips

1. **Compilación frecuente**: Ejecuta `ant compile` después de cada tarea
2. **Dependencias primero**: Respeta orden de dependencias en tasks.md
3. **Tests opcionales**: Marcados con `*` pueden saltarse para MVP
4. **Configuración**: Edita `src/application.properties` según necesites
5. **Documentación**: Mantén comentarios JavaDoc actualizados

---

## 🐛 Problemas Comunes

### "No suitable JavaCompiler found"
- Asegúrate que JDK (no JRE) está instalado
- Verifica: `javac -version`

### "Ant command not found"
- Instala Apache Ant desde https://ant.apache.org
- O usa compilación manual: `javac -cp lib/*:. -d bin src/com/validador/aces/**/*.java`

### "Cannot find symbol" errors
- Verifica que crear clases en orden (respetar dependencias)
- TASK-001 → TASK-002 → TASK-003, etc

### "Class not found: Launcher"
- Espera a TASK-053 para crear Launcher.java
- Mientras tanto, compila otros componentes

---

## 📞 Necesitas Ayuda?

1. **Sobre tareas específicas**: Mira `tasks.md` - incluye descripción, criterios, dependencias
2. **Sobre implementación técnica**: Lee `IMPLEMENTATION_GUIDE.md`
3. **Sobre formato de datos**: Lee `EXCEL_FORMAT_GUIDE.md`
4. **Sobre compilación**: Lee `README.md` o ejecuta `ant info`
5. **Sobre estructura del proyecto**: Lee `IMPLEMENTATION_GUIDE.md` sección 1

---

## 📈 Progreso Esperado

```
Día 1: TASK-001 a TASK-007 (Modelos)
       Tiempo: 2-4 horas
       Complejidad: Baja
       
Día 2: TASK-008 a TASK-012 (Parsers)
       Tiempo: 4-6 horas
       Complejidad: Media
       
Día 3: TASK-013 a TASK-018 (Validadores)
       Tiempo: 4-6 horas
       Complejidad: Media
       
Día 4: TASK-019 a TASK-026 (Comparación + Reportes)
       Tiempo: 4-6 horas
       Complejidad: Media
       
Día 5: TASK-027 a TASK-034 (GUI)
       Tiempo: 6-8 horas
       Complejidad: Alta
       
Día 6: TASK-035 a TASK-047 (Testing)
       Tiempo: 4-6 horas (opcional para MVP)
       Complejidad: Media
       
Día 7: TASK-048 a TASK-054 (Docs + Build)
       Tiempo: 2-3 horas
       Complejidad: Baja
```

---

**Versión**: 1.0  
**Actualizado**: 2024-02-20  
**Estado**: Listo
