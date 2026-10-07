package com.dot.gallery.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Which media views a pending removal applies to. The scope mirrors the mutation: a trashed
 * item leaves the regular views but is arriving in the trash view, a restore only leaves the
 * trash view, and a permanent delete leaves every view.
 */
enum class PendingRemovalScope {
    /** Item is gone from every view (permanent delete). */
    EVERYWHERE,

    /** Item leaves regular views but is arriving in the trash view (trash op). */
    NON_TRASH,

    /** Item only leaves the trash view (restore out of trash). */
    TRASH_ONLY
}
