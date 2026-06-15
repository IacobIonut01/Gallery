import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Removes `<uses-permission>` elements from a merged AndroidManifest.xml.
 *
 * Wired into the build pipeline via AGP's
 * [com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST] transform,
 * so it operates on the **merged** manifest—not the source file.
 */
abstract class StripPermissionsTask : DefaultTask() {

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @get:Input
    abstract val permissionsToRemove: ListProperty<String>

    @TaskAction
    fun transform() {
        var content = mergedManifest.get().asFile.readText()
        for (perm in permissionsToRemove.get()) {
            // Remove the entire <uses-permission .../> element for this permission.
            // Handles both self-closing and any extra attributes (tools:node, etc.).
            val escaped = Regex.escape(perm)
            content = content.replace(
                Regex("""[ \t]*<uses-permission\s[^>]*android:name="$escaped"[^/]*/>\s*\n?"""),
                ""
            )
        }
        updatedManifest.get().asFile.writeText(content)
    }
}
