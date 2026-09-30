# Iconografía de Mony

La UI solicita significados mediante `MonyIcon`; nunca selecciona directamente una librería
o un recurso. `MonyIconResolver` decide el asset según `IconPack` y aplica fallback Material de
forma centralizada.

## Tipos de asset

- `MonyIconAsset.Tintable`: `ImageVector` monocromático para Material, Lucide y Phosphor.
- `MonyIconAsset.Multicolor`: ilustración vectorial propia renderizada por
  `MonyColorIconPainter` sin filtro de color global.

## Mony Color

Mony Color es un pack original del proyecto, sin assets ni dependencias externas. Sus dibujos
usan primitivas vectoriales escalables en un lienzo normalizado de 24 x 24, rellenos pastel y un
contorno obtenido de `MaterialTheme.colorScheme.onSurface` para conservar legibilidad en Light y
Dark Mode. Los modos Automático, Color principal y Personalizado no reemplazan sus rellenos.

Para agregar una ilustración:

1. Agregar el significado a `MonyIcon` si todavía no existe.
2. Incluirlo en `MonyColorIconPainter.supported`.
3. Dibujar el caso con la paleta controlada de `MonyColorPalette`.
4. Agregar una prueba de resolución y representación.

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
