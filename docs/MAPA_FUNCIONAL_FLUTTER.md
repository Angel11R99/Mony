# Mapa funcional y de interfaz de Mony

> Guía de paridad funcional para portar este proyecto a Flutter. Describe lo que la persona encuentra en la app, dónde se accede y qué información o filtros pertenecen a cada pantalla. No pretende replicar la implementación Kotlin/Compose.

## 1. Resumen de navegación

La navegación principal es una **barra flotante inferior** con hasta **8 módulos**. Los ocho destinos existen en la app, pero Ajustes → Navegación permite ocultar/mostrar cada destino. Orden base:

| Orden | Módulo | Qué resuelve |
|---|---|---|
| 1 | Inicio | Resumen del presupuesto/ciclo vigente o próximo, gasto por categoría y movimientos recientes; accesos a registrar movimientos e historial. |
| 2 | Fijos | Compromisos recurrentes que pueden registrarse como movimientos. |
| 3 | Recordatorios | Pagos/cobros pendientes, fechas, estados y alertas; permite registrar el movimiento al cumplirse. |
| 4 | Ahorros | Metas de ahorro y avance; las aportaciones se contabilizan como gastos asociados a la meta. |
| 5 | Lista | Listas de compras, productos y totales; al completar puede asociarse un gasto. |
| 6 | Estadísticas | Reporte financiero con filtros de período/ciclo y categoría. |
| 7 | Plan de ciclo | Planificación de gastos/ahorros por período presupuestario, abonos y plantillas. |
| 8 | Historial | Movimientos, filtros, edición, duplicación, eliminación, exportación y respaldo/restauración. |

La barra se centra cerca del borde inferior y flota sobre el contenido, dejando espacio inferior para no tapar listas ni botones. Cada módulo ocupa el mismo ancho. El seleccionado usa el color principal como fondo y texto/icono contrastante; los demás son transparentes, con contenido secundario y borde. Altura aproximada: 68 dp con nombres y 62 dp sin ellos. En Ajustes → Navegación se configura qué destinos aparecen, las etiquetas y su tamaño. Las rutas de detalle normalmente ocultan la barra y Back vuelve al origen. En pantallas principales, el encabezado queda arriba con el nombre del módulo; el acceso a Ajustes aparece como acción del encabezado en la mayoría de módulos. El contenido principal es desplazable y las acciones/listas quedan dentro del área entre encabezado y barra flotante.

### Rutas que no son pestañas

- **Añadir movimiento**: se abre con acciones de ingreso/gasto. El encabezado indica el tipo; el formulario presenta selección/búsqueda de categoría y campo de monto RD$. En “Detalles” aparecen fecha y nota. Voz y limpieza del borrador son acciones del formulario; salir con cambios pide confirmación. Guardar valida los datos y muestra confirmación.
- **Editar movimiento**: mismo formulario con datos existentes, accesible desde Historial.
- **Voz**: variante de alta para reconocimiento de voz; el usuario revisa/acepta el resultado.
- **Detalle de lista**: desde Lista o Recordatorios; gestión de artículos, cantidades/precios, total y compra.
- **Plantillas del Plan de ciclo**: subpantalla para administrar plantillas reutilizables.
- **Ajustes**: índice accesible desde botones en módulos y desde Inicio; contiene cuatro destinos.

## 2. Módulos principales

### Inicio

Primer destino y pantalla inicial. Muestra presupuesto y período seleccionado, fechas del ciclo, ingresos, gastos, saldo/disponible, progreso de presupuesto, gasto agrupado por categoría y movimientos recientes. Incluye acciones de registrar ingreso/gasto, ir a Historial y Ajustes, consultar historial de ciclos y cerrar ciclo cuando corresponde.

El selector consulta **ciclo actual** o **próximo ciclo**; la vista elegida puede fijarse como preferida. No es un rango arbitrario: las fechas dependen de Finanzas. Contemplar estados de presupuesto sin configurar, sin movimientos, ciclo cerrado y confirmación de cierre.

### Fijos

Lista de plantillas de compromisos recurrentes con descripción, importe, categoría, frecuencia/programación y estado. El filtro visible es por **tipo: gastos o ingresos**, con conteos; no asumir filtro de periodicidad/estado en esta pantalla. Permite crear/editar/eliminar plantilla y registrar una ocurrencia con fecha elegida como movimiento real. La programación automática tiene su propio diálogo con fecha/hora aproximada y controles de periodicidad; hay controles para publicar una entrada y expandir/ver menos detalles. Diferenciar plantilla de movimiento ya registrado y evitar doble registro.

