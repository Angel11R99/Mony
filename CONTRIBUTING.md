# Contribuir a Mony

Las contribuciones a Mony son bienvenidas. Puedes proponer correcciones, mejoras y nuevas funciones mediante el repositorio oficial.

Todo cambio pasa por revisión del titular del proyecto. Enviar una contribución no garantiza que sea aceptada o publicada.

Al participar debes respetar la [licencia de Mony](LICENSE.md). Puedes crear un fork y modificar el código para preparar tu contribución, pero no redistribuir Mony, publicar versiones derivadas ni presentar el proyecto como propio.

## Formas de contribuir

- Reportar fallos reproducibles.
- Corregir errores.
- Mejorar pruebas y documentación.
- Proponer mejoras de accesibilidad o experiencia de usuario.
- Implementar funciones previamente discutidas y aprobadas.
- Revisar código y aportar observaciones técnicas.

Para cambios grandes, abre primero una incidencia y espera confirmación antes de desarrollar la solución.

## Preparación del proyecto

1. Crea un fork del repositorio.
2. Crea una rama enfocada en un único cambio.
3. Abre el proyecto en Android Studio y espera la sincronización de Gradle.
4. Compila el proyecto antes de comenzar.

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

En sistemas Unix:

```bash
./gradlew assembleDebug
```

## Reglas de desarrollo

- Inspecciona la implementación existente antes de modificarla.
- Mantén cada contribución enfocada y evita refactorizaciones no relacionadas.
- Conserva la arquitectura actual basada en Kotlin, Compose, ViewModel, repositorios, Room y Hilt.
- Mantén las funciones financieras principales disponibles sin conexión.
- Escribe todos los textos visibles en español.
- Utiliza pesos dominicanos (DOP / RD$) y conserva los montos en centavos enteros.
- Utiliza el formateador monetario compartido y las utilidades de fecha existentes.
- No accedas a DAOs directamente desde pantallas o ViewModels.
- No agregues dependencias innecesarias, publicidad, seguimiento ni servicios remotos no aprobados.
- Mantén compatibilidad con los temas claro, oscuro y del sistema.
- Proporciona validación y mensajes comprensibles para acciones importantes.
- Actualiza los widgets cuando cambien datos que estos muestran.

## Datos y migraciones

Los datos financieros existentes deben conservarse.

Todo cambio en el esquema de Room debe incluir:

1. Incremento de la versión de la base de datos.
2. Migración explícita y no destructiva.
3. Registro de la migración.
4. Nuevo esquema JSON.
5. Pruebas o verificación de conservación de datos.

No utilices `fallbackToDestructiveMigration()`.

## Pruebas

Ejecuta las pruebas unitarias:

```powershell
.\gradlew.bat testDebugUnitTest
```

Y verifica la compilación:

```powershell
.\gradlew.bat assembleDebug
```

En sistemas Unix sustituye `.\gradlew.bat` por `./gradlew`.

Los cambios de lógica de negocio, presupuesto, fechas, validación o cálculos deben incluir pruebas relevantes cuando sea práctico.

## Pull requests

Cada pull request debe incluir:

- Descripción del problema o mejora.
- Explicación de la solución.
- Pasos para verificarla.
- Pruebas ejecutadas.
- Capturas o grabaciones para cambios visuales.
- Consideraciones de migración cuando correspondan.

El titular puede solicitar cambios, rechazar la propuesta o adaptarla antes de incorporarla. Solo el código revisado y aceptado pasa a formar parte de la versión oficial.

## Reportar fallos

Incluye:

- Versión de Mony y de Android.
- Dispositivo o emulador utilizado.
- Pasos exactos para reproducir el problema.
- Resultado esperado y resultado observado.
- Capturas o mensajes relevantes.

No publiques movimientos financieros, archivos de respaldo, credenciales ni información personal.

## Derechos sobre las contribuciones

Al enviar código, documentación o recursos confirmas que son de tu autoría o que tienes permiso para aportarlos. También aceptas las condiciones de contribución indicadas en [LICENSE.md](LICENSE.md), que permiten integrar y distribuir tu aporte como parte de Mony.
