# Auditoría de migración a Kotlin Multiplatform (Android + iOS)

> **Estado:** Fase 0 — auditoría previa a cualquier cambio de código.
> **Fecha:** 2026-10-02
> **Rama:** `4-mejora-migración-a-kotlin-multi-platform`
> **Estado del árbol:** limpio, sin cambios locales.
> **Baseline verificado:** `compileDebugKotlin` ✅ · `assembleDebug` ✅ · `testDebugUnitTest` ✅ (253 tests / 32 clases heredados + `NormalizationEquivalenceTest`, 254 tests / 33 clases en total, 0 fallos)

---

## 0. Cómo leer este documento

Este documento **no** describe todavía la migración definitiva: describe **qué se encontró**, **qué está bloqueado**,
**qué decisión hay que tomar** y **en qué orden**. Nada de lo escrito aquí está implementado todavía.

Regla de oro derivada de la auditoría:

> El repositorio actual compila y **todas sus pruebas pasan**. Cualquier cambio posterior que las haga fallar
> es una regresión nuestra, no un problema preexistente.

---

## 1. Inventario medido del proyecto

Medido sobre el árbol real, no sobre la documentación (que está desactualizada).

| Métrica | Valor |
|---|---|
| Módulos Gradle | 1 (`:app`) |
| Archivos Kotlin (main) | 319 |
| Líneas Kotlin (main) | 41.610 |
| Archivos Kotlin (test) | 32 |
| Líneas Kotlin (test) | 2.850 |
| Tests unitarios | **253** heredados en 32 clases (0 fallos, 0 omitidos); 254 en 33 clases con el test de equivalencia de normalización |
| `applicationId` | `com.angel.mony` |
| `VERSION_CODE` / `VERSION_NAME` | `6` / `1.0.5` |
| Gradle / AGP / Kotlin | 9.1.0 / 9.0.1 / 2.0.21 |
| `minSdk` / `targetSdk` / `compileSdk` | 24 / 36 / 36 |
| Base de datos real | **`FinanceDatabase` v17** |
| Schemas exportados | `app/schemas/1.json` … `17.json` |
| Entidades Room | **17** |
| DAOs | 12 |
| ViewModels | 14 |
| Widgets Glance | 11 (`widget/`, 16 archivos) |

### Discrepancias entre documentación y código

La documentación **no es fiable** como fuente. Correcciones verificadas:

| Documentación dice | Realidad verificada |
|---|---|
| `FinanceDatabase` versión 15 | **versión 17** |
| "varias migraciones 1→2 … 15→16" | existen migraciones **hasta 16→17** |
| No se mencionan tablas de funding | existen `expense_funding` y `ExpenseFunding` |
| No se mencionan tablas Fortnight | 4 tablas `fortnight_*` |

**Consecuencia:** cualquier trabajo de migración debe partir de `FinanceDatabase.kt` y `app/schemas/17.json`,
nunca de `docs/CONTEXTO.md`.

---

## 2. Clasificación de acoplamiento a plataforma

Criterio usado:

- **`SHARED_DIRECTLY`** — sin cambios, ya es común.
- **`SHARED_AFTER_REFACTOR`** — común tras un refactor mecánico y seguro.
- **`PLATFORM_ABSTRACTION_REQUIRED`** — común detrás de una interfaz `expect/actual` o puerto.
- **`ANDROID_ONLY`** — permanece en Android; iOS recibe equivalente propio.

### 2.1 `SHARED_DIRECTLY` (lista positiva)

- **`domain/model/*` salvo `java.time`** — modelos, validadores, enums, resultados sellados.
- **`domain/usecase/*`** — lógica de negocio pura.
- **Iconografía — hallazgo importante.** Los packs **Lucide** (`com.composables.icons.lucide`) y
  **Phosphor** (`com.adamglin.phosphoricons`) están **vendorizados dentro del propio repositorio** en
  `ui/iconography/vendor/`, junto con IconPark. Confirmado en `THIRD_PARTY_NOTICES.md` (ambos MIT).
  Son `ImageVector` generados, sin dependencia binaria externa.
  → **No hay librería de iconos que portar.** Solo 12 archivos importan
  `androidx.compose.material.icons` (el pack Material), y en Compose Multiplatform eso se resuelve
  con el artefacto `compose.materialIconsExtended`.
