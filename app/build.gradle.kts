import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val appVersionProperties = Properties().apply {
    rootProject.file("version.properties").inputStream().use(::load)
}
val appVersionCode = appVersionProperties.getProperty("VERSION_CODE").toInt()
val appVersionName = appVersionProperties.getProperty("VERSION_NAME")

val releaseStoreFile = System.getenv("RELEASE_STORE_FILE")

android {
    namespace = "com.angel.mony"
    compileSdk {
        version = release(36)
    }

    signingConfigs {
        create("release") {
            storeFile = releaseStoreFile?.let { file(it) }
            storePassword = System.getenv("RELEASE_STORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.angel.mony"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

}

fun readPackagedVersionName(outputDirectory: File): String? {
    val metadata = outputDirectory.resolve("output-metadata.json")
    if (!metadata.isFile) return null
    return Regex("\"versionName\": \"([^\"]+)\"").find(metadata.readText())?.groupValues?.get(1)
}

androidComponents {
    onVariants { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val packageTaskName = "package$variantName"
        val assembleTaskName = "assemble$variantName"
        val unsignedSuffix =
            if (variant.buildType == "release" && releaseStoreFile == null) "-unsigned" else ""
        val targetApkName = "Mony-v$appVersionName-${variant.buildType}$unsignedSuffix.apk"
        val outputDirectory = layout.buildDirectory.dir("outputs/apk/${variant.name}")
        val targetApk = outputDirectory.map { it.file(targetApkName) }

        // El APK se renombra al finalizar el empaquetado, por lo que el archivo declarado
        // por la tarea de empaquetado deja de existir. Si el nombre final falta o quedó
        // desactualizado hay que volver a empaquetar en lugar de reutilizar un artefacto viejo.
        tasks.configureEach {
            if (name == packageTaskName) {
                outputs.upToDateWhen {
                    targetApk.get().asFile.isFile &&
                        readPackagedVersionName(outputDirectory.get().asFile) == appVersionName
                }
            }
        }

        tasks.register("finalize${variantName}Apk") {
            group = "build"
            description = "Nombra el APK de ${variant.name} como $targetApkName."
            dependsOn(packageTaskName)
            val outputDirectory = outputDirectory
            val targetApk = targetApk
            outputs.file(targetApk)
            doLast {
                val directory = outputDirectory.get().asFile
                val targetFile = targetApk.get().asFile
                // Prefiere el APK recién empaquetado (app-debug.apk) frente a uno ya renombrado.
                val packagedApk = directory.listFiles()
                    ?.filter { it.extension == "apk" }
                    ?.firstOrNull { !it.name.startsWith("Mony-") }
                    ?: directory.listFiles()
                        ?.singleOrNull { it.extension == "apk" }
                if (packagedApk == null) {
                    // packageDebug se saltó porque el APK final ya existe y está al día;
                    // no hay nada que renombrar.
                    check(targetFile.isFile) {
                        "No se encontró el APK generado para ${variant.name} en ${directory.absolutePath}."
                    }
                    return@doLast
                }
                directory.listFiles()
                    ?.filter { it.extension == "apk" && it.name != packagedApk.name }
                    ?.forEach { it.delete() }
                if (packagedApk == targetFile) return@doLast
                check(packagedApk.renameTo(targetFile)) {
                    "No se pudo renombrar ${packagedApk.name} como ${targetFile.name}."
                }
                val metadata = directory.resolve("output-metadata.json")
                if (metadata.isFile) {
                    metadata.writeText(
                        metadata.readText().replace(
                            Regex("\"outputFile\": \"[^\"]+\\.apk\""),
                            "\"outputFile\": \"${targetFile.name}\""
                        )
                    )
                }
            }
        }

        tasks.configureEach {
            if (name == assembleTaskName) {
                dependsOn("finalize${variantName}Apk")
            }
        }
    }
}

dependencies {
    implementation(project(":shared"))
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime)
    implementation(libs.google.code.scanner)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.google.document.scanner)
    implementation(platform(libs.kotlinx.serialization.bom))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
