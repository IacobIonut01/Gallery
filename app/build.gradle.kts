plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

val versionMajor = 1
val versionMinor = 0
val versionPatch = 2
val versionBuild = 16

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
    }

    lintOptions {
        baseline(file("lint-baseline.xml"))
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            signingConfig = signingConfigs.getByName("debug")
        }
        create("staging") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
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
        kotlinCompilerExtensionVersion = "1.4.2"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.navigation:navigation-runtime-ktx:2.5.3")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")

    // Compose
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3:1.1.0-beta02")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.0-beta02")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.29.2-rc")
    implementation("com.google.accompanist:accompanist-permissions:0.29.2-rc")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.29.2-rc")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Dagger - Hilt
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0-alpha01")
    implementation("com.google.dagger:hilt-android:2.45")
    kapt("com.google.dagger:hilt-android-compiler:2.45")
    kapt("androidx.hilt:hilt-compiler:1.0.0")

    // Room
    implementation("androidx.room:room-runtime:2.5.1")
    kapt("androidx.room:room-compiler:2.5.1")

    // Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:2.5.1")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.github.bumptech.glide:compose:1.0.0-alpha.3")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    // SVG Support for Glide
    implementation("com.github.qoqa:glide-svg:4.0.2")

    // Exo Player
    implementation("androidx.media3:media3-exoplayer:1.0.0")
    implementation("androidx.media3:media3-ui:1.0.0")

    // Exifinterface
    implementation("androidx.exifinterface:exifinterface:1.3.6")

    // Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha05")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}