# Mony

<!-- APP_VERSION_START -->
[![Versión](https://img.shields.io/badge/versi%C3%B3n-v1.0.1-6750A4)](https://github.com/Angel11R99/Mony/releases/tag/v1.0.1)
[![Descargar](https://img.shields.io/badge/descargar-%C3%BAltima_versi%C3%B3n-6750A4)](https://github.com/Angel11R99/Mony/releases/latest)
<!-- APP_VERSION_END -->

Mony es una aplicación Android de finanzas personales diseñada para registrar y consultar tus movimientos con rapidez. Room mantiene los datos financieros localmente para que las funciones principales sigan disponibles sin conexión a Internet.

## Descargar

<!-- APP_DOWNLOAD_START -->
Descarga **Mony v1.0.1** desde su [release en GitHub](https://github.com/Angel11R99/Mony/releases/tag/v1.0.1) o consulta [todas las versiones disponibles](https://github.com/Angel11R99/Mony/releases).
<!-- APP_DOWNLOAD_END -->

## Funciones principales

- Registro, edición, duplicado y eliminación de ingresos y gastos.
- Presupuestos mensuales o quincenales con ciclos y cierre manual o automático.
- Historial con filtros, búsqueda y exportación local a CSV y PDF.
- Estadísticas por período y categoría, comparaciones y límites de gasto.
- Entradas fijas recurrentes y pagos o cobros pendientes con recordatorios.
- Metas de ahorro y seguimiento de su progreso.
- Listas de compra con productos, precios, descuentos, recargos y métodos de pago.
- Escaneo de códigos de barras y lectura de tickets; la consulta externa de productos es opcional.
- Categorías personalizables y límites de presupuesto por categoría.
- Apariencia clara, oscura o según el sistema, con colores configurables.
- Once widgets para balances, presupuesto, estadísticas, movimientos, accesos rápidos y otros resúmenes.
- Soporte para pesos dominicanos (`DOP`, `RD$`).

## Privacidad y funcionamiento sin conexión

Room es la fuente principal de los datos financieros y las funciones esenciales no requieren una cuenta, un servidor ni conexión a Internet. La consulta de productos por código de barras puede usar un servicio externo de forma opcional. Consulta la [Política de privacidad de Mony](PRIVACY_POLICY.md) para conocer los detalles.

## Arquitectura

La aplicación usa una arquitectura por capas similar a Clean Architecture con MVVM:

- **domain**: modelos y reglas de negocio en Kotlin puro.
- **data**: base de datos Room, mapeadores e implementaciones de repositorios.
- **presentation**: pantallas con Jetpack Compose y ViewModels.
- **navigation**: grafo de navegación principal.
- **ui**: tema y preferencias de apariencia.
- **widget**: widgets creados con Glance.
- **di**: inyección de dependencias con Hilt.
- **core**: utilidades compartidas, como el formato monetario.

## Tecnologías

- Kotlin y coroutines con Flow.
- Jetpack Compose y Material 3.
- Room.
- Hilt.
- Navigation Compose.
- WorkManager.
- Glance App Widgets.
- ML Kit y Google Code Scanner.

## Requisitos de desarrollo

- Android Studio compatible con Android Gradle Plugin 9.
- JDK 17 o una versión compatible configurada por Android Studio.
- Android SDK 36; la aplicación admite dispositivos desde Android 7.0 (`minSdk 24`).

## Compilación

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

En sistemas Unix:

```bash
./gradlew assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/` con un nombre como `Mony-v1.0.0-debug.apk`.

## Preparar la siguiente versión

La versión se administra en `version.properties`. Para incrementar automáticamente el parche (`1.0.0` → `1.0.1`), aumentar `versionCode`, actualizar este README y compilar el APK release, ejecuta:

```powershell
.\gradlew.bat buildNextRelease
```

El APK release se genera en `app/build/outputs/apk/release/`. Antes de publicarlo debes configurar una firma de producción; no publiques APK sin firmar ni claves privadas en el repositorio.

Para cambiar manualmente una versión mayor o menor, edita `VERSION_NAME` y aumenta `VERSION_CODE` en `version.properties`, y después ejecuta:

```powershell
.\gradlew.bat syncVersionDocumentation assembleRelease
```

## Contribuciones

Los reportes de errores, mejoras y contribuciones de código son bienvenidos y serán revisados por el propietario del proyecto. Lee [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir un issue o pull request.

## Licencia

Mony tiene código fuente disponible para inspección y contribuciones. Se permite modificarlo para uso personal o para contribuir al proyecto oficial, pero no redistribuirlo, renombrarlo, usarlo comercialmente ni publicar versiones derivadas sin autorización escrita. Consulta [LICENSE.md](LICENSE.md).

## Agradecimientos

- Inspirada en la necesidad de administrar finanzas personales de forma simple y sin conexión.
- Construida con prácticas modernas de desarrollo Android.
