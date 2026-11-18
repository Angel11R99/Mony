# Registro inteligente por voz

Desde Inicio, el usuario puede dictar un gasto o ingreso sin seleccionar previamente el tipo. Android transcribe el audio y Google Gemini Nano, mediante ML Kit GenAI Prompt y AICore, interpreta el texto.

La arquitectura es: voz → transcripción → Gemini Nano → propuesta estructurada → validaciones Kotlin → vista previa → confirmación y guardado.

El motor activo es `GoogleVoiceTransactionEngine`. El antiguo intérprete de frases y Qwen3 no se usan como respaldo de producción. Mony no inventa importes ni categorías, y solo admite categorías activas existentes. Si falta información, se conserva un borrador revisable.

Gemini Nano requiere un teléfono compatible con AICore. Su disponibilidad se comprueba antes de la inferencia. Si no está disponible, el registro manual permanece activo y no se presenta una respuesta de IA simulada.

El reconocimiento de voz es independiente de la IA y puede requerir el servicio convencional de Android; el audio podría enviarse a ese proveedor, previa aceptación del usuario.

Quedan pendientes pruebas instrumentadas con dispositivo físico compatible, especialmente para español dominicano, clasificaciones ambiguas y guardado correcto.