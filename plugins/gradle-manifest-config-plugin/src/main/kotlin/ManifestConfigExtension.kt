import org.gradle.api.provider.ListProperty

/**
 * Configuration DSL for the `manifest-config` plugin.
 *
 * ### Permission stripping
 * [stripPermissions] lists fully-qualified permission names
 * (e.g. `android.permission.INTERNET`) that should be removed from the
 * merged AndroidManifest.xml of every variant.
 *
 * ### Example
 * ```kotlin
 * manifestConfig {
 *     stripPermissions.set(listOf(
 *         "android.permission.INTERNET",
 *         "android.permission.ACCESS_NETWORK_STATE"
 *     ))
 * }
 * ```
 */
abstract class ManifestConfigExtension {
    /** Fully-qualified permission names to remove from the merged manifest. */
    abstract val stripPermissions: ListProperty<String>
}
