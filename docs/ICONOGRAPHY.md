# Iconografía de Mony

La UI solicita significados mediante `MonyIcon`; nunca selecciona directamente una librería
o un recurso. `MonyIconResolver` decide el asset según `IconPack` y aplica fallback Material de
forma centralizada.

## Tipos de asset

- `MonyIconAsset.Tintable`: `ImageVector` monocromático para Material, Lucide y Phosphor.
- `MonyIconAsset.Multicolor`: builder de un `ImageVector` de Mony Color, sin filtro de color global.

## Mony Color

Mony Color usa el set real **IconPark** (IconPark de ByteDance, Apache-2.0) empaquetado como
`ImageVector` de 48 x 48 en `ui/iconography/vendor/iconpark`. La app sigue siendo totalmente
offline: no hay dependencia runtime, descarga ni render de SVG.

### Diseños

IconPark no publica un archivo distinto por diseño: dibuja cada glifo una sola vez y deriva sus
cuatro diseños oficiales (**outline**, **filled**, **two-tone**, **multi-color**) reasignando cuatro
slots de color. El generador reproduce ese criterio, por lo que los 50 vectores se emiten una única
vez como `IconPark.<glifo>(palette: IconParkPalette)` y cambiar de diseño solo cambia la paleta.

Los slots son los del runtime de IconPark (`packages/svg/src/runtime`):

| Token del artwork | Slot de `IconParkPalette` | Papel |
| --- | --- | --- |
| `#000` | `outerStroke` | Contorno exterior, y relleno en glifos como `car` o `wifi` |
| `#2F88FF` | `outerFill` | Relleno exterior |
| `#fff` | `innerStroke` | Contorno interior, y relleno en `more-two`, `pin`, `strongbox`, `upload`, `view-list` |
| `#43CCF8` | `innerFill` | Relleno interior |

Un slot puede pintarse como relleno o como trazo según el path, por eso el código generado lo
resuelve por nombre y no asume un papel fijo. `outerFill` e `innerFill` admiten `null` porque el
diseño *Contorno* no pinta relleno.

Los cuatro diseños disponibles son:

| `MonyColorStyle` | Nombre en UI | Paleta |
| --- | --- | --- |
| `MULTI_COLOR` | Multicolor | `onSurface`, `#2F88FF`, `#fff`, `#43CCF8` |
| `TWO_TONE` | Bicolor | `onSurface`, `#2F88FF`, `onSurface`, `#2F88FF` |
| `OUTLINE` | Contorno | `onSurface`, sin relleno, `onSurface`, sin relleno |
| `FILLED` | Sólido | `onSurface`, `onSurface`, recorte, recorte |

`MULTI_COLOR` es el default y reproduce el aspecto previo del pack, de modo que las instalaciones
existentes no cambian al actualizar. El recorte de *Sólido* usa `ColorScheme.knockoutColor()` en
lugar de un blanco literal: en Dark Mode el cuerpo es claro y un detalle blanco sería invisible.

El diseño se guarda con la clave `mony_color_style` y, al no existir, se resuelve en `MULTI_COLOR`.
Solo aparece en Ajustes → Apariencia cuando el pack activo es Mony Color, y se previsualiza con la
paleta real del tema antes de elegirlo.

Un `tint` explícito o `MonyIconRole.STATE` nunca reemplaza la paleta del artwork, y los modos
Automático, Color principal y Personalizado siguen sin aplicarse a Mony Color.

Estados semánticos y controles estructurales (`Back`, `Check`, `Close`, `Completed`, `Delete`,
`Error`, `Warning`, `Info`, `Dropdown`, `ExpandMore`, `ExpandLess`, `AlertsDisabled`, `Restore`,
`ScanBarcode`, `ScanDocument`, `ScanPrice`, `TrendUp`, `TrendDown`, `TrendFlat`) no tienen
artwork propio: vuelven al fallback Material para que error, warning, éxito, selección y
deshabilitado sigan siendo coloreables. `Back` además conserva el auto-mirroring de Android.

El mapa vivo está en `MonyColorIconMappings.kt`. Para cambiar la ilustración de un significado:

1. Ajustar `ICONS` en `tools/iconpark/generate.mjs` si el glifo debe cambiar o agregarse.
2. Regenerar con
   `node tools/iconpark/generate.mjs --source <paquete>@iconify-json/icon-park/icons.json`.
3. Actualizar la entrada correspondiente en `MONY_COLOR_VECTORS`.
4. Extender las pruebas de resolución, fallback y representación.

El generador no tiene dependencias de npm y falla de forma explícita ante máscaras con recorte
real, viewports inesperados o iconos ausentes.

Si una ilustración Mony Color o un equivalente de otro pack no existe, el resolver devuelve el
`ImageVector` Material del mismo significado. El fallback ignora el color global monocromático de
Mony Color, pero respeta siempre un `tint` explícito o `MonyIconRole.STATE` para no perder estados
de error, advertencia, éxito, selección o deshabilitado. Un significado nuevo debe tener mapping
Material y mappings de cada pack; devolver `null` documenta que el fallback Material es intencional.

## Uso desde UI

- La UI solo renderiza `MonyIcon(...)`; no importa una librería gráfica ni un pack.
- Acciones, navegación, categorías y elementos decorativos usan `MonyIconRole.NORMAL`.
- Error, warning, success, selección y disabled usan `MonyIconRole.STATE` o `tint` explícito.
- `LocalIconography` distribuye pack y color global desde el tema; cada icono no lee preferencias.

## Agregar un pack

1. Verificar licencia, mantenimiento, compatibilidad Compose y tamaño.
2. Agregar el valor a `IconPack` sin renombrar valores ya persistidos.
3. Crear un resolver central y conectarlo desde `resolveOrNull`.
4. Mapear todos los significados o declarar el fallback Material con `null`.
5. Extender las pruebas de resolución, categorías, fallback, persistencia y preview.
