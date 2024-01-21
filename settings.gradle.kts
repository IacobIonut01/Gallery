pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven {
            name = "glide-snapshot"
            setUrl("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven {
            name = "glide-snapshot"
            setUrl("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
}
rootProject.name = "Gallery"
include(":app")
include(":baselineprofile")
include(":libs:gesture")
include(":libs:cropper")