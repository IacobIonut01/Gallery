package com.dot.gallery.core

/**
 * Application identity installed once by the host app ([GalleryApp]) so module
 * code can reference the application id without seeing BuildConfig.
 */
object AppIdentity {
    var applicationId: String = ""

    /**
     * Mirrors the app's per-flavor `BuildConfig.ALLOW_ALL_FILES_ACCESS` gate so
     * data-layer code can read it without seeing BuildConfig.
     */
    var allowAllFilesAccess: Boolean = false

    /**
     * Mirrors the app's per-flavor `BuildConfig.ML_MODELS_BUNDLED` flag so
     * model-management code can read it without seeing BuildConfig.
     */
    var mlModelsBundled: Boolean = false

    /**
     * Mirrors `BuildConfig.BUILD_TYPE == "release"` so module code can gate
     * debug-only instrumentation without seeing BuildConfig.
     */
    var isReleaseBuild: Boolean = true

    /**
     * Mirrors the app's per-flavor `BuildConfig.CONTENT_AUTHORITY` FileProvider
     * authority so module code can mint shareable URIs without seeing BuildConfig.
     */
    var contentAuthority: String = ""

    /** Mirrors `BuildConfig.VERSION_NAME` for log headers/exports. */
    var versionName: String = ""

    /** Mirrors `BuildConfig.VERSION_CODE` for log headers/exports. */
    var versionCode: Long = 0

    /** Mirrors `BuildConfig.FLAVOR` for diagnostics. */
    var flavor: String = ""

    /** Mirrors `BuildConfig.BUILD_TYPE` for diagnostics. */
    var buildType: String = ""
}
