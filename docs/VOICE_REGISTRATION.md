# Registro por voz

Mony usa `SpeechRecognizer` para capturar una frase y un intérprete Kotlin local para convertirla
en un borrador. El intérprete no envía datos a servidores ni necesita Android. La captura prioriza
el reconocedor en el dispositivo desde Android 12 (API 31) cuando está disponible.

Si el dispositivo solo ofrece reconocimiento convencional, Mony avisa que el audio puede enviarse
al proveedor del servicio y que puede requerir internet. La persona debe aceptarlo antes de iniciar
la escucha. El diálogo permite marcar **No volver a mostrar**; esta autorización puede revocarse en
`Ajustes → Finanzas → Registro por voz`. El registro manual continúa disponible sin conexión.

## Gramática

Cada escucha procesa como máximo un movimiento. Se admiten estas formas de creación:

```text
Registra/Agrega/Anota/Suma un gasto…
Registra/Agrega/Anota/Suma un ingreso…
Gasté…
Recibí…
```

El monto puede expresarse con dígitos o palabras, con pesos y centavos. Los separadores numéricos
ambiguos no se interpretan. Las fechas admitidas son `hoy`, `ayer`, `anteayer`, `hace N días`,
`AAAA-MM-DD`, `DD/MM/AAAA` y `D de <mes> de AAAA`. Una fecha con mes pero sin año requiere
aclaración.

Las categorías se buscan solamente entre categorías activas del tipo indicado. Se normalizan
acentos, mayúsculas y espacios. Una coincidencia duplicada muestra opciones; una categoría
inexistente no se crea ni se sustituye por una coincidencia débil.

La nota comienza con `nota` o `con nota`. Las palabras que aparezcan dentro de la nota no se
interpretan como tipo, categoría ni comando. En una frase completa, el guardado automático solo se
autoriza mediante una cláusula final separada por `y`, por ejemplo:

```text
… con nota taxi al trabajo y guárdalo
… y guarda el gasto
… y guarda el ingreso
```

`Guárdalo` también funciona como comando independiente en una escucha posterior. Por eso `nota
recordar guardar` permanece como texto y no guarda el movimiento.

## Correcciones y control

```text
Cambia el monto a…
Selecciona/Cambia la categoría a…
Pon la fecha de…
Cambia/Pon la nota…
Agrega a la nota…
Borra la nota
Muéstrame las categorías
Revisa el movimiento
¿Qué falta para guardar?
Deshaz el último cambio
Cancelar dictado
Limpia el formulario
Volver/Salir sin guardar
La fuente es…
Confirma la fuente
La primera/La segunda/La tercera/La cuarta
```

La intención de guardar pertenece al borrador actual. Se conserva mientras se resuelven campos o
financiamiento, y se elimina al cancelar o limpiar. El botón táctil Guardar permite completar el
mismo flujo sin voz.

## Verificación manual pendiente

Estas comprobaciones requieren un dispositivo o emulador con servicios de reconocimiento y no se
pueden certificar mediante pruebas JVM:

1. Añadir el widget **Registrar por voz**, pulsarlo con la app cerrada y confirmar una sola escucha.
2. Repetir con permiso sin conceder, concederlo y confirmar que la escucha continúa.
3. Probar un dispositivo con reconocedor local y otro que solo tenga reconocedor convencional.
4. Denegar permanentemente el permiso y verificar que el formulario manual continúa utilizable.
5. Rotar o recrear la Activity con un borrador y con el diálogo de financiamiento visible.
6. Probar silencio, idioma no disponible, error de red, detener y abandonar la pantalla.
7. Dictar un gasto que exceda el disponible, indicar la fuente y confirmar que existe una sola
   transacción con su financiamiento atómico.