### Recordatorios

Pagos por realizar y cobros por recibir, con descripción, monto, categoría, fecha de vencimiento, estado/comentario y hora de alerta opcional. Permite crear, editar, ver detalle, eliminar y convertir en ingreso/gasto; algunos pueden abrir una lista relacionada. Filtros: **tipo** (pagos/cobros), **período/fecha de vencimiento** y **estado**. Hay hoja de filtros, resumen de selección y estado vacío con acción de crear. Las alertas son notificaciones locales.

### Ahorros

Metas con nombre, objetivo monetario y progreso/acumulado. Permite crear, editar, eliminar y registrar aportaciones. Cada aporte progresa la meta y también se refleja financieramente como gasto asociado; no mantenerlo como un saldo bancario separado. La parte superior tiene filtro de **estado: Activas / Completadas** y búsqueda por nombre; al consultar completadas, el alta de meta no se muestra. Se puede finalizar una meta activa y reabrir una completada, ambas con confirmación. El formulario de aporte muestra desglose aplicado a la meta y excedente cuando corresponde. El tamaño de las tarjetas de metas también es configurable como preferencia de presentación.

### Lista

La pantalla principal muestra listas activas y completadas. Incluye búsqueda por nombre y filtro **Todas / En compra (activas) / Completadas**. Permite crear, abrir, duplicar, completar/reabrir y eliminar. Eliminar confirma; reabrir una compra completada revierte el gasto que se creó al completar.

El detalle muestra nombre/presupuesto de lista, sección **Productos**, sección **Ajustes** (impuestos, descuentos u otros importes que suman o restan), subtotal, ajustes, total y disponible/exceso frente al presupuesto. Cada producto lleva cantidad, estimado por unidad, precio real por unidad, estado comprado/pendiente y código de barras si existe. Permite añadir/editar/quitar producto y ajuste, marcar comprado y finalizar. La fecha de compra puede elegirse al completar; si un código de barras coincide posiblemente se solicita confirmar vinculación con el producto reconocido. La app puede consultar catálogo remoto para ayudar a identificar artículos, pero la operación normal de listas sigue offline.

### Estadísticas

Reporte del período: comparativas/tendencias, ingresos frente a gastos, balance, cantidad de movimientos, gasto promedio y desglose por categoría. El resumen debe identificar claramente el período.

Filtros: período (**Ciclo actual, Este mes, Este año, Todo, Personalizado**); selector adicional “Mis ciclos” cuando existen ciclos históricos; fecha inicial/final para rango manual; categoría (Todas o una concreta). El cambio recalcula resumen y desgloses. En pantalla, botón/resumen de filtros precede al reporte; debajo aparecen tarjetas de balance y comparativa, tendencias, categorías y métricas. Mostrar estado vacío si el rango no contiene movimientos.

### Plan de ciclo

Plan por período presupuestario, con presupuesto de referencia y renglones de gasto o ahorro: nombre, categoría, importe planificado, pagos acumulados y pendientes. Permite crear/editar/eliminar planes e ítems, registrar abonos, cerrar y reabrir; acceso a **Plantillas**. Selector de período: anterior, actual y siguiente.

El período se obtiene del calendario financiero (aperturas configurables; no asumir 1–15). En ciclos configurados como mensuales se presenta el período mensual. Mostrar abierto/cerrado y planificado/pagado/pendiente.

Reglas: planificar no crea transacciones; el presupuesto es referencia. El abono sí crea un movimiento y un registro de pago; ahorro se contabiliza como gasto asociado a meta. No permitir borrar plan/ítem con abonos. Reabrir requiere acción explícita. Plantillas se copian como instantáneas y no modifican planes existentes.

### Historial

Lista de movimientos con fecha, tipo, categoría, monto y nota/detalles; resumen de cantidad e importes filtrados. Permite ver detalle, editar, duplicar y eliminar con confirmación. En el encabezado están ajustes y las acciones de exportar/respaldar según menú de acciones. Aquí también están exportación y respaldo/restauración.

Filtros: **tipo** (Todos/Gastos/Ingresos), **ciclo** (Todos los ciclos, actual, anterior e históricos), **categoría** (selector con búsqueda y opciones acordes al tipo), **fechas desde/hasta** con opción de quitar cada límite y **orden** (Más recientes, Más antiguos, Mayor monto, Menor monto, Categoría A–Z, Categoría Z–A). Se muestra resumen de filtros antes de lista y resumen del resultado con número de movimientos, ingreso y gasto. Si no hay resultados se explica. Exportar permite formatos disponibles (CSV/Excel/PDF) y resultados filtrados o todo el historial. Restaurar presenta vista previa y confirmación; respaldo incluye movimientos, fijos, pendientes, listas, metas, ciclos, planes/abonos, categorías y configuración cubierta por el formato.

