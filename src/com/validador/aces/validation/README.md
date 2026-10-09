# Paquete: validation

Reglas que revisan una `Application` contra su línea de producto del catálogo.

| Clase | Rol | ¿Se usa en la aplicación? |
|---|---|---|
| `Validator` | Clase base abstracta: `validate(Application, Catalog)` → `List<ValidationError>` | — |
| `AttributeValidator` | Detecta atributos **requeridos** ausentes o vacíos | **Sí** (lo usa `Comparator`) |
| `ValidationSchema` | Separa los atributos de una línea en requeridos y opcionales | **Sí** |
| `CompositeValidator` | Ejecuta varios `Validator` y acumula errores; `stopOnFirstCriticalError` | No (hoy solo hay un validador) |
| `EnumValidator` | El valor de un atributo debe estar en una lista | No (solo pruebas) |
| `RangeValidator` | El valor numérico de un atributo debe estar en un rango | No (solo pruebas) |
| `DateFormatValidator` | El valor debe tener un formato de fecha | No (sin pruebas) |

## `AttributeValidator`

- `validate(app, catalog)`: busca la línea con `Catalog.findProductByName` y delega.
- `validateResolved(app, línea, esquema)`: igual, con la línea y el esquema ya resueltos; es lo que usa `Comparator.compareAll` para no buscar en el catálogo por cada aplicación.
- Línea no encontrada → un `WARNING` `PRODUCT_LINE_NOT_FOUND` ("sin clasificar"; no penaliza el compliance).
- Atributo requerido faltante → un `ERROR` `MISSING_REQUIRED_ATTRIBUTE` por atributo. Un valor en blanco cuenta como ausente.

## Agregar una regla

Subclase de `Validator` y agregarla a un `CompositeValidator`. Hoy `Comparator` no construye ese compuesto: conectar `Enum`/`Range`/`DateFormat` exige además decidir dónde se declaran las reglas (el catálogo solo dice Required/Optional) y cómo afectan al compliance. Ver `LOGICA_DEL_PROGRAMA.md` §4.3.
