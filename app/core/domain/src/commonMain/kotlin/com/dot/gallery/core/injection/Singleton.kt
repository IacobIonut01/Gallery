package com.dot.gallery.core.injection

import me.tatarka.inject.annotations.Scope

/**
 * kotlin-inject scope annotation replacing `javax.inject.Singleton` (a JVM-only
 * artifact — unavailable to commonMain/wasmJs). One instance per component
 * graph. All singleton bindings and the component itself must use this
 * annotation — kotlin-inject ties a component to a single scope class.
 */
@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Singleton
