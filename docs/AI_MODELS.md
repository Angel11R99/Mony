# IA por voz — Gemini Nano

Mony utiliza Gemini Nano mediante Google ML Kit GenAI Prompt y Android AICore para interpretar las transcripciones de voz localmente. No utiliza API remota de Gemini.

La preparación consulta `Generation.getClient().checkStatus()` y permite descargar el modelo mediante AICore cuando Android indica que es descargable. No se usan los modelos Qwen3 de LiteRT-LM ni su intérprete de respaldo.

**Compatibilidad:** Android 8 (API 26) o superior y un dispositivo con AICore y Gemini Nano habilitados. La compatibilidad no está garantizada en todos los teléfonos. Los dispositivos con bootloader desbloqueado pueden no tener acceso a esta API.

El modelo genera una propuesta JSON. Kotlin verifica tipo, monto, fecha y las categorías activas existentes. El usuario revisa el resultado antes de guardar. Si Gemini Nano no está disponible se conserva el registro manual, sin simular inferencia.

La voz puede depender del proveedor convencional de reconocimiento de Android y requerir consentimiento. La inferencia de Gemini Nano, cuando funciona, es local.

**Verificación pendiente:** compilación y pruebas instrumentadas en dispositivo compatible; no hay pruebas reales disponibles sin el teléfono conectado.