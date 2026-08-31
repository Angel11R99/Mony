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

android {
    namespace = "com.angel.mony"
    compileSdk {
        version = release(36)
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

androidComponents {
    onVariants(selector().all()) { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        tasks.configureEach {
            if (name == "assemble$variantName") {
                doLast {
                    val outputDirectory = layout.buildDirectory
                        .dir("outputs/apk/${variant.name}")
                        .get()
                        .asFile
                    val apks = outputDirectory.listFiles()?.filter { it.extension == "apk" }.orEmpty()
                    val generatedApk = apks.singleOrNull { !it.name.startsWith("Mony-") }
                    val currentApk = generatedApk ?: apks.singleOrNull {
                        it.name.startsWith("Mony-v$appVersionName-${variant.buildType}")
                    } ?: error("No se encontró el APK generado para ${variant.name}.")
                    val unsignedSuffix = if (currentApk.name.contains("unsigned")) "-unsigned" else ""
                    val targetApk = outputDirectory.resolve(
                        "Mony-v$appVersionName-${variant.buildType}$unsignedSuffix.apk"
                    )
                    if (generatedApk != null) {
                        targetApk.delete()
                        check(generatedApk.renameTo(targetApk)) {
                            "No se pudo renombrar ${generatedApk.name} como ${targetApk.name}."
                        }
                    }
                    val outputMetadata = outputDirectory.resolve("output-metadata.json")
                    outputMetadata.writeText(
                        outputMetadata.readText().replace(
                            Regex("\"outputFile\": \"[^\"]+\\.apk\""),
                            "\"outputFile\": \"${targetApk.name}\""
                        )
                    )
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
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
    ksp(libs.androidx.room.compiler)
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
