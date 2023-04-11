/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class Settings(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun apply(body: (SharedPreferences.Editor) -> Unit) {
        val editor = sharedPreferences.edit()
        body.invoke(editor).also {
            editor.apply()
        }
    }

    fun getString(name: String, default: String = ""): String? =
        sharedPreferences.getString(name, default)

    fun getBoolean(name: String, default: Boolean = false): Boolean =
        sharedPreferences.getBoolean(name, default)

    fun getInt(name: String, default: Int = -1): Int =
        sharedPreferences.getInt(name, default)

    companion object {
        object Album {
            const val LAST_SORT = "album_last_sort"
        }

        object Misc {
            const val USER_CHOICE_MEDIA_MANAGER = "use_media_manager"
        }

        private const val PREFERENCE_NAME = "settings"
    }
}