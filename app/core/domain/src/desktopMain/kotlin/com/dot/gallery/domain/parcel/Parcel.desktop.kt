package com.dot.gallery.domain.parcel

/**
 * JVM/desktop has no `android.os.Parcelable` — an empty marker keeps the common
 * models compiling. `Parcelize`/`IgnoredOnParcel` are `@OptionalExpectation`
 * annotations, so they need no actual here.
 */
actual interface Parcelable
