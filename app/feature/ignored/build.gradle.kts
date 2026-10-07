import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    kotlin("plugin.parcelize")
}

kotlin {
    android {
        namespace = "com.dot.gallery.feature.ignored"
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
            implementation(project(":app:core:ui"))
            implementation(project(":app:core:domain"))
            implementation(project(":app:core:data"))
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.components.resources)
            implementation(libs.cmp.components.ui.tooling.preview)
            implementation(libs.haze)
            implementation(libs.haze.blur)
            implementation(libs.haze.blur.materials)
            implementation(libs.haze.glass)
            implementation(libs.haze.glass.material3)
            implementation(libs.cmp.material.icons.extended)
            implementation(libs.cmp.ui.backhandler)
            implementation(libs.compose.lifecycle.runtime)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlin.inject.runtime)
        }
        androidMain.dependencies {
            implementation(libs.javax.inject)
        }
    }
}

compose.resources {
    packageOfResClass = "com.dot.gallery.feature.ignored.generated.resources"
}

// `variant.sources.assets` is null under com.android.kotlin.multiplatform.library, so CMP
// registers copyAndroidMainComposeResourcesToAndroidAssets without an output dir and
// composeResources never reach the APK. Package them onto the androidMain classpath
// ourselves -- the resource reader falls back to classLoader.getResourceAsStream for
// anything not in assets.
val packageAndroidComposeResources by tasks.registering(Copy::class) {
    dependsOn(
        "prepareComposeResourcesTaskForCommonMain",
        "prepareComposeResourcesTaskForAndroidMain"
    )
    from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"))
    from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/androidMain/composeResources"))
    into(layout.buildDirectory.dir("generated/composeResAssets/composeResources/com.dot.gallery.feature.ignored.generated.resources"))
}
kotlin.sourceSets.getByName("androidMain").resources.srcDir(
    // srcDir at the composeResAssets root (parent of the Copy destination) so resource
    // paths keep their composeResources/<package> prefix; the provider carries the dep
    packageAndroidComposeResources.map { it.destinationDir.parentFile.parentFile }
)
