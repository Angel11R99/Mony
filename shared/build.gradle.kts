import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
}

kotlin {
    // AGP 9 ya no admite `androidTarget()` de KMP junto a los plugins `com.android.library`
    // o `com.android.application`. `androidLibrary` es el reemplazo oficial para AGP 9+.
androidLibrary {
        namespace = "com.angel.mony.shared"
        compileSdk = 36
        minSdk = 24
        // `:app` compila con source/target Java 11 sin toolchain, así que `shared` se
        // alinea con `jvmTarget` en lugar de exigir un JDK 11 instalado en la máquina.
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Los targets de iOS se declaran aquí pero Gradle los ignora al compilar en un host
    // que no los soporta (Windows). Se compilan y verifican en macOS con Xcode.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
        }
    }
}