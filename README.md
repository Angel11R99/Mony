<p align="center">
  <img src="docs/assets/mony-banner.svg" width="100%" alt="Mony — Tus finanzas, en tu órbita. Finanzas personales para Android." />
</p>

<h1 align="center">Tu historial. Tu ritmo. Tu Mony.</h1>

<p align="center">
  Registra hoy, entiende tus hábitos y prepara lo que viene.<br />
  <strong>Finanzas personales para Android, con tus datos en tu dispositivo.</strong>
</p>

<!-- APP_VERSION_START -->
  <!-- 
  <p align="center">
    <a href="https://github.com/Angel11R99/Mony/releases/tag/v1.0.6"><img src="https://img.shields.io/badge/versi%C3%B3n-v1.0.6-6750A4" alt="Versión de Mony" /></a>
    <a href="https://github.com/Angel11R99/Mony/releases/latest"><img src="https://img.shields.io/badge/descargar-%C3%BAltima_versi%C3%B3n-6750A4" alt="Descargar última versión" /></a>
  </p> 
  -->
<!-- APP_VERSION_END -->

<p align="center">
  <a href="https://github.com/Angel11R99/Mony/releases/latest"><img src="https://img.shields.io/badge/Descargar_Mony-6750A4?style=for-the-badge&amp;logo=android&amp;logoColor=white" alt="Descargar Mony para Android" /></a>
  <a href=".github/CONTRIBUTING.md"><img src="https://img.shields.io/badge/Contribuir-24112F?style=for-the-badge&amp;logo=github&amp;logoColor=white" alt="Contribuir al proyecto" /></a>
  <a href="https://github.com/Angel11R99/Mony/releases/tag/v1.0.6"><img src="https://img.shields.io/badge/Versi%C3%B3n-v1.0.6-6750A4?style=for-the-badge&amp;logo=android&amp;logoColor=white" alt="Versión de Mony" /></a>
</p>

<p align="center">
  <a href="#01--tu-día-a-día">Funciones</a> ·
  <a href="#02--tus-datos-contigo">Privacidad</a> ·
  <a href="#03--por-dentro">Desarrollo</a> ·
  <a href="#04--forma-parte">Contribuir</a>
</p>

## `01` — Tu día a día

| Registra | Organiza | Visualiza |
| :--- | :--- | :--- |
| Ingresos y gastos: crea, edita y duplica movimientos. | Presupuestos mensuales o por ciclos, con cierre manual o automático. | Estadísticas por período y categoría, comparaciones y límites de gasto. |
| Listas de compra con precios, descuentos, recargos y métodos de pago. | Entradas recurrentes, pagos y cobros pendientes con recordatorios. | Historial con búsqueda, filtros y exportación a PDF, Excel y CSV. |
| Códigos de barras y lectura de tickets. | Metas de ahorro y categorías personalizables. | Once widgets para consultar tus finanzas y acceder a acciones rápidas. |

<p align="center"><strong>RD$ / DOP</strong> · Tema claro, oscuro o del sistema · Colores configurables</p>

## `02` — Tus datos, contigo

> [!NOTE]
> Las funciones financieras esenciales funcionan sin Internet, sin cuenta y sin servidor. Room mantiene los datos financieros localmente.

La consulta externa de productos por código de barras es opcional. Más detalles en la [política de privacidad](docs/PRIVACY_POLICY.md).

<!-- APP_DOWNLOAD_START -->
Descarga **Mony v1.0.6** desde su [release en GitHub](https://github.com/Angel11R99/Mony/releases/tag/v1.0.6) o consulta [todas las versiones disponibles](https://github.com/Angel11R99/Mony/releases).
<!-- APP_DOWNLOAD_END -->

## `03` — Por dentro

**Kotlin · Jetpack Compose · Material 3 · Room · Hilt**<br />
Coroutines / Flow · Navigation Compose · WorkManager · Glance · ML Kit · Google Code Scanner

Arquitectura por capas con MVVM: reglas de negocio en `domain`, persistencia en `data` y pantallas con ViewModels en `presentation`.

<details>
<summary><strong>Compilar y preparar una versión</strong></summary>

Abre el proyecto en Android Studio y sincroniza Gradle.

| Acción | Windows | macOS / Linux |
| :--- | :--- | :--- |
| Compilar debug | `.\gradlew.bat assembleDebug` | `./gradlew assembleDebug` |
| Pruebas unitarias | `.\gradlew.bat testDebugUnitTest` | `./gradlew testDebugUnitTest` |
| Preparar siguiente parche y compilar release | `.\gradlew.bat buildNextRelease` | `./gradlew buildNextRelease` |

La versión vive en `version.properties`. `buildNextRelease` incrementa el parche y `versionCode`, sincroniza este README y genera el APK en `app/build/outputs/apk/release/`. Para un salto minor o major, usa `-PversionBump=minor` o `-PversionBump=major`.

Configura una firma de producción antes de publicar; nunca subas claves privadas ni APK sin firmar.

**Capas auxiliares:** `navigation` conecta las pantallas, `ui` define la apariencia, `widget` contiene los widgets, `di` configura Hilt y `core` reúne utilidades compartidas.

</details>

## `04` — Forma parte

¿Un fallo reproducible o una idea para mejorar Mony? [Abre un issue](https://github.com/Angel11R99/Mony/issues/new/choose). Para aportar código, consulta la [guía breve de contribución](.github/CONTRIBUTING.md).

**Código fuente disponible, con permisos limitados:** estudio, uso personal no comercial y contribuciones al proyecto oficial. Redistribución, versiones derivadas y uso comercial requieren autorización escrita. [Ver licencia](LICENSE.md).

<p align="center">
  <sub>Creado por <a href="https://github.com/Angel11R99">Angel Rodriguez</a> · Hecho para llevar tus finanzas a tu ritmo.</sub>
</p>
