import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kspAndroid)
    alias(libs.plugins.roomPlugin)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.baselineProfilePlugin)
    alias(libs.plugins.kotlin.compose.compiler)
    id("kotlin-parcelize")
}

android {
    namespace = "com.dot.gallery"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dot.gallery"
        minSdk = 30
        targetSdk = 34
        versionCode = 30013
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        base.archivesName.set("Gallery-${versionName}-$versionCode")
        if (getApiKey() == "\"DEBUG\"") {
            base.archivesName.set("${base.archivesName.get()}-nomaps")
        }
    }

    lint.baseline = file("lint-baseline.xml")

    signingConfigs {
        create("release") {
            storeFile = file("release_key.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "MAPS_TOKEN", getApiKey())
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appProvider"] = "com.dot.gallery.debug.media_provider"
            buildConfigField("Boolean", "ALLOW_ALL_FILES_ACCESS", allowAllFilesAccess)
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
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("Boolean", "ALLOW_ALL_FILES_ACCESS", allowAllFilesAccess)
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
        freeCompilerArgs += "-Xcontext-receivers"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeCompiler {
        enableStrongSkippingMode = true
        includeSourceInformation = true
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
    implementation(libs.avif.coder.coil)

    // Exo Player
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.compose.video)

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

    // Splashscreen
    implementation(libs.androidx.core.splashscreen)

    // Jetpack Security
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

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

val allowAllFilesAccess: String
    get() {
        val fl = rootProject.file("app.properties")

        return try {
            val properties = Properties()
            properties.load(FileInputStream(fl))
            properties.getProperty("ALL_FILES_ACCESS")
        } catch (e: Exception) {
            "true"
        }
    }

@Suppress("UnstableApiUsage")
val gitHeadVersion: String
    get() {
        return providers.exec {
            commandLine("git", "show", "-s", "--format=%h", "HEAD")
        }.standardOutput.asText.get().trim()
    }