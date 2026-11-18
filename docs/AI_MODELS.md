# Modelos de IA de Mony

El catálogo admite cuatro opciones:

- **Gemini Nano** — acceso por Google AICore, solamente en dispositivos admitidos.
- **Qwen3 0.6B** — compacto y descargable con LiteRT-LM.
- **Qwen3 1.7B** — descargable con mayor uso de memoria.
- **Qwen2.5 1.5B Instruct** — alternativa descargable y verificada mediante archivo LiteRT-LM, tamaño y SHA-256.

Los modelos de Qwen se descargan mediante WorkManager. En Ajustes se muestra el estado de la descarga, porcentaje, bytes transferidos, errores y botones para pausar, reanudar o eliminar. La reanudación conserva el archivo parcial cuando el servidor admite HTTP Range. Las descargas siguen disponibles aunque cierres la pantalla.

No todos los teléfonos pueden ejecutar un modelo grande con fluidez. Gemini Nano depende de AICore; Qwen depende de memoria disponible, CPU y compatibilidad con LiteRT-LM. Al escoger un modelo no se garantiza su ejecución: se valida al prepararlo.

Existen otras familias LiteRT, incluidas Gemma 3, pero algunas requieren aceptar previamente términos o usar formatos/aceleradores especiales, así que no se ofrecen como descargas directas sin verificar las condiciones, el hash y los requisitos exactos.

El registro mediante voz sigue el flujo: reconocimiento → IA local → borrador → validación de campos → confirmación manual. Los modelos interpretan lenguaje natural en español, sin obligar a usar frases de ejemplo. La IA no guarda directamente transacciones ni inventa categorías. Si hay ambigüedad se pide aclaración.

## Verificación

CI ejecuta `./gradlew assembleDebug` y `./gradlew testDebugUnitTest`. La prueba instrumentada `QwenVoiceInterpretationInstrumentedTest` debe ejecutarse en Android con un modelo instalado para confirmar tipo, importe, categoría, fecha y nota con inferencia real. La compilación no demuestra precisión de Qwen en un dispositivo concreto.
