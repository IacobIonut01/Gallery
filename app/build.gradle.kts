import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kspAndroid)
    alias(libs.plugins.roomPlugin)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.baselineProfilePlugin)
    id("kotlin-parcelize")
}

android {
    namespace = "com.dot.gallery"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dot.gallery"
        minSdk = 30
        targetSdk = 34
        versionCode = 21041
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        archivesName.set("Gallery-${versionName}-$versionCode")
        if (getApiKey() == "\"DEBUG\"") {
            archivesName.set("${archivesName.get()}-nomaps")
        }
    }

    lint.baseline = file("lint-baseline.xml")

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "MAPS_TOKEN", getApiKey())
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appProvider"] = "com.dot.gallery.debug.media_provider"
            buildConfigField(
                "String",
                "CONTENT_AUTHORITY",
                "\"com.dot.gallery.debug.media_provider\""
            )
        }
        getByName("release") {
            manifestPlaceholders += mapOf(
                "appProvider" to "com.dot.gallery.media_provider"
            )
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "MAPS_TOKEN", getApiKey())
            buildConfigField("String", "CONTENT_AUTHORITY", "\"com.dot.gallery.media_provider\"")
        }
        create("staging") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            manifestPlaceholders["appProvider"] = "com.dot.staging.debug.media_provider"
            buildConfigField(
                "String",
                "CONTENT_AUTHORITY",
                "\"com.dot.staging.debug.media_provider\""
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true")
        freeCompilerArgs += "-Xcontext-receivers"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    room {
        schemaDirectory("$projectDir/schemas/")
    }
}

dependencies {
    runtimeOnly(libs.androidx.profileinstaller)
    implementation(project(":libs:cropper"))
    "baselineProfile"(project(mapOf("path" to ":baselineprofile")))

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Core - Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.compose.lifecycle.runtime)

    // Compose
    implementation(libs.compose.activity)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)

    // Compose - Shimmer
    implementation(libs.compose.shimmer)
    // Compose - Material3
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)

    // Compose - Accompanists
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.drawablepainter)

    // Android MDC - Material
    implementation(libs.material)

    // Kotlin - Coroutines
    implementation(libs.kotlinx.coroutines.core)
    runtimeOnly(libs.kotlinx.coroutines.android)

    // Dagger - Hilt
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // Kotlin Extensions and Coroutines support for Room
    implementation(libs.room.ktx)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)
    implementation(libs.jxl.coder.coil)
    implementation(libs.coil.network.okhttp)

    // Exo Player
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Exif Interface
    implementation(libs.androidx.exifinterface)

    // Zoomable
    implementation(libs.zoomable)

    // Datastore Preferences
    implementation(libs.datastore.prefs)

    // Fuzzy Search
    implementation(libs.fuzzywuzzy)

    // GPU Image
    implementation(libs.gpuimage)

    // Pinch to zoom
    implementation(libs.pinchzoomgrid)

    // Subsampling
    implementation(libs.zoomable.image.coil)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)
    debugRuntimeOnly(libs.compose.ui.test.manifest)
}

fun getApiKey(): String {
    val fl = rootProject.file("api.properties")

    return try {
        val properties = Properties()
        properties.load(FileInputStream(fl))
        properties.getProperty("MAPS_TOKEN")
    } catch (e: Exception) {
        "\"DEBUG\""
    }
}

@Suppress("UnstableApiUsage")
val gitHeadVersion: String
    get() {
        return providers.exec {
            commandLine("git", "show", "-s", "--format=%h", "HEAD")
        }.standardOutput.asText.get().trim()
    }