- **Tests de dominio puros** — 253 tests existentes, la mayoría ya portables.

### 2.2 `SHARED_AFTER_REFACTOR`

| Área | Acoplamiento actual | Acción |
|---|---|---|
| `java.time` | 58 archivos (18 en `domain/`) | Sustituir por `kotlinx-datetime`. **Requiere test de equivalencia por caso.** |
| `Math.*Exact` | 3 archivos de dominio | Crear `MoneyMath` común (overflow-safe) con el mismo comportamiento observable |
| `MoneyFormatter` | `java.text.NumberFormat`, `Currency`, `Locale`, `BigDecimal` | Separar **representación** (común, entero) de **formato** (por plataforma) |
| `CsvExporter` | `java.time.format`, `BigDecimal` | Extraer formateo de fecha a un formateador común |
| `HistorySpreadsheetWriter` | `java.util.zip.ZipOutputStream` | ZIP por plataforma; el modelo de filas es común |
| `javax.inject` | 23 archivos | Quitar anotaciones; usar constructores + contenedor KMP |

### 2.3 `PLATFORM_ABSTRACTION_REQUIRED`

| Capacidad | Android actual | iOS requerirá |
|---|---|---|
| Persistencia transaccional | `database.withTransaction` | `iosMain` transaction scope |
| Preferencias | `SharedPreferences` (5 archivos) | `NSUserDefaults` / DataStore KMP |
| Notificaciones | canales + `POST_NOTIFICATIONS` | `UNUserNotificationCenter` |
| Tareas periódicas | `WorkManager` (4 schedulers) | `BGTaskScheduler` |
| Widgets | Glance (11) | WidgetKit vía bridge |
| Cámara / OCR | Google ML Kit Text Recognition + GMS Document Scanner | VisionKit / `VNRecognizeTextRequest` |
| Exportar PDF | `android.graphics.pdf.PdfDocument` | PDFKit / CoreGraphics |
| Compartir / guardar | `FileProvider` + `Intent` | `UIActivityViewController` / `ShareLink` |
| Deep links | `android.net.Uri` en `FinanceApp.kt` | `NSUserActivity` / URL |

### 2.4 `ANDROID_ONLY`

- `FinanceApplication.kt` y los 4 schedulers (`FixedEntry`, `BudgetAlert`, `PendingReminder`, `WidgetRefresh`).
- Los 11 widgets Glance de `widget/`.
- `HistoryPdfWriter.kt` (API `android.graphics`).
- `WidgetData.kt` como *consumidor* Glance (los modelos que produce sí son comunes).
- `androidTest/` (7 archivos, tests de instrumentación).

### 2.5 Métricas de acoplamiento (archivos de `main`)

| Señal | Archivos | Lectura |
|---|---|---|
| Importan `android.*` | 45 / 319 (14 %) | Acoplamiento explícito bajo |
| Usan `java.time` | 58 | **El mayor bloqueador real** |
| Importan `androidx.room` | 33 | Solo la capa `data/` |
| Importan `javax.inject` | 23 | Trivial de quitar |
| Importan `androidx.navigation` | **1** | Excelente: navegación contenida en `FinanceApp.kt` |
| Usan `SharedPreferences` | 5 | Preferencias concentradas |
| Estado de UI | 98 × `collectAsStateWithLifecycle`, **0 × `collectAsState()`** | Patrón ya uniforme y portable |

**Conclusión de la auditoría:** el proyecto está **mejor acoplado de lo que sugiere la documentación**.
No hay una reescritura de por medio; hay que **extraer 3 bibliotecas JVM** (`java.time`,
`java.text`/`BigDecimal`/`zip`) y **desacoplar el DI**.

