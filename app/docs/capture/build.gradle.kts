import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.compose.compiler)
}

// The docs capture tier (DOCS_SITE_PLAN.md §3, §7). A plain com.android.library
// whose own androidTest APK is the instrumentation target — the scenes render
// content composables straight from :app:docs:showcase's shared registry, so no
// :app:androidApp assemble is needed.
android {
    namespace = "com.dot.gallery.docs.capture"
    compileSdk = 37

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Same packaging gap as :app:androidApp — scenes render through CMP `Font(path, assets)`,
// which reads only AssetManager. Library composeResources ride the classpath in
// the test APK, so merge them into the androidTest variant's real assets.
val captureResModules = listOf(
    ":app:docs:showcase",
    ":app:core:ui",
    ":app:core:domain",
    ":app:feature:settings",
    // Native scenes pull typefaces from these modules' composeResources too —
    // without them the editor/viewer text renders in the fallback font.
    ":app:commonApp",
    ":app:feature:editor",
    ":app:feature:viewer",
)
    .map(::project)
    .filter {
        file("${it.projectDir}/src/commonMain/composeResources").isDirectory ||
            file("${it.projectDir}/src/androidMain/composeResources").isDirectory
    }
abstract class MergeComposeResAssetsTask : DefaultTask() {
    @get:InputFiles
    abstract val resourceDirs: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun run() {
        val dest = output.get().asFile
        resourceDirs.files.forEach { dir ->
            if (!dir.isDirectory) return@forEach
            // only fonts need the AssetManager path; strings/images resolve via classpath
            dir.walkTopDown()
                .filter { it.isFile && "/font/" in "/${it.relativeTo(dir).invariantSeparatorsPath}" }
                .forEach { it.copyTo(File(dest, it.relativeTo(dir).path), overwrite = true) }
        }
    }
}

val mergeComposeResAssetsForAndroidTest by tasks.registering(MergeComposeResAssetsTask::class) {
    captureResModules.forEach { m ->
        dependsOn("${m.path}:packageAndroidComposeResources")
        resourceDirs.from(m.layout.buildDirectory.dir("generated/composeResAssets"))
    }
    output.set(layout.buildDirectory.dir("generated/composeResMergedAndroidTestAssets"))
}
androidComponents.onVariants { variant ->
    variant.androidTest?.sources?.assets?.addGeneratedSourceDirectory(
        mergeComposeResAssetsForAndroidTest
    ) { it.output }
}

// Stage ONLY the two Subject-Cutout SAM models as androidTest assets — the
// subject-cutout scene copies them through the real ModelManager (validation,
// checksum, onDownloadComplete) into the isolated docs app's private files.
val prepareDocsCutoutModels by tasks.registering(Sync::class) {
    from(rootProject.file("app/ml-models-pack/src/main/assets")) {
        include("mobile_sam_image_encoder.onnx", "sam_mask_decoder_single.onnx")
    }
    into(layout.buildDirectory.dir("generated/docsCutoutModels"))
}
// srcDir() equivalent for the generated dir: AGP 9's `android.sourceSets` DSL no
// longer supports `getByName("androidTest")` from Kotlin (the decorated source set
// does not implement the legacy AndroidLibrarySourceSet interface — ClassCastException),
// so register the dir through the variant sources API and pin task ordering onto the
// merge task, which is what a producer-tracked srcDir provider would have done.
androidComponents.onVariants { variant ->
    variant.androidTest?.sources?.assets?.addStaticSourceDirectory(
        layout.buildDirectory.dir("generated/docsCutoutModels").get().asFile.absolutePath
    )
}
tasks.matching { it.name == "mergeDebugAndroidTestAssets" }
    .configureEach { dependsOn(prepareDocsCutoutModels) }

dependencies {
    implementation(project(":app:docs:showcase"))
    implementation(project(":app:core:ui"))
    implementation(project(":app:core:domain"))
    implementation(libs.compose.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // espresso pulls in androidx.test:runner — the AndroidJUnitRunner classes the
    // instrumentation APK needs.
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.compose.activity)
    androidTestImplementation(libs.compose.ui)
    androidTestImplementation(libs.compose.foundation)
    androidTestImplementation(libs.compose.material3)
}
