import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    android {
        namespace = "com.dot.gallery.feature.setup"
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
            implementation(compose.preview)
            implementation(libs.cmp.material.icons.extended)
            implementation(libs.cmp.ui.backhandler)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.haze)
            implementation(libs.haze.blur)
            implementation(libs.haze.blur.materials)
        }
    }
}

compose.resources {
    packageOfResClass = "com.dot.gallery.feature.setup.generated.resources"
}

// `variant.sources.assets` is null under com.android.kotlin.multiplatform.library, so CMP's
// copyAndroidMainComposeResourcesToAndroidAssets never gets an output dir and
// composeResources never reach the APK. Copy them ourselves into the package-qualified
// layout the runtime expects and ship them on the androidMain classpath — the resource
// reader falls back to classLoader.getResourceAsStream for anything not in assets.
val packageAndroidComposeResources by tasks.registering(Copy::class) {
    dependsOn(
        "prepareComposeResourcesTaskForCommonMain",
        "prepareComposeResourcesTaskForAndroidMain"
    )
    from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"))
    from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/androidMain/composeResources"))
    into(
        layout.buildDirectory.dir(
            "generated/composeResAssets/composeResources/com.dot.gallery.feature.setup.generated.resources"
        )
    )
}
kotlin.sourceSets.getByName("androidMain").resources.srcDir(
    // parent of the Copy task's destinationDir so resource paths keep their
    // composeResources/<package> prefix; the provider chain carries the task dep
    packageAndroidComposeResources.map { it.destinationDir.parentFile.parentFile }
)
