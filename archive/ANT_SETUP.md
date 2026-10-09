# 🔨 Configuración Apache Ant - Validador ACES

## ✅ Estado de Instalación

**Fecha**: Octubre 1, 2026  
**Estado**: ✅ COMPLETADO  
**Versión de Ant**: 1.10.14  
**Ubicación**: `C:\opt\ant`  
**Java**: OpenJDK 25.0.4.1 LTS  

---

## 📋 Resumen de Instalación

### Lo que se instaló

1. **Apache Ant 1.10.14** (versión estable)
   - Ubicación: `C:\opt\ant`
   - Directorio bin: `C:\opt\ant\bin`
   - Ejecutables: `ant.bat`, `ant.cmd`

2. **Variables de Entorno**
   - `ANT_HOME`: `C:\opt\ant`
   - `PATH`: Incluye `C:\opt\ant\bin`

3. **Alias PowerShell**
   - Agregado a: `C:\Users\12170\OneDrive - TecAlliance\Documents\WindowsPowerShell\Microsoft.PowerShell_profile.ps1`
   - Comando: `ant` (después de recargar PowerShell)

### Verificación

```powershell
# Verificar Ant está instalado
C:\opt\ant\bin\ant.bat -version
# Apache Ant(TM) version 1.10.14 compiled on August 16 2023

# Verificar información del proyecto
C:\opt\ant\bin\ant.bat info
# Muestra targets disponibles, directorios, versión del proyecto
```

---

## 🚀 Comandos Disponibles

Desde la carpeta del proyecto (`c:\Users\12170\Downloads\validadorDeAtributosAces`):

### Compilación

```powershell
# Compilar proyecto
C:\opt\ant\bin\ant.bat compile

# Limpiar archivos compilados
C:\opt\ant\bin\ant.bat clean

# Compilar todo (clean, compile, jar, javadoc)
C:\opt\ant\bin\ant.bat all
```

### Empaquetado

```powershell
# Crear JAR ejecutable
C:\opt\ant\bin\ant.bat jar

# Compilar, empaquetar y ejecutar
C:\opt\ant\bin\ant.bat run
```

### Documentación

```powershell
# Generar JavaDoc
C:\opt\ant\bin\ant.bat javadoc
```

### Información

```powershell
# Ver información del proyecto y targets disponibles
C:\opt\ant\bin\ant.bat info
```

---

## 📂 Estructura del Proyecto Ant

El archivo `build.xml` configura:

### Propiedades

```xml
<property name="src.dir" value="src"/>           <!-- Código fuente -->
<property name="bin.dir" value="bin"/>           <!-- Compilado -->
<property name="lib.dir" value="lib"/>           <!-- Librerías -->
<property name="dist.dir" value="dist"/>         <!-- Distribución (JAR) -->
<property name="docs.dir" value="docs"/>         <!-- Documentación -->
```

### Librerías incluidas

- `aalto-xml-1.4.0.jar`
- `commons-compress-1.28.0.jar`
- `commons-io-2.20.0.jar`
- `commons-lang3-3.18.0.jar`
- `fastexcel-0.20.2.jar`
- `fastexcel-reader-0.20.2.jar`
- `opczip-1.2.0.jar`
- `stax2-api-4.2.2.jar`

### Configuración Java

- **Source**: Java 11
- **Target**: Java 11
- **Encoding**: UTF-8
- **Debug**: Activado (líneas, variables, fuente)

### Punto de entrada

- **Main Class**: `com.validador.aces.Launcher`
- **Manifest**: Se genera automáticamente con `build.xml`

---

## 💡 Próximos Pasos

### 1. Crear estructura de directorios

```powershell
# Crear la estructura base
mkdir "src\com\validador\aces\models"
mkdir "src\com\validador\aces\parsers"
mkdir "src\com\validador\aces\validation"
mkdir "src\com\validador\aces\comparison"
mkdir "src\com\validador\aces\reporting"
mkdir "src\com\validador\aces\gui"
```

### 2. Comenzar con TASK-001

```
Archivo: src/com/validador/aces/models/Catalog.java
- Crear clase Catalog
- Definir propiedades
- Implementar getters y setters
- Método: addProductLine()
```

### 3. Compilar y verificar

```powershell
C:\opt\ant\bin\ant.bat compile
```

---

## 🔧 Configuración Avanzada

### Para usar `ant` directamente (sin ruta completa)

**Opción 1: Recargar PowerShell** (recomendado)
```powershell
# Cierra y reabre PowerShell
# Ahora puedes usar: ant compile
```

**Opción 2: Usar la ruta completa**
```powershell
# Siempre funciona
C:\opt\ant\bin\ant.bat compile
```

### Personalizar build.xml

El archivo `build.xml` es totalmente customizable:

- Modificar directorios: cambiar `<property>`
- Agregar librerías: agregar archivos a `lib/`
- Cambiar versión Java: modificar `source` y `target` en `<javac>`
- Agregar targets: copiar y personalizar `<target>`

---

## 📊 Archivos Generados por Ant

Después de ejecutar comandos Ant:

```
project/
├── bin/                    ← Generado por 'compile'
│   └── com/validador/aces/*.class
├── dist/                   ← Generado por 'jar'
│   ├── validador-aces.jar
│   ├── lib/                ← Copia de librerías
│   └── MANIFEST.MF
└── docs/                   ← Generado por 'javadoc'
    └── HTML documentation
```

---

## 🐛 Troubleshooting

### "ant: command not found"

**Solución**: Usa la ruta completa:
```powershell
C:\opt\ant\bin\ant.bat compile
```

O recarga PowerShell después de instalar.

### "Cannot find javac"

**Solución**: Verifica que tienes JDK (no solo JRE):
```powershell
javac -version
# Debe mostrar versión de compilador
```

### "No classes found to compile"

**Solución**: Crea la estructura de directorios:
```powershell
mkdir "src\com\validador\aces\models"
```

### Error de dependencias en compile

**Solución**: Asegúrate que las librerías existen en `lib/`:
```powershell
Get-ChildItem lib/
# Debe mostrar 8 archivos .jar
```

---

## 📞 Referencias

- **Apache Ant**: https://ant.apache.org/
- **Build.xml actual**: Archivo en raíz del proyecto
- **Documentación técnica**: `IMPLEMENTATION_GUIDE.md`
- **Plan de tareas**: `tasks.md`
- **Quick Start**: `QUICK_START.md`

---

## ✨ Checklist de Configuración

- [x] Java 11+ instalado y verificado
- [x] Apache Ant 1.10.14 descargado
- [x] Ant instalado en `C:\opt\ant`
- [x] ANT_HOME configurado
- [x] PATH actualizado
- [x] Alias PowerShell creado (requiere recarga)
- [x] Verificado con `ant info`
- [x] build.xml existente y configurado
- [ ] Crear estructura de directorios en `src/`
- [ ] Comenzar TASK-001 (crear Catalog.java)
- [ ] Ejecutar `ant compile`

---

**Creado por**: Kiro Agent  
**Versión**: 1.0  
**Última actualización**: Octubre 1, 2026  
