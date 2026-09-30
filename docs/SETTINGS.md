# Arquitectura de Configuración

`Configuración` funciona como un índice de destinos, no como una pantalla monolítica de controles.
La jerarquía actual es:

```text
Ajustes
├── Personalización  # tema, colores, iconos, formas, tipografía y fondo
├── Navegación       # módulos, etiquetas y transiciones
├── Finanzas         # presupuesto, ciclo, cierre y alertas
└── Categorías       # administración CRUD y límites
```

Cada destino es una ruta real de Navigation Compose bajo `settings/*`. Back vuelve siempre al
índice. No se añade búsqueda mientras el índice siga siendo pequeño y escaneable.

## Reglas

- Una configuración simple permanece inline dentro de su destino.
- Una configuración compleja con lista, formulario o CRUD recibe una subpantalla.
- La mayoría de flujos no debe superar dos niveles: Ajustes → destino.
- Dialog y BottomSheet se reservan para selección, edición o confirmación breve.
- No crear una card por opción. Agrupar filas relacionadas en una superficie con divisores.
- El índice muestra solo título, descripción breve, icono semántico y, cuando aporta valor, resumen.
- Las pantallas usan una sola lista desplazable, respetan insets y mantienen objetivos táctiles de 48dp.
- Toda preferencia persistente debe ofrecer estado reactivo, feedback apropiado y conservar valores existentes.