### Formulario de movimiento (alta, edición y voz)

Es una pantalla de tarea, no una pestaña. En la parte superior se distingue el tipo **Gasto/Ingreso**, con importe como dato principal; después se busca/selecciona categoría filtrada por el tipo. “Detalles” contiene **Fecha** y **Nota**. Alta por voz añade botón/estado de escucha, texto reconocido y badge del tipo (por identificar, por confirmar, gasto o ingreso); el resultado es editable antes de guardar. Si la frase sugiere otro tipo, se pide confirmación para cambiar. Si el reconocimiento convencional requiere servicio remoto, se explica y se puede continuar manualmente. Limpiar y salir con borrador solicitan confirmación.

### Plantillas de Fijos

La pantalla llamada Fijos administra en realidad plantillas reutilizables de transacciones periódicas. El editor incluye descripción, tipo gasto/ingreso, monto, categoría con búsqueda, plantilla activa y opciones de programación. Una plantilla inactiva deja de estar disponible para agregar; eliminar la plantilla no borra los movimientos ya creados.

### Plantillas de Plan de ciclo

Subpantalla de CRUD de conceptos reutilizables, con descripción, tipo gasto/ahorro, monto planificado, categoría, meta de ahorro cuando es ahorro y nota opcional. Seleccionar categorías/montos para el plan no registra pagos.

## 3. Configuración

El índice “Ajustes” presenta “¿Qué quieres configurar?” y cuatro filas agrupadas en una superficie. Cada una navega a pantalla real con encabezado y Back; no es una lista monolítica de controles.

### Personalización

Selector interno de secciones que muestra solo controles relacionados:

1. **Colores/Tema**: tema del sistema, claro u oscuro. Color principal para botones, selección, ingresos y elementos destacados. Color secundario/acento para gastos, alertas y acciones que requieren atención. Selector de colores, vista previa en vivo y restablecer apariencia.
2. **Estilo**: geometría de botones, tarjetas y chips; familia tipográfica global; decoración de fondo (ninguna, médica o gatos) y su intensidad cuando aplica. Hay vistas previas de forma/fondo y sugerencias de combinación de fuente.
3. **Iconos**: paquete Material, Lucide, Phosphor, IconPark o Mony Color (según selector); estilo específico para Mony Color; modo de color de iconos y selector personalizado si se elige ese modo. Vista previa. La preferencia es global.

Los cambios se reflejan de inmediato y persisten; la apariencia de widgets debe acompañar el cambio.

### Navegación

- Mostrar/ocultar cada uno de los ocho módulos en la barra.
- Mostrar/ocultar nombres bajo los iconos.
- Tamaño de texto de etiquetas (opciones seleccionables).
- Transición entre módulos: ninguna, desvanecer, deslizar o escala.
- Vista previa de la barra según configuración.

Controla la presentación/navegación, no elimina módulos ni datos.

### Finanzas

- **Ciclo financiero:** periodicidad mensual o quincenal y definición de períodos/aperturas y cierres. Puede haber varios tramos; agregar período y editar días de apertura/cierre.
- **Cierre automático:** activar/desactivar y elegir hora. También existe cierre manual desde Inicio.
- **Presupuesto:** monto administrado en cada período, expresado en DOP.
- **Alertas de presupuesto:** preferencias de aviso de uso/límite.
- **Registro por voz:** control de avisos convencionales del reconocimiento.

No fijar días quincenales en Flutter: Inicio, Estadísticas, Historial y Plan de ciclo comparten el calendario configurado. Modificarlo afecta sus rangos.

### Categorías

CRUD de categorías de ingresos y gastos. En la pantalla se cambia entre listas **Gastos** e **Ingresos**, con conteo; cada lista muestra categorías, acción de crear/editar/borrar. El editor contiene nombre, tipo, icono y color/estilo visual. El selector de icono es una tarea contextual. Crear, editar y eliminar. Alimenta formularios, filtros, Estadísticas, Fijos, Recordatorios y Plan. No permitir eliminación de categoría en uso y mostrar motivo entendible.

## 4. Widgets de pantalla de inicio

Son piezas opcionales que el usuario añade al launcher del teléfono; no son pantallas de la app ni pestañas. El repositorio contiene **12**:

