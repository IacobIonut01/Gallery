import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin that conditionally strips permissions from the merged manifest.
 *
 * When [ManifestConfigExtension.stripPermissions] is non-empty, a
 * [StripPermissionsTask] is registered for every variant and wired into AGP's
 * merged-manifest transform pipeline.  The task removes the listed
 * `<uses-permission>` elements **after** manifest merging, so source files
 * are never modified.
 */
class ManifestConfigPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "manifestConfig",
            ManifestConfigExtension::class.java
        )
        extension.stripPermissions.convention(emptyList())

        project.pluginManager.withPlugin("com.android.application") {
            val androidComponents = project.extensions.getByType(
                ApplicationAndroidComponentsExtension::class.java
            )

            androidComponents.onVariants { variant ->
                val perms = extension.stripPermissions.get()
                if (perms.isNotEmpty()) {
                    val taskProvider = project.tasks.register(
                        "stripPermissions${variant.name.replaceFirstChar { it.uppercase() }}",
                        StripPermissionsTask::class.java
                    ) {
                        permissionsToRemove.set(perms)
                    }
                    variant.artifacts.use(taskProvider)
                        .wiredWithFiles(
                            StripPermissionsTask::mergedManifest,
                            StripPermissionsTask::updatedManifest
                        )
                        .toTransform(SingleArtifact.MERGED_MANIFEST)
                }
            }
        }
    }
}
