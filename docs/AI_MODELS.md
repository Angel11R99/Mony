# Modelos de IA de Mony

El usuario puede elegir entre:
- **Gemini Nano**, mediante Google ML Kit GenAI Prompt y Android AICore, si el dispositivo lo admite.
- **Qwen3 0.6B**, modelo descargable y ejecutado localmente mediante Google LiteRT-LM.
- **Qwen3 1.7B**, modelo descargable de mayor consumo, también mediante LiteRT-LM.

El administrador `VoiceModelCatalog` expone el catálogo y selecciona el motor disponible. Si AICore no está disponible, un modelo Qwen ya instalado puede ofrecer una alternativa. Los modelos Qwen requieren una descarga inicial, espacio libre y recursos suficientes. Ningún motor garantiza precisión perfecta; las respuestas se validan en Kotlin.

La inferencia es local. La captura de voz de Android puede depender de servicios convencionales que requieran consentimiento, y debe tratarse por separado.

La IA produce un borrador de gasto o ingreso. Mony verifica importe, fecha y categoría activa antes de presentar la vista previa y requiere confirmación para guardar. Los fallos de inferencia no activan el antiguo intérprete por reglas.

## Pruebas

`./gradlew assembleDebug` y `./gradlew testDebugUnitTest`. Ejecutar pruebas reales en Android con los modelos instalados para medir precisión, latencia y memoria. Las pruebas JVM y la compilación no demuestran que Gemini Nano esté disponible en todos los teléfonos.