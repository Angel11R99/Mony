<h1 align="center">Construyamos un mejor Mony</h1>
<p align="center">Errores · Ideas · Código · Documentación</p>

## `01` — Empieza con un issue

Busca si existe uno similar y [abre un issue](https://github.com/Angel11R99/Mony/issues/new/choose). Antes de desarrollar una PR, espera confirmación del titular. Las funciones nuevas deben discutirse y aprobarse previamente.

Para reportar un fallo, incluye **versión de Mony y Android, dispositivo, pasos, resultado esperado y observado**, y capturas si ayudan. No publiques datos financieros, respaldos, credenciales ni información personal.

## `02` — Prepara tu cambio

Crea un fork y una rama para un único cambio. Abre el proyecto en Android Studio, sincroniza Gradle y compila antes de empezar.

| Verificación | Windows | macOS / Linux |
| :--- | :--- | :--- |
| Compilación | `.\gradlew.bat assembleDebug` | `./gradlew assembleDebug` |
| Pruebas unitarias | `.\gradlew.bat testDebugUnitTest` | `./gradlew testDebugUnitTest` |

**Las reglas esenciales:**

- Inspecciona el código existente y conserva las capas Kotlin / Compose / ViewModel / repositorios / Room / Hilt; no accedas a DAOs desde pantallas o ViewModels.
- Mantén las finanzas disponibles sin conexión, textos en español y montos DOP en centavos enteros; reutiliza el formateador monetario y las utilidades de fecha.
- Respeta los temas claro, oscuro y del sistema, valida las entradas, muestra mensajes comprensibles y actualiza los widgets afectados.
- Evita cambios ajenos al objetivo, dependencias innecesarias, publicidad, seguimiento y servicios remotos no aprobados.
- Añade pruebas relevantes para cambios de lógica, presupuesto, fechas, validación o cálculos cuando sea práctico.

> [!IMPORTANT]
> Si cambia el esquema de Room: incrementa su versión, añade y registra una migración explícita no destructiva, genera el esquema JSON y verifica la conservación de datos. Nunca uses `fallbackToDestructiveMigration()`.

Las convenciones completas están en [AGENTS.md](../AGENTS.md).

## `03` — Envía tu PR

Vincula un **issue abierto de este repositorio, creado antes que la PR**, mediante `Closes #123`, `Fixes #123` o `Resolves #123`. No debe existir otra PR abierta para el mismo issue. La comprobación automática valida estos requisitos y rechaza referencias a otras PR.

Usa la [plantilla de PR](PULL_REQUEST_TEMPLATE.md): problema, solución, pasos de verificación, pruebas ejecutadas, evidencia visual y migraciones cuando correspondan. El titular revisa y decide la incorporación.

---

**Al contribuir**, confirmas la autoría o el permiso para aportar tu trabajo y aceptas las [condiciones de la licencia](../LICENSE.md), que permiten integrar y distribuir el aporte como parte de Mony. Los forks sirven para estudiar o contribuir; redistribuir, publicar derivados o presentar Mony como propio requiere autorización.
