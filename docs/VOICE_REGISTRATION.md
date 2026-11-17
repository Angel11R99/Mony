# Registro por voz

Mony usa `SpeechRecognizer` para capturar una frase y `NaturalLanguageTransactionEngine` para
convertir voz o texto en cambios de un borrador. El motor primario es local cuando se instala el
modelo; si no, usa un respaldo Kotlin con cobertura limitada. Ninguno envía transcripciones a
servidores. La captura prioriza el reconocedor en el dispositivo desde Android 12 (API 31) cuando
está disponible.

Si el dispositivo solo ofrece reconocimiento convencional, Mony avisa que el audio puede enviarse
al proveedor del servicio y que puede requerir internet. La persona debe aceptarlo antes de iniciar
la escucha. El diálogo permite marcar **No volver a mostrar**; esta autorización puede revocarse en
`Ajustes → Finanzas → Registro por voz`. El registro manual continúa disponible sin conexión.

## Comprensión local

La comprensión usa `NaturalLanguageTransactionEngine`. Con el modelo opcional instalado, el motor
principal ejecuta **Qwen3-0.6B INT4 no-think mediante LiteRT-LM 0.16.1**, solo en CPU. El modelo genera
un objeto JSON con intención, cambios de campo, categoría propuesta, fecha, nota o aclaración. La
respuesta se valida en Kotlin; el modelo no tiene acceso a repositorios ni puede guardar. Los campos
solo se aplican si el borrador visible sigue igual al iniciar la inferencia, y el guardado continúa
requiriendo la confirmación explícita ya existente.

La aplicación descarga el archivo tras pulsar **Instalar comprensión local**. Es una descarga de
347,251,840 bytes (aprox. 331 MiB), con SHA-256 verificado antes de instalarlo. Se usa una revisión
fijada del artefacto público de LiteRT Community, licencia Apache 2.0. El archivo no se incluye en el
APK. El primer acceso necesita Internet y espacio libre; después la inferencia funciona sin red.
La aplicación no envía transcripciones a un servicio remoto. La voz aún depende de la disponibilidad
y privacidad del servicio de reconocimiento del dispositivo, tal como se describe arriba.

La integración usa CPU y requiere ABI `arm64-v8a` o `x86_64`, al menos 6 GB de RAM declarada por el
dispositivo, que no esté marcado como dispositivo de baja memoria, y unos 400 MB libres para instalar
el archivo. Es un umbral conservador, no una garantía de velocidad o éxito: el benchmark publicado
para Qwen3-0.6B INT4 reporta picos cercanos a 2.7 GiB en CPU en algunos teléfonos. Al terminar cada
inferencia se cierra el runtime para liberar memoria. Si falta el modelo, el dispositivo no es
compatible, falla la descarga o falla la inferencia, se conserva el intérprete Kotlin anterior como
respaldo local limitado. El formulario manual sigue disponible en todos los casos.

La integración Kotlin compila con LiteRT-LM 0.16.1. Se eligió esta versión porque la 0.17+ publicada
compila con metadatos Kotlin 2.4 y el compilador de este proyecto no los admite; no se actualizó el
Kotlin/Hilt global solo para habilitar el runtime. Esto limita el conjunto de modelos compatibles:
se usa el archivo INT4 publicado en formato `.litertlm`, descrito para uso con LiteRT-LM y la API
Kotlin de Android.

Las pruebas JVM comprueban el intérprete de respaldo, el parser de control y el borrador; no cargan
el archivo de 347 MB ni miden precisión del LLM. No se publican métricas de
precisión inventadas. Hace falta una evaluación en dispositivos compatibles con frases separadas
del prompt de desarrollo antes de tratar la calidad semántica como calibrada. El modelo pequeño
puede confundir relaciones complejas, categorías cercanas o notas narrativas largas; la aclaración
y la revisión del formulario siguen siendo parte del flujo.

Se admiten formas conversacionales además de órdenes explícitas, por ejemplo:

```text
Registra/Agrega/Anota/Suma un gasto…
Registra/Agrega/Anota/Suma un ingreso…
Gasté…, compré… por…, me pagaron…, me depositaron…, me devolvieron…
```

El monto puede expresarse con dígitos o palabras, con pesos y centavos, incluidos multiplicadores
como `25 mil` y miles con coma como `1,250`. Los separadores numéricos ambiguos no se interpretan.
Las fechas admitidas son `hoy`, `ayer`, `anteayer`, `hace N días`,
`AAAA-MM-DD`, `DD/MM/AAAA` y `D de <mes> de AAAA`. Una fecha con mes pero sin año requiere
aclaración.

Las categorías se buscan solamente entre categorías activas del tipo indicado. Se normalizan
acentos, mayúsculas y espacios. Una coincidencia duplicada muestra opciones; una categoría
inexistente no se crea ni se sustituye por una coincidencia débil.

El modelo intenta conservar la narración libre como nota, incluso si está intercalada con monto,
categoría o fecha. Si el modelo no está disponible, el respaldo solo puede separar notas explícitas
con `nota`/`con nota` y algunas cláusulas introducidas por `porque` o `pero recuerda que`; no ofrece
comprensión libre equivalente. En ambos modos, el texto narrativo nunca autoriza guardar por sí solo.
En una frase completa, el guardado automático solo se
autoriza mediante una cláusula final separada por `y`, por ejemplo:

```text
… con nota taxi al trabajo y guárdalo
… y guarda el gasto
… y guarda el ingreso
```

`Guárdalo` también funciona como comando independiente en una escucha posterior. Por eso `nota
recordar guardar` permanece como texto y no guarda el movimiento.

## Dictado y texto

El texto escrito en **Describe el movimiento** y el resultado del reconocedor pasan por la misma
interfaz de comprensión asíncrona. Se envía el borrador actual (sin historial financiero) para
entender correcciones parciales; los campos no mencionados se mantienen. Las operaciones de voz de
control como guardar, cancelar y deshacer permanecen detrás de comandos deterministas y nunca se
delegan al modelo.

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
