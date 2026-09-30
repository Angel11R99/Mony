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

El artwork conserva su paleta original (`#2F88FF`, `#43CCF8`, `#fff`). Solo el trazo `#000` se
sustituye por `MaterialTheme.colorScheme.onSurface`, de modo que el marco del icono mantiene
contraste en Light y Dark Mode sin verse afectado por los modos Automático, Color principal o
Personalizado. Un `tint` explícito no reemplaza esos rellenos.

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
