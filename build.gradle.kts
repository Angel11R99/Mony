import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

val versionFile = layout.projectDirectory.file("version.properties").asFile
val readmeFile = layout.projectDirectory.file("README.md").asFile

fun readAppVersion(): Pair<Int, String> {
    val properties = Properties().apply {
        versionFile.inputStream().use(::load)
    }
    return properties.getProperty("VERSION_CODE").toInt() to
        properties.getProperty("VERSION_NAME")
}

fun updateReadmeVersion(versionName: String) {
    val versionBlock = """<!-- APP_VERSION_START -->
[![Versión](https://img.shields.io/badge/versi%C3%B3n-v$versionName-6750A4)](https://github.com/Angel11R99/Mony/releases/tag/v$versionName)
[![Descargar](https://img.shields.io/badge/descargar-%C3%BAltima_versi%C3%B3n-6750A4)](https://github.com/Angel11R99/Mony/releases/latest)
<!-- APP_VERSION_END -->"""
    val downloadBlock = """<!-- APP_DOWNLOAD_START -->
Descarga **Mony v$versionName** desde su [release en GitHub](https://github.com/Angel11R99/Mony/releases/tag/v$versionName) o consulta [todas las versiones disponibles](https://github.com/Angel11R99/Mony/releases).
<!-- APP_DOWNLOAD_END -->"""
    val currentReadme = readmeFile.readText()
    val updatedReadme = currentReadme
        .replace(
            Regex("<!-- APP_VERSION_START -->.*?<!-- APP_VERSION_END -->", RegexOption.DOT_MATCHES_ALL),
            versionBlock
        )
        .replace(
            Regex("<!-- APP_DOWNLOAD_START -->.*?<!-- APP_DOWNLOAD_END -->", RegexOption.DOT_MATCHES_ALL),
            downloadBlock
        )
    check(updatedReadme != currentReadme || currentReadme.contains("v$versionName")) {
        "README.md no contiene los marcadores de versión esperados."
    }
    readmeFile.writeText(updatedReadme)
}

tasks.register("syncVersionDocumentation") {
    group = "versioning"
    description = "Sincroniza la versión de version.properties con README.md."
    doLast {
        val (_, versionName) = readAppVersion()
        updateReadmeVersion(versionName)
    }
}

val bumpPatchVersion = tasks.register("bumpPatchVersion") {
    group = "versioning"
    description = "Incrementa VERSION_CODE y la revisión semántica de VERSION_NAME."
    doLast {
        val (versionCode, versionName) = readAppVersion()
        val parts = versionName.split('.').map(String::toInt)
        check(parts.size == 3) {
            "VERSION_NAME debe usar el formato MAJOR.MINOR.PATCH, por ejemplo 1.0.0."
        }
        val nextVersionName = "${parts[0]}.${parts[1]}.${parts[2] + 1}"
        versionFile.writeText(
            "VERSION_CODE=${versionCode + 1}\nVERSION_NAME=$nextVersionName\n"
        )
        updateReadmeVersion(nextVersionName)
        logger.lifecycle("Versión preparada: v$nextVersionName (${versionCode + 1})")
    }
}

tasks.register<GradleBuild>("buildNextRelease") {
    group = "versioning"
    description = "Incrementa la versión, actualiza README.md y compila el APK release."
    dependsOn(bumpPatchVersion)
    dir = rootDir
    tasks = listOf(":app:assembleRelease")
}