---

## 3. Los 17 puntos de decisión

Cada punto lleva: **situación encontrada → decisión propuesta → riesgo → cómo se verifica.**

### 1. Estructura de módulos Gradle
- **Encontrado:** un solo `:app`, con el `NavHost`, Hilt, WorkManager y los 11 widgets.
- **Decisión:** añadir un módulo `shared` (KMP) y **mantener `:app` sin renombrar**.
  Renombrar a `androidApp` es Cosmetic, pero obliga a tocar `buildNextRelease` (que depende de
  `:app:assembleRelease`), `release.yml`, `syncVersionDocumentation` y los scripts de `.github`.
  Ese churn no aporta nada a la migración y amplifica el riesgo sobre la cadena de releases.
- **Consecuencia:** `applicationId`, `versionCode`, `versionName`, firma y nombre de APK
  (`Mony-v<version>-<buildType>.apk`) quedan **intactos por construcción**, no por cuidado.
- **Verificación:** el APK de debug debe seguir instalándose y arrancando; comparar
  `versionCode`/`versionName`/firma antes y después.

### 2. Persistencia — ¿Room KMP o SQLDelight?
- **Encontrado:** Room `2.8.3`, 17 entidades, 12 DAOs, 33 archivos con import de Room, migraciones escritas a mano.
- **Decisión:** **Room KMP**, no SQLDelight. Motivo determinante: Room KMP usa el **mismo motor SQLite
  (AndroidSQLiteDriver)** y puede abrir el archivo `personal_finance.db` v17 existente. SQLDelight obligaría a
  **reescribir los 12 DAOs a SQL a mano** y a reconstruir toda la lógica transaccional: exactamente el tipo de
  reescritura que este proyecto no quiere.
- **Riesgo:** Room KMP sigue en maduración; su API KMP es más estrecha que la de Android.
- **Verificación:** abrir una base v17 real, leer filas y comprobar `user_version = 17`.

### 3. Compatibilidad con la base de datos existente
- **Encontrado:** `personal_finance.db` v17 con datos financieros reales; `fallbackToDestructiveMigration` prohibido.
- **Decisión:** Room KMP **sin destructivo**. Las 17 migraciones se conservan. Además, copia de seguridad
  obligatoria (`adb` pull) antes de la primera prueba sobre la base real.
- **Riesgo:** **crítico** si se pierde. **Verificación:** fixture v17 con datos → abrir → comparar filas.

### 4. Capa de dominio portable
- **Encontrado:** 18 archivos de dominio usan `java.time`; 3 usan `Math.*Exact`.
- **Decisión:** migrar a `kotlinx-datetime`. Crear `MoneyMath` en `commonMain` replicando
  `addExact/subtractExact/multiplyExact` **con el mismo comportamiento de saturación**.
- **Riesgo:** medio — un cambio silencioso aquí altera saldos.
- **Verificación:** los tests existentes de `BudgetCycleCalculator`, `FortnightPeriod`, `ExpenseFundingEvaluation`
  deben seguir dando **exactamente** los mismos números.

### 5. Dinero y formato
- **Encontrado:** `Long` en centavos en todo el dominio (correcto). El problema es *solo* el formateo:
  `NumberFormat` + `Currency` + `Locale("es","DO")` + `BigDecimal`, y `cents / 100.0` en pantalla.
- **Decisión:** el **dominio sigue siendo `Long` céntimos, sin cambios**. Se separan tres capas:
  1. `MoneyAmount(cents)` — aritmética entera común (común a las dos plataformas).
  2. `MoneyFormatter` — produce la cadena; implementación por plataforma para garantizar
     `RD$ 1.250,00` idéntico en ambos sistemas.
  3. Parsing — común, validado.
- **Riesgo:** medio — separadores de miles y coma decimal.
- **Verificación:** los 3 tests de `MoneyFormatterTest` + tabla de casos con acentos de `RD$`.

