import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

// Thin desktop shell — WIP. Consumes :app:commonApp's desktop target, which for
// now only carries the BuildKonfig/BuildConfig shim; screens and services
// migrate into commonMain incrementally.
kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.cmp.material3)
                implementation(project(":app:commonApp"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.dot.gallery.desktop.MainKt"
    }
}
