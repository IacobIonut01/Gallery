import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class ApkVersioningPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "apkVersioning",
            ApkVersioningExtension::class.java
        )

        extension.flavorVersionCodes.convention(emptyMap())
        extension.versionCodeMultiplier.convention(10)
        extension.variables.convention(emptyMap())

        project.pluginManager.withPlugin("com.android.application") {
            val androidComponents = project.extensions.getByType(
                ApplicationAndroidComponentsExtension::class.java
            )
            val android = project.extensions.getByType(
                ApplicationExtension::class.java
            )

            androidComponents.onVariants { variant ->
                val codes = extension.flavorVersionCodes.get()
                val multiplier = extension.versionCodeMultiplier.get()
                val baseVersionCode = android.defaultConfig.versionCode ?: 0
                val versionName = android.defaultConfig.versionName ?: ""
                val flavorName = variant.flavorName ?: ""
                val buildType = variant.buildType ?: "release"

                // Only override versionCode when flavor codes are configured
                val computedVersionCode = if (codes.isNotEmpty()) {
                    // Support multi-dimension flavors: sum offsets from each individual
                    // product flavor rather than looking up the combined flavorName.
                    val flavorOffset = variant.productFlavors
                        .sumOf { (_, name) -> codes[name] ?: 0 }
                    val code = flavorOffset + baseVersionCode * multiplier
                    variant.outputs.forEach { output ->
                        output.versionCode.set(code)
                    }
                    code
                } else {
                    baseVersionCode
                }

                // Only rename APK when outputFileName is explicitly set
                if (extension.outputFileName.isPresent) {
                    // Per-dimension flavor placeholders (e.g. {abi}, {ml})
                    val dimensionVars = variant.productFlavors.associate { (dimension, name) ->
                        dimension to name
                    }
                    val builtins = mapOf(
                        "versionName" to versionName,
                        "versionCode" to computedVersionCode.toString(),
                        "flavorName" to flavorName,
                        "buildType" to buildType
                    ) + dimensionVars
                    val allVars = builtins + extension.variables.get()
                    val resolvedName = allVars.entries.fold(extension.outputFileName.get()) { name, (key, value) ->
                        name.replace("{$key}", value)
                    }

                    // Read APK directory without transforming the artifact pipeline.
                    // Using get() instead of toTransformMany() keeps baseline profiles
                    // and other AGP-managed companion files intact.
                    val apkDir = variant.artifacts.get(SingleArtifact.APK)

                    val renameTask = project.tasks.register(
                        "renameApkFor${variant.name}",
                        RenameApkTask::class.java
                    ) {
                        inputDir.set(apkDir)
                        newFileName.set(resolvedName)
                        outputDir.set(
                            project.layout.buildDirectory.dir(
                                "outputs/apk-renamed/$flavorName/$buildType"
                            )
                        )
                    }

                    // Run the rename task as part of the assemble lifecycle
                    val capitalizedName = variant.name.replaceFirstChar { c -> c.uppercase() }
                    project.tasks.configureEach {
                        if (name == "assemble$capitalizedName") {
                            finalizedBy(renameTask)
                        }
                    }
                }
            }
        }
    }
}