### 6. Repositorios y transacciones
- **Encontrado:** `RoomRepositories.kt` = 1.391 líneas con `withTransaction`; `ExpenseFunding` duplica la
  lógica de desborde en varios flujos. `SaveTransaction` recibe `javax.inject.Inject`.
- **Decisión:** los repositorios se mueven tal cual. `withTransaction` se encapsula en un puerto
  `TransactionRunner` (Room KMP en ambos, sin código específico de SO). **No se cambia ni una línea de
  la lógica cross-table.**
- **Riesgo:** **crítico** — es donde vive la integridad de los datos.
- **Verificación:** tabla de equivalencia: cada operación con `withTransaction` debe conservar
  atomicidad, rollback y efectos colaterales.

### 7. Inyección de dependencias
- **Encontrado:** Hilt `2.60.1` en 23 archivos; 14 ViewModels con `hiltViewModel()`; patrón
  `@EntryPoint` + `EntryPointAccessors` para widgets y workers.
- **Decisión:** **Koin** para `commonMain` (maduro en KMP, sin KSP, soporte nativo). Hilt **se queda en
  `androidApp`/fase inicial** y se retira solo cuando la capa común quede completa.
- **Alternativa considerada:** contenedor propio. Descartada por coste/beneficio.
- **Riesgo:** medio. **Verificación:** mismo grafo de objetos disponible en ambos objetivos.

### 8. Estado de presentación
- **Encontrado:** 14 ViewModels, `viewModelScope`, **98 usos de `collectAsStateWithLifecycle`** y
  **0 de `collectAsState()`** → patrón uniforme y ya portable.
- **Decisión:** los ViewModels se convierten en *state holders* comunes con `kotlinx.coroutines.viewModelScope`.
  En iOS se accede con `@Composable` + máquina de estados, sin `hiltViewModel()`.
- **Riesgo:** bajo. **Verificación:** los 3 tests de `StartupScreenTest` y paridad de estado por pantalla.

### 9. Navegación
- **Encontrado:** **un solo archivo**, `navigation/FinanceApp.kt`, con el único `NavHost`.
  Importa `android.net.Uri`.
- **Decisión:** mantener **un único grafo compartido** — no dos. Se migra la definición de rutas a una
  sealed class común y `android.net.Uri` se aísla detrás de un tipo `AppRoute`/`DeepLink`.
- **Riesgo:** medio — `FloatingModuleBarTest` (11 tests) depende del comportamiento de la barra flotante.
- **Verificación:** los 11 tests de `FloatingModuleBarTest` + recorrido manual de las rutas
  (`home`, `add/{type}`, `edit/{type}/{id}`, `history`, `statistics`, `fixed`, `pending`, `fortnight`,
  `fortnight/templates`, `settings`).

### 10. UI Compose compartida
- **Encontrado:** Compose BOM `2024.09.00`, Material 3, tema en `ui/theme/`; **el pack de iconos Material**
  es la única dependencia de Material Icons.
- **Decisión:** Compose Multiplatform. **Misma identidad visual**, sin rediseño.
  En iOS: `material3` se mantiene y se documentan los ajustes inevitables
  (tipografía del sistema, ripple, densidad de toque, diálogos).
- **Riesgo:** medio-alto — es donde "funciona" se convierte en "se ve igual".
- **Verificación:** `IconographyTest` (20 tests) + `DecorativeBackgroundTest` (3) como puerta de calidad.

### 11. Apariencia y tema
- **Encontrado:** `AppAppearance` / `AppearancePreferences` sobre `SharedPreferences`, semilla de color
  elegida por el usuario, icon packs con persistencia de la elección.
- **Decisión:** migrar a DataStore KMP o un store KMP propio, **conservando claves y valores actuales**
  para que las instalaciones existentes no pierdan su tema. Sin migrar por moda (regla del proyecto).
- **Riesgo:** medio — afecta la primera apertura tras actualizar.
- **Verificación:** prueba de migración de preferencias con valores precargados.

