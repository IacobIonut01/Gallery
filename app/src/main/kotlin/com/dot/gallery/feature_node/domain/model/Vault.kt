package com.dot.gallery.feature_node.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.UUID

@Parcelize
data class Vault(
    val uuid: UUID = UUID.randomUUID(),
    val name: String
): Parcelable, Serializable
