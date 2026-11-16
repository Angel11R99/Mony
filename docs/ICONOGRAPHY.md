# Iconografía

La aplicación renderiza todos los iconos mediante `MonyIcon` y `MonyIconResolver`. Las pantallas no dependen directamente de una biblioteca de iconos. Los paquetes disponibles son Material, Lucide, Phosphor, MingCute, Flat Color Icons, Fluent Emoji Flat.

Los paquetes de ilustraciones a color conservan el color original de sus vectores y no reciben el tinte global. Si un significado no tiene ilustración en un paquete, se usa el icono Material correspondiente. Los demás paquetes siguen el modo de color automático, principal o personalizado.

## Licencias y fuentes

Los iconos se incluyen como subconjuntos locales de vectores Compose, por lo que la aplicación funciona sin conexión:

| Paquete | Fuente | Licencia |
| --- | --- | --- |
| MingCute | `@mingcute/svg` 3.1.0 | Apache-2.0 |
| Flat Color Icons | `@iconify-json/flat-color-icons` 1.2.3 (Icons8) | MIT |
| Fluent Emoji Flat | `@iconify-json/fluent-emoji-flat` 1.2.6 (Microsoft Fluent UI Emoji) | MIT |

Los avisos completos se encuentran en `app/src/main/assets/licenses/`.

## Regenerar subconjuntos

Extrae los paquetes npm en un directorio temporal y ejecuta:

```sh
node tools/icons/generate-packs.mjs \
  --mingcute <directorio-paquete-mingcute> \
  --flat <directorio-paquete-flat-color>/icons.json \
  --fluent-flat <directorio-paquete-fluent-emoji-flat>/icons.json \
```

La lista de glifos semánticos incluidos y sus conversiones SVG a `ImageVector` están en `tools/icons/generate-packs.mjs`. Después de regenerar, verifica los vectores generados y sus referencias en `MonyIconResolver.kt`.