### 12. Fechas, zonas horarias y husos
- **Encontrado:** persistencia en `*EpochDay` / `*EpochMillis` y uso de `java.time` en 58 archivos.
  `Core library desugaring` activo, `minSdk = 24`.
- **Decisión:** migrar a `kotlinx-datetime` manteniendo **el mismo formato de persistencia**
  (`epochDay` / `epochMillis`), de modo que las bases existentes siguen siendo legibles.
  `ZoneId` por plataforma.
- **Riesgo:** medio-alto — los cortes de ciclo dependen del huso.
- **Verificación:** `BudgetCycleTest` (21 tests) + `DateRangeTest` (3) + casos de cambio de día.

### 13. Trabajo en segundo plano
- **Encontrado:** 4 schedulers en `FinanceApplication.onCreate` → `FixedEntry`, `BudgetAlert`,
  `PendingReminder`, `WidgetRefresh`, intervalo único de 15 min, `KEEP`.
- **Decisión:** **Android conserva WorkManager tal cual.** iOS recibe su propio scheduler equivalente
  (`BGTaskScheduler`), registrado en el AppDelegate. No se reescribe el scheduler de Android.
- **Riesgo:** medio. **Verificación:** `PendingReminderTest` (3) + disparo manual en ambos sistemas.

### 14. Notificaciones
- **Encontrado:** canales Android en `FinanceApplication`, con permiso `POST_NOTIFICATIONS`.
- **Decisión:** detrás de un `Notifier` común con dos implementaciones. El **texto y la lógica de
  evaluación** (`BudgetAlertEvaluator`, 6 tests) ya son comunes y no se tocan.
- **Riesgo:** bajo. **Verificación:** `BudgetAlertEvaluatorTest` (6) + receptor en ambos sistemas.

### 15. Widgets
- **Encontrado:** 11 widgets Glance; `WidgetData.kt` mezcla Hilt EntryPoint + repositorios + `updateAll`;
  ya existen `WidgetFormattingTest` (13) y `WidgetCalculationsTest` (11).
- **Decisión:** **el cálculo y los snapshots son comunes** (y sus 24 tests pasan tal cual).
  Android mantiene Glance. iOS usa WidgetKit con un bridge, **reutilizando los mismos cálculos**.
- **Riesgo:** medio. **Verificación:** los 24 tests de widgets deben estar verdes en `commonTest`.

### 16. Exportaciones, backup y OCR
- **Encontrado:** `FullBackupExporter` (formato de backup v3, exporta las 17 entidades con remapeo de IDs)
  **importa entidades Room** → está acoplado a la capa Android.
  `CsvExporter` (10 tests), `HistorySpreadsheetWriter`, `HistoryPdfWriter` (`android.graphics.pdf`),
  `FileProvider`. El OCR **sí existe hoy**: ML Kit Text Recognition y GMS Document Scanner, usados en
  `presentation/list/ShoppingListScreen.kt`; `ListReceiptParser` (24 tests) es parsing de texto puro.
- **Decisión:** el **modelo de backup v3 pasa a ser común** (serialización `kotlinx.serialization`,
  ya presente: `1.8.1`). Solo la *escritura del archivo* y el *PDF* se abstraen por plataforma.
- **Riesgo:** medio-alto — el backup es la red de seguridad del usuario.
- **Verificación:** exportar → importar → **comparar los 3 conjuntos** en ambas plataformas.

### 17. Identidad, firma y releases
- **Encontrado:** `version.properties` (`6` / `1.0.5`), workflow `release.yml` con `version_bump`, scripts de
  `syncVersionDocumentation`, salida `Mony-v<version>-<buildType>.apk`.
- **Decisión:** `androidApp` conserva **exactamente** la misma versión, firma, `applicationId` y nombre de
  artefacto. La versionación sigue siendo la de `version.properties`. **Nunca** publicar un APK sin firmar.
- **Riesgo:** **crítico** — romper la cadena de actualización de los usuarios.
- **Verificación:** comparar `versionCode`/`versionName`/firma del APK antes y después de cada fase.

