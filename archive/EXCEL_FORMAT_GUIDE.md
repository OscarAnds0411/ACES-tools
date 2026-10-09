# Guía de Formato Excel - Catálogos y Aplicaciones ACES

## 1. ESTRUCTURA DE CATÁLOGO ACES (.xlsx)

### Sheet 1: "Catalog" o "Catálogo"

Información general del catálogo.

| Column A | Column B | Column C | Column D |
|----------|----------|----------|----------|
| **Field** | **Value** | | |
| Catalog ID | ACES-2024-Q1 | | |
| Catalog Name | ACES Master Catalog 2024 Q1 | | |
| Version | 2.5.1 | | |
| Description | Master catalog for ACES attributes | | |
| Created Date | 2024-01-15 | | |
| Last Modified | 2024-02-20 | | |

### Sheet 2: "ProductLines" o "Líneas"

Lista de líneas de producto.

| Column A | Column B | Column C |
|----------|----------|----------|
| **ProductLine ID** | **ProductLine Name** | **Description** |
| PL-001 | Electronics | Categoría de productos electrónicos |
| PL-002 | Clothing | Categoría de prendas de vestir |
| PL-003 | Home & Garden | Categoría de hogar y jardín |
| PL-004 | Sports | Categoría de artículos deportivos |

### Sheet 3+: Atributos por Línea

**Nombre de Sheet**: `PL-001_Attributes`, `PL-002_Attributes`, etc.

| Column A | Column B | Column C | Column D | Column E | Column F | Column G | Column H |
|----------|----------|----------|----------|----------|----------|----------|----------|
| **Attr ID** | **Attr Name** | **Type** | **Required** | **Min Value** | **Max Value** | **Allowed Values** | **Description** |
| ATTR-001 | Brand | STRING | YES | | | | Brand name |
| ATTR-002 | Model Number | STRING | YES | | | | Model identifier |
| ATTR-003 | Unit Price | DECIMAL | YES | 0 | 999999.99 | | Price in USD |
| ATTR-004 | Stock Quantity | INTEGER | YES | 0 | | | Available units |
| ATTR-005 | Release Date | DATE | NO | | | | ISO 8601 format |
| ATTR-006 | Is Active | BOOLEAN | YES | | | TRUE,FALSE | Product status |
| ATTR-007 | Category | STRING | YES | | | Electronics,Accessories,Replacement | Product category |

### Convenciones

| Columna | Descripción | Ejemplo |
|---------|-------------|---------|
| Attr ID | Identificador único del atributo | ATTR-001 |
| Attr Name | Nombre descriptivo | "Brand", "Model Number" |
| Type | Tipo de dato | STRING, INTEGER, DECIMAL, BOOLEAN, DATE |
| Required | Si es obligatorio | YES, NO |
| Min Value | Valor mínimo (números) | 0, -100 |
| Max Value | Valor máximo (números) | 999.99, 1000 |
| Allowed Values | Valores permitidos (enum) | "RED,GREEN,BLUE" o "TRUE,FALSE" |
| Description | Descripción del atributo | Cualquier texto descriptivo |

## 2. ESTRUCTURA DE APLICACIÓN (.xlsx)

### Sheet 1: "Application Info"

Información general de la aplicación.

| Column A | Column B |
|----------|----------|
| **Field** | **Value** |
| Application Name | My Store Application |
| Application Version | 1.2.3 |
| Last Updated | 2024-02-20 |
| Target Catalog | ACES-2024-Q1 |

### Sheet 2+: Datos por Línea de Producto

**Nombre de Sheet**: `PL-001_Data`, `PL-002_Data`, etc.

Debe coincidir con los atributos del catálogo.

**Ejemplo para Electronics (PL-001)**:

| ATTR-001 Brand | ATTR-002 Model Number | ATTR-003 Unit Price | ATTR-004 Stock Quantity | ATTR-005 Release Date | ATTR-006 Is Active | ATTR-007 Category |
|----------|----------|----------|----------|----------|----------|----------|
| Samsung | SM-G990B | 899.99 | 150 | 2024-01-10 | TRUE | Electronics |
| Apple | A2846 | 1099.99 | 200 | 2024-01-15 | TRUE | Electronics |
| Sony | WH-1000XM5 | 399.99 | 75 | 2023-12-01 | TRUE | Accessories |
| LG | 65UP7750-W | 1299.99 | 0 | 2024-02-01 | FALSE | Electronics |
| | | | | | | |

### Convenciones

- **Encabezados**: Deben ser exactamente "ATTR-XXX AttributeName"
- **Valores en blanco**: Se tratan como NULL (puede ser inválido si Required=YES)
- **Espacios en blanco**: Se preservan (excepto leading/trailing que se trimean)
- **Formato de fecha**: ISO 8601 (YYYY-MM-DD)
- **Booleanos**: "TRUE", "FALSE", "true", "false", "Yes", "No", "Y", "N"
- **Números**: Punto decimal (.) para separador decimal

## 3. TIPOS DE DATO SOPORTADOS

### STRING
- Cualquier cadena de caracteres
- Máximo: 10,000 caracteres
- Validación: Longitud mínima/máxima (si se especifica)

```
Ejemplo: "Samsung Galaxy S24"
Validación: Puede incluir regex en Allowed Values
```

### INTEGER
- Números enteros (sin decimales)
- Rango: -2,147,483,648 a 2,147,483,647
- Validación: Rango mínimo/máximo

```
Ejemplo: 150
Validación: Min=0, Max=1000
```