| Widget | Qué muestra/permite |
|---|---|
| Finanzas (`FinanceWidget`) | Tarjeta/resumen financiero general y entrada a la app. |
| Progreso del presupuesto | Uso del presupuesto vigente. |
| Ingresos y gastos | Comparación resumida de entradas y salidas. |
| Movimientos recientes | Últimos movimientos locales. |
| Estadísticas | Resumen estadístico compacto. |
| Acceso rápido | Atajos para acciones financieras. |
| Registro por voz | Acceso directo a captura por voz. |
| Recordatorios pendientes | Pendientes/avisos próximos. |
| Compromisos fijos | Entradas fijas relevantes/próximas. |
| Gasto diario | Gasto del día. |
| Límites por categoría | Progreso de límites por categoría. |
| Metas de ahorro | Metas y avance. |

Incluyen previews y fondo para claro/oscuro, y formato RD$. Deben representar datos reales y refrescar tras movimientos, presupuesto/apariencia, publicación de fijos/pendientes, cierre de ciclos y registro/reversión de abonos. Replicar sus deep links/acciones, en especial alta de ingreso/gasto y voz.

## 5. Estructura funcional interna para organizar la versión Flutter

Organiza por **función de producto** (features), no como una copia de los nombres de paquetes Kotlin. Un mapa entendible podría ser:

```text
Aplicación
├── Navegación raíz
│   ├── Barra inferior y preferencias de módulos
│   ├── Inicio
│   ├── Fijos
│   ├── Recordatorios
│   ├── Ahorros
│   ├── Listas de compra
│   ├── Estadísticas
│   ├── Plan de ciclo
│   └── Historial
├── Flujos compartidos
│   ├── Alta/edición de movimiento y voz
│   ├── Selector de categoría
│   ├── Detalle de movimiento
│   └── Confirmaciones, mensajes y selección de fecha/hora
├── Ajustes
│   ├── Índice
│   ├── Personalización
│   ├── Navegación
│   ├── Finanzas y alertas
│   └── Categorías
└── Servicios compartidos de producto
    ├── Datos financieros locales
    ├── Reglas de ciclos y dinero
    ├── Exportar/importar respaldo
    ├── Notificaciones y tareas recurrentes
    └── Widgets / accesos directos
```
La barra raíz contiene solo los 8 destinos principales. Formularios y detalles se apilan como rutas secundarias y conservan contexto para volver al módulo que los abrió. Ajustes es otra ruta, y sus cuatro secciones son subrutas de Ajustes. Categoría, fecha, monto y feedback deben reutilizarse visualmente donde el flujo es el mismo; cada módulo conserva sus propias acciones y filtros. La información financiera debe provenir de una fuente local común para que un registro actualice todas las pantallas dependientes.

## 6. Reglas transversales para Flutter

- **Idioma:** interfaz y feedback en español.
- **Moneda:** peso dominicano (DOP), presentación `RD$`; guardar montos con precisión monetaria, no usar coma flotante como verdad financiera.
- **Offline-first:** movimientos, ciclos, reportes locales, ajustes, fijos, recordatorios, metas, planes y widgets siguen funcionando sin Internet. Catálogo externo es complementario.
- **Registro:** flujos separados de ingreso/gasto, categorías coherentes, monto positivo, validación visible y bloqueo de doble envío.
- **Feedback:** guardar, borrar, cerrar ciclo y registrar abono deben confirmar éxito o explicar error de forma recuperable.
- **Actualización transversal:** datos modificados actualizan Inicio, Historial, Estadísticas, módulos relacionados y widgets; mantener una fuente de verdad local.
- **Fechas:** ciclos configurables y compartidos; evitar asumir fechas quincenales universales.
- **Accesibilidad/tema:** claro/oscuro, texto legible, áreas táctiles suficientes, contraste y estados que no dependan solo del color.

## 7. Árbol de pantallas sugerido para el nav de Flutter

```text
App
├── Barra inferior configurable (8 destinos)
│   ├── Inicio
│   ├── Fijos
│   ├── Recordatorios
│   ├── Ahorros
│   ├── Lista
│   ├── Estadísticas
│   ├── Plan de ciclo
│   └── Historial
├── Formularios/rutas secundarias
│   ├── Añadir ingreso/gasto
│   ├── Editar movimiento
│   ├── Registro por voz
│   ├── Detalle de lista
│   └── Plantillas del Plan de ciclo
└── Ajustes (índice)
    ├── Personalización
    ├── Navegación
    ├── Finanzas
    └── Categorías
```

