# Registro por voz

Desde Inicio, el usuario pulsa «Registrar con voz» y describe un gasto o ingreso sin seleccionar el tipo. Android convierte voz en texto, y el motor local configurado identifica el tipo, importe, fecha, nota y categoría activa. El formulario muestra un borrador editable antes del guardado.

## Motores

El catálogo de modelos permite elegir **Gemini Nano (AICore)** o los modelos descargables **Qwen3 0.6B** y **Qwen3 1.7B (LiteRT-LM)**. Gemini Nano no está disponible en todos los dispositivos. Qwen tampoco está garantizado en teléfonos con memoria insuficiente.

Un fallo del modelo no desencadena interpretaciones basadas en frases fijas ni crea transacciones de manera automática. El registro manual continúa disponible.

El sistema debe respetar el consentimiento de reconocimiento convencional de Android, ya que el audio puede enviarse al proveedor del servicio de voz. La inferencia mediante estos motores es local.

Quedan pendientes pruebas instrumentadas de inferencia real con teléfonos compatibles y categorías personalizadas. Las capacidades del modelo dependen del idioma, recursos del teléfono y la calidad del prompt.