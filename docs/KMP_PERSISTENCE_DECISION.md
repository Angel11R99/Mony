# Decisión de persistencia: Room KMP frente a SQLDelight

> **Decisión: Room KMP.**
> Documento de auditoría — Fase 0. Nada implementado todavía.
> Fecha: 2026-10-02 · Rama: `4-mejora-migración-a-kotlin-multi-platform`

---

## 1. Veredicto

**Adoptar Room KMP.** Se descarta SQLDelight.

La decisión no es una preferencia de moda: se apoya en cinco hechos medidos en este repositorio,
no en documentación genérica.

| # | Hecho medido | Consecuencia |
|---|---|---|
| H1 | El proyecto ya usa **Room 2.8.3** | Room 2.8 ya soporta KMP. **No hace falta subir de versión** solo para multiplatform |
| H2 | Las **17 entidades** son Kotlin puro con anotaciones Room | Se mueven a `commonMain` sin reescribir |
| H3 | **98 métodos `suspend`** y **28 no-suspend que devuelven todos `Flow`** | La capa DAO **ya cumple** la regla de Room 3.0. **Cero cambios** |
| H4 | Las **16 migraciones** son `execSQL` con SQL literal — **sin cursores, sin binds, sin queries** | Conversión mecánica a `SQLiteConnection`. **Cero lógica que perder** |
| H5 | El proyecto ya usa **KSP** (`2.3.11`), nunca KAPT | Room 3.0 exige KSP. Condición ya cumplida |

---

## 2. Por qué SQLDelight se descarta

SQLDelight es técnicamente sólido y más maduro en KMP que Room. Se descarta por **coste de reescritura**,
que es exactamente lo que este proyecto prohíbe:

1. **12 DAOs que reescribir a SQL a mano.** Los DAO actuales son métodos anotados con
   `@Query`/`@Insert`/`@Update`. SQLDelight no lee esas anotaciones: habría que transcribir
   ~126 métodos a archivos `.sq`, reescribiendo cada cadena SQL a mano.
2. **Reconstruir `RoomRepositories.kt` (1.391 líneas).** La lógica cross-table de transacciones,
   `withTransaction`, el manejo de desborde de `ExpenseFunding` y la re-vinculación de fijos/pendientes
   están escrita contra la API transaccional de Room. Con SQLDelight habría que reimplementar
   ese contrato, y ahí es donde viven los datos del usuario.
3. **Reimplementar las 16 migraciones** contra un esquema que SQLDelight no conoce.
4. **Verificación de consultas en tiempo de compilación perdida.** H2 y H5 se convierten en
   "escritura masiva con verificación posterior" en lugar de "el compilador lo garantiza".

En resumen: SQLDelight obligaría a reescribir aproximadamente **la mitad de la capa de datos**.
Room KMP permite moverla.

---

## 3. Variante de versión: Room 2.8.3 KMP frente a Room 3.0

| | Room 2.8.3 KMP | Room 3.0 (`androidx.room3`) |
|---|---|---|
| Soporte KMP | Sí | Sí, es el objetivo de la librería |
| Paquete | `androidx.room` | `androidx.room3` |
| Migraciones en `commonMain` | `Migration` recibe `SupportSQLiteDatabase` (**Android-only**) | `suspend fun migrate(connection: SQLiteConnection)` (**común**) |
| Procesador | KSP o KAPT | Solo KSP |
| DAOs | admite `suspend` y no-`suspend` | exige `suspend` salvo observables |
| Madurez en KMP | Alta | Reciente (3.0.x) |

### El punto que decide

La migración de una base v17 real debe ejecutarse **en Android y en iOS**.
Con Room 2.8.3, la clase `Migration` es Android-only, porque recibe `SupportSQLiteDatabase`.
Por tanto, incluso quedándose en 2.8.3 habría que escribir la migración a mano.

**Solución robusta y válida en ambas versiones:** no depender de la clase `Migration`, sino escribir un
`SchemaMigrator` común que aplique la ruta de SQL existente leyendo `user_version`:

```kotlin
// Propuesta de forma — NO implementada todavía
internal class SchemaMigrator(private val connection: SQLiteConnection) {
    suspend fun migrateTo(target: Int) {
        var current = connection.readUserVersion()
        while (current < target) {
            SQL_PATH[current]?.forEach { connection.execSQL(it) } ?: error("Falta migración $current")
            current++
            connection.writeUserVersion(current)
        }
    }
}
```

Consecuencias:

- Los 16 bloques `execSQL` existentes se **copian tal cual** dentro de `SQL_PATH`.
- El migrador funciona en Room 2.8.3 **y** en Room 3.0, porque `SQLiteConnection` existe en ambas.
- Se elimina la dependencia de `androidx.sqlite.db`, que es Android-only.

