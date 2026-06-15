pluginManagement {
    includeBuild("plugins")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        mavenLocal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal()
        // Vendored prebuilt AARs (e.g. com.gemalto.jp2:jp2-android, no longer published to a live repo)
        flatDir { dirs("$rootDir/app/libs") }
    }
}
rootProject.name = "Gallery"
include(":app")
include(":baselineprofile")
include(":libs:gesture")
include(":libs:cropper")
include(":libs:panoramaviewer")
include(":libs:scrollbar")
include(":ml-models")