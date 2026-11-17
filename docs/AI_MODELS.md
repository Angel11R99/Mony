# Modelos locales de comprensión

## Selección

Mony conserva el identificador elegido en `language_model_preferences`. Al abrir un formulario,
primero intenta preparar ese modelo si el archivo está instalado. Si falta o no puede inicializarse,
prueba los demás archivos instalados y guarda el primer modelo compatible. El orden predeterminado,
cuando no existe una selección utilizable, es Qwen3 0.6B y luego Qwen3 1.7B para preferir el uso
menor de memoria. Mony nunca descarga un modelo automáticamente.

## Estados del archivo y del motor

Los archivos se guardan en `filesDir/language_models/`. El catálogo comprueba su tamaño esperado.
Antes de declarar un modelo listo para el formulario, Mony verifica el SHA-256 y llama a
`Engine.initialize()` con LiteRT-LM en CPU. Una inicialización fallida no se presenta como ausencia
de archivo: se conserva como error de integridad, compatibilidad o memoria. La primera inferencia
también se maneja por separado y muestra un error recuperable si el motor no logra procesar el texto.

La selección activa, la presencia del archivo y la preparación del motor son estados distintos.
El motor se libera cuando el formulario pasa a segundo plano y se prepara de nuevo al regresar.

## Catálogo actual y verificación

El catálogo contiene Qwen3 0.6B y Qwen3 1.7B. La prueba instrumentada ejecuta una inferencia real
del Qwen3 0.6B instalado en un Pixel 9 Pro AVD y comprueba el monto y la categoría para un gasto y
un ingreso. La compatibilidad y el rendimiento de Qwen3 1.7B deben confirmarse en el dispositivo
en el que se descargue; su tamaño y uso de memoria son mayores.

El reconocimiento de voz solamente transcribe. La transcripción y el texto escrito pasan por el
mismo motor semántico; la entrada escrita no solicita permiso de micrófono.
