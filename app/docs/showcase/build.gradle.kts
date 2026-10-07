import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Android variant so :app:docs:capture (the on-device capture tier) can iterate
    // the same commonMain scene registry the wasmJs live host serves.
    android {
        namespace = "com.dot.gallery.docs.showcase"
        compileSdk = 37
        minSdk = 29
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "docs-showcase.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":app:core:ui"))
            implementation(project(":app:core:domain"))
            implementation(project(":app:core:data"))
            implementation(project(":app:feature:settings"))
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.components.resources)
            implementation(libs.cmp.material.icons.extended)
            implementation(libs.kotlinx.coroutines.core)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