---

## 4. Riesgos principales

| # | Riesgo | Severidad | Mitigación |
|---|---|---|---|
| R1 | Pérdida de datos al tocar la persistencia | **Crítica** | Copia de la base real antes de cada prueba; nunca destructivo; tests de migración |
| R2 | Desborde de `Long` alterado al portar `Math.*Exact` | **Crítica** | `MoneyMath` común + tests de equivalencia exacta |
| R3 | Ruptura de la cadena de actualización Android | **Crítica** | Congelar `applicationId`, firma y `versionCode` |
| R4 | Regresión en la lógica transaccional cross-table | **Crítica** | `RoomRepositories.kt` se mueve **literalmente**; tabla de equivalencia |
| R5 | Divergencia de `java.time` (husos, DST, cortes de ciclo) | Alta | Mantener `epochDay`/`epochMillis`; `BudgetCycleTest` como puerta |
| R6 | Backup v3 no restaurable | Alta | Comparar los 3 conjuntos en ambas plataformas |
| R7 | Divergencia visual iOS | Alta | Sin rediseño; ajustes documentados; tests de iconografía como puerta |
| R8 | Madurez de Room KMP | Media | Spike aislado antes de migrar los 12 DAOs |
| R9 | Regresión en widgets/notificaciones por cambio de plataforma | Media | 24 tests de widgets verdes en `commonTest` |
| R10 | Diff gigantesco difícil de revisar | Media | Fases verticales pequeñas y compilables, una por PR |

---

## 5. Fases propuestas

Cada fase debe quedar **compilando y con los tests verdes**, sin funcionalidad incompleta.

| Fase | Alcance | Puerta de salida |
|---|---|---|
| **0. Auditoría** | Este documento | ✅ Hecho |
| **1. Andamiaje KMP** | Módulo `shared` (KMP) manteniendo `:app` intacto | APK de debug idéntico, 253 tests verdes |
| **2. Dominio común** | `domain/model` + `usecase` a `commonMain`; `kotlinx-datetime`; `MoneyMath` | Tests de dominio verdes en común |
| **3. Datos comunes** | Room KMP, 17 entidades, migraciones, repositorios | Abre v17 real y lee filas correctamente |
| **4. DI + state holders** | Koin, ViewModels → state holders | Grafo igual en ambas plataformas |
| **5. UI compartida** | Compose M3, tema, iconografía, navegación única | Iconografía y navegación verdes en común |
| **6. Servicios de plataforma** | Notificaciones, tareas, preferencias, export, backup | Backup exportable/importable en ambas |
| **7. iOS app** | `iosApp`, WidgetKit, adaptaciones visuales | Recorrido completo en iOS |
| **8. Paridad** | Checklist funcional | `docs/KMP_FEATURE_PARITY.md` al 100 % |

---

## 6. Lo que este documento todavía NO decide

Se documenta aquí explícitamente para no dar por hecho nada:

1. Versión exacta de Room KMP a adoptar (R8) → requiere un *spike* medido.
2. Elección final DataStore KMP vs. store propio para preferencias.
3. Estrategia de WidgetKit en iOS (bridge vs. reimplementación).
4. Alcance del PDF en iOS (nuevo, o se exporta HTML/CSV).
5. Cómo se preserva la elección de icon pack del usuario entre plataformas.

Nada de esto bloquea la **Fase 1**.

---

## 7. Documentos relacionados

- `docs/KMP_PERSISTENCE_DECISION.md` — comparación detallada Room KMP vs. SQLDelight (siguiente).
- `docs/KMP_ARCHITECTURE.md` — estructura final de módulos.
- `docs/KMP_PLATFORM_SERVICES.md` — tabla de capacidades Android/iOS.
- `docs/KMP_FEATURE_PARITY.md` — checklist de paridad funcional.
- `docs/CONTEXTO.md` — **desactualizado** respecto a DB v17; usar con precaución.