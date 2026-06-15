plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("manifestConfig") {
            id = "manifest-config"
            implementationClass = "ManifestConfigPlugin"
        }
    }
}
