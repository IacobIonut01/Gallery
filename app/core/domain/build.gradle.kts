import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    kotlin("plugin.parcelize")
}

kotlin {
    android {
        namespace = "com.dot.gallery.core.domain"
        compileSdk = 37
        minSdk = 29
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    // Desktop shell target — commonApp/desktopApp consume domain from commonMain.
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    targets.configureEach {
        if (name == "android") {
            compilations.configureEach {
                compileTaskProvider.configure {
                    compilerOptions.freeCompilerArgs.addAll(
                        "-P",
                        "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.dot.gallery.domain.parcel.Parcelize"
                    )
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.uri.kmp)
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.room3.common)
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.ui)
        }
        androidMain.dependencies {
            implementation(libs.room3.runtime)
            implementation(libs.androidx.exifinterface)
            implementation(libs.androidx.core.ktx)
            implementation(libs.datastore.prefs)
            implementation(libs.jxl.coder.coil)
            implementation(libs.avif.coder.coil)
            implementation(libs.javax.inject)
            // Media decode infra moved from :app:androidApp (core/decoder, core/sandbox, core/image/thumbnail)
            implementation(libs.sketch.core)
            implementation(libs.sketch.animated.core)
            implementation(libs.sketch.video)
            implementation(libs.zoomimage.core)
            implementation(libs.glide)
            implementation(libs.androidsvg)
            implementation(libs.nga.tiff)
            implementation(libs.okio)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}
