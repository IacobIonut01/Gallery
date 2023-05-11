import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("androidx.baselineprofile")
}

val versionMajor = 1
val versionMinor = 0
val versionPatch = 4
val versionBuild = 18

android {
    namespace = "com.dot.gallery"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.dot.gallery"
        minSdk = 30
        targetSdk = 33
        versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
        archivesName.set("Gallery-$versionName")
    }

    lintOptions {
        baseline(file("lint-baseline.xml"))
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "MAPS_TOKEN", getApiKey())
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "Gallery Debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appProvider"] = "com.dot.gallery.debug.media_provider"
            buildConfigField("String", "CONTENT_AUTHORITY", "\"com.dot.gallery.debug.media_provider\"")
        }
        getByName("release") {
            manifestPlaceholders += mapOf()
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "MAPS_TOKEN", getApiKey())
            manifestPlaceholders["appName"] = "Gallery"
            manifestPlaceholders["appProvider"] = "com.dot.gallery.media_provider"
            buildConfigField("String", "CONTENT_AUTHORITY", "\"com.dot.gallery.media_provider\"")
        }
        create("staging") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            manifestPlaceholders["appName"] = "Gallery Staging"
            manifestPlaceholders["appProvider"] = "com.dot.gallery.staging.media_provider"
            buildConfigField("String", "CONTENT_AUTHORITY", "\"com.dot.staging.debug.media_provider\"")
        }
    }
    flavorDimensions += "version"

    productFlavors {
        create("system") {
            dimension = "version"
        }
        create("compat") {
            dimension = "version"
        }
    }

    sourceSets {
        getByName("system").java.setSrcDirs(listOf("src/common/java", "src/system/java"))
        getByName("compat").java.setSrcDirs(listOf("src/common/java", "src/compat/java"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    "baselineProfile"(project(mapOf("path" to ":baselineprofile")))
    val bom = "2023.05.01"
    val lifecycleVersion = "2.6.1"
    val material3Version = "1.1.0"
    val accompanistVersion = "0.31.0-alpha"
    val kotlinCoroutinesVersion = "1.6.4"
    val hiltVersion = "2.45"
    val roomVersion = "2.5.1"
    val glideVersion = "4.15.1"
    val media3Version = "1.0.1"

    // Core
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.navigation:navigation-runtime-ktx:2.5.3")

    // Core - Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    // Compose
    implementation("androidx.activity:activity-compose:1.7.1")
    implementation(platform("androidx.compose:compose-bom:$bom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose - Material3
    implementation("androidx.compose.material3:material3:$material3Version")
    implementation("androidx.compose.material3:material3-window-size-class:$material3Version")

    // Compose - Accompanists
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-navigation-animation:$accompanistVersion")

    // Android MDC - Material
    implementation("com.google.android.material:material:1.9.0")

    // Kotlin - Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinCoroutinesVersion")

    // Dagger - Hilt
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0-alpha01")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")
    kapt("androidx.hilt:hilt-compiler:1.0.0")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$roomVersion")

    // Glide
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    implementation("com.github.bumptech.glide:compose:1.0.0-alpha.3")
    kapt("com.github.bumptech.glide:compiler:$glideVersion")

    // SVG Support for Glide
    implementation("com.github.qoqa:glide-svg:4.0.2")

    // Exo Player
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // Exifinterface
    implementation("androidx.exifinterface:exifinterface:1.3.6")

    // Compose-Extended-Gestures
    implementation("com.github.SmartToolFactory:Compose-Extended-Gestures:3.0.0")

    // Datastore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.0-alpha04")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:$bom"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

fun getApiKey(): String {
    val fl = rootProject.file("api.properties")

    return try {
        val properties = Properties()
        properties.load(FileInputStream(fl))
        properties.getProperty("MAPS_TOKEN")
    } catch (e: Exception) {
        "DEBUG"
    }
}