> Esto **reduce el riesgo R8** (madurez de Room KMP): si Room 3.0 resulta problemático,
> se puede permanecer en 2.8.3 sin reescribir las migraciones.

### Ruta recomendada

1. Mover a `shared` con **Room 2.8.3 KMP** y el `SchemaMigrator` común. Sin cambio de paquete.
2. Verificar contra una base v17 real.
3. Evaluar la subida a `androidx.room3` **como fase posterior y opcional**, no como parte de la
   migración a iOS.

Esta ruta evita hacer coincidir dos cambios de riesgo (multiplatform + major version) en la misma fase.

---

## 4. Driver SQLite

| Driver | Artefacto | Platforms | Tamaño |
|---|---|---|---|
| `BundledSQLiteDriver` | `androidx.sqlite:sqlite-bundled` | Android, iOS, JVM, Linux | **Aumenta** |
| `AndroidSQLiteDriver` | `androidx.sqlite:sqlite-framework` | Solo Android | No aumenta |
| `NativeSQLiteDriver` | `androidx.sqlite:sqlite-framework` | iOS, macOS, Linux | No aumenta |

### Decisión: `BundledSQLiteDriver` en **ambas** plataformas

A pesar de que en Android `AndroidSQLiteDriver` ahorraría tamaño, se elige el driver embebido
en ambos sistemas por una razón de paridad funcional:

- **Una sola versión de SQLite en las dos plataformas.** Se evita divergencia en ordenación de
  textos, comparación de cadenas y manejo de fechas — y el app ordena categorías por nombre,
  busca productos por texto y agrupa por fecha.
- **Un solo motor que validar.** Un bug de SQLite se reproduce igual en ambos sistemas.
- Consistencia con la documentación oficial, que recomienda `BundledSQLiteDriver` para KMP.

Coste aceptado: aumento del APK y del binario iOS. Se documenta como conscious trade-off.

---

## 5. Riesgos de la persistencia y su mitigación

| Riesgo | Severidad | Mitigación |
|---|---|---|
| Room encuentra el archivo en otra ruta y "crea" una base vacía | **Crítica** | Verificar la ruta real del archivo en un dispositivo con datos; comprobar `user_version = 17` y número de filas al abrir |
| Un `execSQL` no portado a `SQLiteConnection` | **Crítica** | Los 16 son SQL literal; comparar carácter a carácter contra el original en el diff |
| `SchemaMigrator` sin write de `user_version` | **Crítica** | Test de migración con fixture v1→v17 que verifique `user_version` al final |
| Divergencia de SQLite entre plataformas | Alta | `BundledSQLiteDriver` en ambas (ver §4) |
| Regresión en `withTransaction` | **Crítica** | Los repositorios se mueven literalmente; tabla de equivalencia por operación |
| API KMP de Room más estrecha que la de Android | Media | Compilar el módulo `shared` antes de mover nada de la UI |
| `fallbackToDestructiveMigration` colándose por convenience | **Crítica** | Prohibido explícitamente; revisar en cada revisión |
| Room 3.0 con Kotlin 2.0.21 | Media | Se **evita** en la ruta recomendada; Room 3.0 queda como fase opcional |

---

## 6. Verificación obligatoria antes de dar por buena esta decisión

Ninguna fase que toque persistencia se da por terminada sin pasar esta lista:

1. Copia de `personal_finance.db` de un dispositivo con datos reales.
2. Abrir esa base con el código nuevo: `user_version == 17`.
3. Contar filas de las 17 tablas antes y después: **idéntico**.
4. Ejecutar el recorrido completo: registrar gasto, ingreso, cierre de ciclo, fijo, pendiente,
   pago de quincena, compra de lista deshopping.
5. Exportar backup v3, importar en un entorno limpio, comparar los tres conjuntos.
6. Repetir 5 en la plataforma contraria.
7. `testDebugUnitTest` verde (baseline: 253 tests / 32 clases).

---

## 7. Lo que esta decisión **no** cubre

- **Preferencias:** `SharedPreferences` es un problema aparte (ver punto 11 de
  `KMP_MIGRATION_AUDIT.md`). No tiene relación con Room.
- **Caché en memoria:** `FinanceDataCache` es lógica de aplicación, no persistencia.
- **Backup:** el formato v3 es un fichero portable; se-commoniza en la fase 6, no aquí.

---

## 8. Documentos relacionados

- `docs/KMP_MIGRATION_AUDIT.md` — auditoría completa y los 17 puntos.
- `docs/KMP_ARCHITECTURE.md` — estructura de módulos.
- `docs/KMP_PLATFORM_SERVICES.md` — capacidades por plataforma.
- `docs/KMP_FEATURE_PARITY.md` — checklist de paridad.