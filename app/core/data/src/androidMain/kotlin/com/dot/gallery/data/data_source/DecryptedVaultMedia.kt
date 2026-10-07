@file:Suppress("DEPRECATION")

package com.dot.gallery.data.data_source

import android.content.Context
import android.net.Uri
import android.security.keystore.UserNotAuthenticatedException
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
import androidx.security.crypto.MasterKey
import com.dot.gallery.domain.model.Vault
import com.dot.gallery.domain.model.VaultInfo
import com.dot.gallery.domain.util.toKotlinByteArray
import com.dot.gallery.core.logging.printError
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.uuid.Uuid
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import com.dot.gallery.domain.model.EncryptedMedia
import com.dot.gallery.domain.uri.path
import com.dot.gallery.domain.util.migrate
import com.dot.gallery.domain.util.fromKotlinByteArray
import java.util.UUID

data class DecryptedVaultMedia(
    val bytes: ByteArray?,
    val mimeType: String,
    val tempFile: File? = null
) {
    /** Read bytes from either in-memory array or temp file. */
    fun readBytes(): ByteArray = bytes ?: tempFile?.readBytes() ?: error("No data available")

    /** Open an InputStream over the decrypted content without loading everything into memory. */
    fun openStream(): InputStream = bytes?.inputStream() ?: tempFile?.inputStream() ?: error("No data available")

    /** Clean up temp file if one was created. */
    fun cleanup() { tempFile?.delete() }
}