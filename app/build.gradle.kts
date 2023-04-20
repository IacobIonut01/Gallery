import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser

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

tasks.register("generateBp") {
    val project = project(":app")
    val configuration = project.configurations["compatDebugRuntimeClasspath"]

    val libsBase = File("${project.projectDir.absolutePath}/libs")
    libsBase.deleteRecursively()

    val moduleString = { it: ModuleVersionIdentifier -> "${it.group}:${it.name}:${it.version}" }
    val modulePath =
        { it: ModuleVersionIdentifier -> "${it.group.replace(".", "/")}/${it.name}/${it.version}" }

    val spaces = { it: Int ->
        var ret = ""
        for (i in it downTo 1) {
            ret += ' '
        }
        ret
    }

    val moduleName = { it: Any ->
        when (it) {
            is ModuleVersionIdentifier -> {
                "${rootProject.name}_${it.group}_${it.name}"
            }
            is String -> {
                if (it.contains(":")) {
                    val (group, artifactId) = it.split(":")
                    "${rootProject.name}_${group}_${artifactId}"
                } else {
                    "${rootProject.name}_${it}"
                }
            }
            else -> {
                throw Exception("Invalid `it` type")
            }
        }
    }

    val moduleNameAosp = { it: String ->
        when (it) {
            "androidx.constraintlayout:constraintlayout" -> "androidx-constraintlayout_constraintlayout"
            "com.google.auto.value:auto-value-annotations" -> "auto_value_annotations"
            "com.google.guava:guava" -> "guava"
            "com.google.guava:listenablefuture" -> "guava"
            "org.jetbrains.kotlin:kotlin-stdlib" -> "kotlin-stdlib"
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7" -> "kotlin-stdlib-jdk7"
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8" -> "kotlin-stdlib-jdk8"
            "org.jetbrains.kotlinx:kotlinx-coroutines-android" -> "kotlinx-coroutines-android"
            "org.jetbrains.kotlinx:kotlinx-coroutines-core" -> "kotlinx-coroutines-core"
            else -> it.replace(":", "_")
        }
    }

    val isAvailableInAosp = { group: String, artifactId: String ->
        when {
            group.contains("org.jetbrains") -> {
                !artifactId.contains("parcelize") && !artifactId.contains("extensions-runtime")
            }
            group == "com.google.auto.value" -> true
            group == "com.google.guava" -> true
            group == "junit" -> true
            else -> false
        }
    }

    val shouldSkip = { group: String, artifactId: String ->
        when {
            artifactId.contains("compose-bom") -> true
            else -> false
        }
    }
    // Update app/Android.bp
    File("${project.projectDir.absolutePath}/Android.bp").let { file ->
        // Read dependencies
        val dependencies = "${spaces(8)}// DO NOT EDIT THIS SECTION MANUALLY\n".plus(
            configuration.allDependencies.joinToString("\n") {
                if (!shouldSkip(it.group!!, it.name)) {
                    if (isAvailableInAosp(it.group!!, it.name)) {
                        "${spaces(8)}\"${moduleNameAosp("${it.group}:${it.name}")}\","
                    } else {
                        "${spaces(8)}\"${moduleName("${it.group}:${it.name}")}\","
                    }
                } else "${spaces(8)}// SKIPPED \"${moduleName("${it.group}:${it.name}")}\""
            }
        )

        // Replace existing dependencies with newly generated ones
        file.writeText(
            file.readText().replace(
                "static_libs: \\[.*?\\]".toRegex(RegexOption.DOT_MATCHES_ALL),
                "static_libs: [%s]".format("\n$dependencies\n${spaces(4)}")
            )
        )
    }

    // Update app/libs
    configuration.resolvedConfiguration.resolvedArtifacts.sortedBy {
        moduleString(it.moduleVersion.id)
    }.distinctBy {
        moduleString(it.moduleVersion.id)
    }.forEach {
        val id = it.moduleVersion.id

        // Skip modules that are available in AOSP
        if (isAvailableInAosp(id.group, it.name)) {
            return@forEach
        }

        // Get file path
        val dirPath = "${libsBase}/${modulePath(id)}"
        val filePath = "${dirPath}/${it.file.name}"

        // Copy artifact to app/libs
        it.file.copyTo(File(filePath))

        // Parse dependencies
        val dependencies =
            it.file.parentFile.parentFile.walk().filter { file -> file.extension == "pom" }
                .map { file ->
                    val ret = mutableListOf<String>()

                    val pom = XmlParser().parse(file)
                    val dependencies = (pom["dependencies"] as NodeList).firstOrNull() as Node?

                    dependencies?.children()?.forEach { node ->
                        val dependency = node as Node
                        ret.add(
                            "${
                                (dependency.get("groupId") as NodeList).text()
                            }:${
                                (dependency.get("artifactId") as NodeList).text()
                            }"
                        )
                    }

                    ret
                }.flatten()

        var targetSdkVersion = android.defaultConfig.targetSdk
        var minSdkVersion = 14

        // Extract AndroidManifest.xml for AARs
        if (it.file.extension == "aar") {
            copy {
                from(zipTree(filePath).matching { include("/AndroidManifest.xml") }.singleFile)
                into(dirPath)
            }

            val androidManifest = XmlParser().parse(File("${dirPath}/AndroidManifest.xml"))

            val usesSdk = (androidManifest["uses-sdk"] as NodeList).first() as Node
            targetSdkVersion = (usesSdk.get("@targetSdkVersion") as Int?) ?: targetSdkVersion
            minSdkVersion = (usesSdk.get("@minSdkVersion") as Int?) ?: minSdkVersion
        }

        // Write Android.bp
        File("$libsBase/Android.bp").let { file ->
            // Add autogenerated header if file is empty
            if (file.length() == 0L) {
                file.writeText("// DO NOT EDIT THIS FILE MANUALLY")
            }

            val formatDeps = { addNoDeps: Boolean ->
                val deps = dependencies.filter { dep ->
                    when {
                        configuration.resolvedConfiguration.resolvedArtifacts.firstOrNull { artifact ->
                            dep == "${artifact.moduleVersion.id.group}:${artifact.moduleVersion.id.name}"
                        } == null -> {
                            val moduleName = if (addNoDeps) {
                                moduleName(id)
                            } else {
                                "${moduleName(id)}-nodeps"
                            }
                            println("$moduleName: Skipping $dep because it's not in resolvedArtifacts")
                            false
                        }
                        dep == "org.jetbrains.kotlin:kotlin-stdlib-common" -> false
                        else -> true
                    }
                }.distinct().toMutableList()

                if (addNoDeps) {
                    // Add -nodeps dependency for android_library/java_library_static
                    deps.add(0, "${id.group}_${id.name}-nodeps")
                }

                var ret = ""

                if (deps.isNotEmpty()) {
                    deps.forEach { dep ->
                        ret += if (dep.contains(":")) {
                            val (group, artifactId) = dep.split(":")
                            if (isAvailableInAosp(group, artifactId)) {
                                "\n${spaces(8)}\"${moduleNameAosp(dep)}\","
                            } else {
                                "\n${spaces(8)}\"${moduleName(dep)}\","
                            }
                        } else {
                            "\n${spaces(8)}\"${moduleName(dep)}\","
                        }
                    }
                    ret += "\n${spaces(4)}"
                }

                ret
            }

            when (it.extension) {
                "aar" -> {
                    file.appendText(
                        """

                            android_library_import {
                                name: "${moduleName(id)}-nodeps",
                                aars: ["${modulePath(id)}/${it.file.name}"],
                                sdk_version: "$targetSdkVersion",
                                min_sdk_version: "$minSdkVersion",
                                apex_available: [
                                    "//apex_available:platform",
                                    "//apex_available:anyapex",
                                ],
                                static_libs: [%s],
                            }

                            android_library {
                                name: "${moduleName(id)}",
                                sdk_version: "$targetSdkVersion",
                                min_sdk_version: "$minSdkVersion",
                                apex_available: [
                                    "//apex_available:platform",
                                    "//apex_available:anyapex",
                                ],
                                manifest: "${modulePath(id)}/AndroidManifest.xml",
                                static_libs: [%s],
                                java_version: "1.7",
                            }

                        """.trimIndent().format(formatDeps(false), formatDeps(true))
                    )
                }
                "jar" -> {
                    file.appendText(
                        """

                            java_import {
                                name: "${moduleName(id)}-nodeps",
                                jars: ["${modulePath(id)}/${it.file.name}"],
                                sdk_version: "$targetSdkVersion",
                                min_sdk_version: "$minSdkVersion",
                                apex_available: [
                                    "//apex_available:platform",
                                    "//apex_available:anyapex",
                                ],
                            }

                            java_library_static {
                                name: "${moduleName(id)}",
                                sdk_version: "$targetSdkVersion",
                                min_sdk_version: "$minSdkVersion",
                                apex_available: [
                                    "//apex_available:platform",
                                    "//apex_available:anyapex",
                                ],
                                static_libs: [%s],
                                java_version: "1.7",
                            }

                        """.trimIndent().format(formatDeps(true))
                    )
                }
                else -> throw Exception("Unknown file extension: ${it.extension}")
            }
        }
    }
}