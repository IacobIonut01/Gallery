/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.trash

import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.isCloud

/**
 * How one item inside a mixed local+cloud removal batch is handled.
 *
 * [TRASHABLE]: the item can go through a recoverable trash path (locals via
 * MediaStore, cloud items whose provider declares a real bin).
 * [DELETE_ONLY]: a cloud item whose provider has no recoverable trash — its
 * `trashAsset` is a permanent delete, so under a TRASH action it is deleted
 * permanently *and the dialog must warn about it* (the same treatment
 * un-trashable SD-card items got before MediaStore supported removable
 * volumes — see `trash_incompatible_*` strings).
 * [BLOCKED]: a cloud item on a read-only account — never mutated.
 */
enum class RemovalClass {
    TRASHABLE,
    DELETE_ONLY,
    BLOCKED
}

/**
 * Pure classifier — [isCloud] is a parameter (not read off the item) so the
 * full truth table is runnable on the plain JVM, where `android.net.Uri` and
 * therefore [Media.isCloud] are unavailable.
 */
fun removalClass(
    isCloud: Boolean,
    isReadOnlyCloud: Boolean,
    providerSupportsTrash: Boolean,
): RemovalClass = when {
    !isCloud -> RemovalClass.TRASHABLE
    isReadOnlyCloud -> RemovalClass.BLOCKED
    providerSupportsTrash -> RemovalClass.TRASHABLE
    else -> RemovalClass.DELETE_ONLY
}

fun Media.removalClass(
    isReadOnlyCloud: Boolean,
    providerSupportsTrash: Boolean,
): RemovalClass = removalClass(isCloud, isReadOnlyCloud, providerSupportsTrash)