### DECIMAL
- Números con decimales
- Precisión: 2 decimales (centavos)
- Validación: Rango mínimo/máximo

```
Ejemplo: 899.99
Validación: Min=0, Max=999999.99
```

### BOOLEAN
- Valores verdadero/falso
- Aceptados: TRUE, FALSE, true, false, Yes, No, Y, N, 1, 0

```
Ejemplo: TRUE
Validación: Solo estos valores permitidos
```

### DATE
- Fechas en formato ISO 8601
- Formato: YYYY-MM-DD
- Validación: Fecha válida, rango (si se especifica)

```
Ejemplo: 2024-02-20
Validación: YYYY-MM-DD, 1900-01-01 a 2099-12-31
```

## 4. VALIDACIONES

### Atributo Requerido (Required = YES)

```
❌ Valor en blanco → ERROR: "Required attribute missing"
❌ Solo espacios → ERROR: "Required attribute is blank"
✅ Cualquier valor no vacío → OK
```

### Tipo de Dato Incorrecto

```
Atributo Type=INTEGER, Valor="abc"
❌ → ERROR: "Invalid data type: expected INTEGER, got STRING"
```

### Rango Fuera de Límites

```
Atributo Type=INTEGER, Min=0, Max=100
❌ Valor: -5 → ERROR: "Value -5 is below minimum 0"
❌ Valor: 150 → ERROR: "Value 150 exceeds maximum 100"
✅ Valor: 50 → OK
```

### Valor No en Enum

```
Atributo Allowed Values="RED,GREEN,BLUE"
❌ Valor: "YELLOW" → ERROR: "Value YELLOW not in allowed list: RED, GREEN, BLUE"
✅ Valor: "RED" → OK (case-insensitive según config)
```

### Formato de Fecha Inválido

```
Atributo Type=DATE
❌ Valor: "2024/02/20" → ERROR: "Invalid date format: expected YYYY-MM-DD"
❌ Valor: "02-20-2024" → ERROR: "Invalid date format: expected YYYY-MM-DD"
✅ Valor: "2024-02-20" → OK
```

## 5. EJEMPLOS DE ARCHIVOS VÁLIDOS

### Catálogo Mínimo Válido

```
Sheet "Catalog":
| Catalog ID | ACES-2024-V1 |
| Name | ACES Catalog |

Sheet "ProductLines":
| ProductLine ID | ProductLine Name |
| PL-001 | Electronics |

Sheet "PL-001_Attributes":
| Attr ID | Attr Name | Type | Required |
| ATTR-001 | Brand | STRING | YES |
```

### Aplicación Mínima Válida

```
Sheet "Application Info":
| Application Name | My App |
| Target Catalog | ACES-2024-V1 |

Sheet "PL-001_Data":
| ATTR-001 Brand |
| Samsung |
| Apple |
```

## 6. ERRORES COMUNES

### Error: "Sheet not found"
- Verificar que nombre de sheet coincida exactamente
- Nombres son case-sensitive
- Usar prefijo correcto: "PL-001_Attributes", "PL-001_Data"

### Error: "Column header mismatch"
- Encabezados deben ser: "ATTR-XXX AttributeName"
- Sin espacios extras al inicio/final
- Verificar que ID de atributo coincida con catálogo

### Error: "No ProductLines found"
- Verificar que sheet "ProductLines" existe
- Verificar que contiene filas de datos (no solo encabezado)
- ProductLine ID debe coincidir con nombres de sheets

### Error: "Malformed Excel file"
- Archivo puede estar corrupto
- Intentar abrir y resguardar en Excel
- Verificar que no hay caracteres especiales en rutas

## 7. MEJORES PRÁCTICAS

### Nombrado de Hojas
```
✅ "Catalog"
✅ "ProductLines"
✅ "PL-001_Attributes"
❌ "Attributes_PL_001" (orden diferente)
❌ "PL-001 Attributes" (espacios)
```

### Identificadores Únicos
```
✅ ATTR-001, ATTR-002, ... (secuencial)
✅ PL-ELECT, PL-CLOTH, ... (mnemotécnico)
❌ 1, 2, 3 (sin prefijo)
❌ attr_001 (guiones bajos)
```

### Descripción de Campos
```
✅ "Brand name of product"
✅ "Price in USD, including tax"
❌ "Brand" (demasiado vago)
❌ "Bla bla bla" (no descriptivo)
```

### Validaciones
```
✅ Type=DECIMAL, Min=0, Max=999999.99
✅ Type=STRING, Allowed="RED,GREEN,BLUE"
❌ Type=INTEGER, Min="0", Max="100" (strings)
❌ Type=STRING, Min=0 (inconsistente)
```

## 8. FORMATO RECOMENDADO

### Catálogo ACES
```
validador-de-atributos-aces.xlsx
├── Sheet 1: Catalog (info general)
├── Sheet 2: ProductLines (índice de líneas)
├── Sheet 3: PL-ELECT_Attributes (atributos electrónica)
├── Sheet 4: PL-CLOTH_Attributes (atributos ropa)
└── Sheet 5: PL-HOME_Attributes (atributos hogar)
```

### Aplicación a Validar
```
mi-tienda-datos.xlsx
├── Sheet 1: Application Info
├── Sheet 2: PL-ELECT_Data (datos de electrónica)
├── Sheet 3: PL-CLOTH_Data (datos de ropa)
└── Sheet 4: PL-HOME_Data (datos de hogar)
```

---

**Nota**: Todos los nombres de sheets y columnas son case-sensitive en la implementación final. Verificar exactitud.
