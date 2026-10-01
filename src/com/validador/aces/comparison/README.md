# Paquete: comparison

## Descripción

Contiene la lógica para comparar catálogos y detectar diferencias.

## Clases

### TASK-019: CatalogComparator
- **Descripción**: Comparador base para catálogos
- **Métodos**: compare(), findDifferences(), generateDiff()
- **Retorna**: ComparisonResult con diferencias identificadas
- **Estado**: [TODO]

### TASK-020: AttributeDifferenceDetector
- **Descripción**: Detecta diferencias en atributos
- **Métodos**: detectAdded(), detectRemoved(), detectModified()
- **Tipos de diferencia**: Nuevo, eliminado, modificado, reordenado
- **Estado**: [TODO]

### TASK-021: ChangeAnalyzer
- **Descripción**: Analiza y categoriza cambios
- **Métodos**: analyzeChange(), calculateImpact(), suggestMigration()
- **Categorías**: Breaking change, non-breaking change, deprecated
- **Estado**: [TODO]

## Dependencias

- Depende de: models (Catalog, ComparisonResult, etc.)
- Depende de: validation (para validar cambios)

## Archivos Esperados

```
comparison/
├── CatalogComparator.java
├── AttributeDifferenceDetector.java
├── ChangeAnalyzer.java
└── README.md
```

## Tipos de Cambios

### Breaking Changes
- Eliminación de atributos requeridos
- Cambio de tipo de dato (reducción)
- Cambio en validaciones (más restrictivo)

### Non-Breaking Changes
- Adición de atributos opcionales
- Cambio de tipo (ampliación)
- Cambio en validaciones (menos restrictivo)

### Deprecated
- Marcado para eliminación futura
- Reemplazo sugerido

## Diferencias Detectables

- Atributos nuevos/eliminados
- Cambios en propiedades (nombre, tipo, descripción)
- Cambios en validaciones
- Cambios en valores permitidos
- Reorden de atributos
- Cambios en líneas de producto

## Notas

- Generar reportes detallados de cambios
- Sugerir migraciones cuando sea posible
- Identificar impacto potencial en aplicaciones existentes
- Mantener historial de comparaciones
