/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Optional password-based encryption for the whole backup archive.
 *
 * Layout: MAGIC(6) | SALT(16) | BASE_NONCE(12) | CHUNK_SIZE(4) | [LEN(4) + GCM_CHUNK]*
 *
 * The plaintext (a ZIP archive) is split into [CHUNK_SIZE] blocks, each independently
 * encrypted with AES-256-GCM using a per-chunk nonce derived from [BASE_NONCE] + index,
 * so memory stays bounded regardless of backup size. The key is derived from the user
 * password with PBKDF2-HMAC-SHA256.
 */
object BackupCrypto {

    private val MAGIC = "RFBKP1".toByteArray() // 6 bytes
    const val MAGIC_SIZE = 6
    private const val SALT_LEN = 16
    private const val NONCE_LEN = 12
    private const val CHUNK_SIZE = 1 * 1024 * 1024 // 1 MiB
    private const val PBKDF2_ITERATIONS = 120_000
    private const val PBKDF2_KEY_LENGTH = 256

    /** Returns true if [header] (the first [MAGIC_SIZE] bytes of a file) marks an encrypted backup. */
    fun isEncrypted(header: ByteArray): Boolean =
        header.size >= MAGIC_SIZE && header.copyOf(MAGIC_SIZE).contentEquals(MAGIC)

    /** Reads up to [count] header bytes from [input] without consuming more than needed. */
    fun readHeader(input: InputStream, count: Int = MAGIC_SIZE): ByteArray {
        val buf = ByteArray(count)
        val read = readFully(input, buf)
        return if (read == count) buf else buf.copyOf(read)
    }

    /** Encrypts the [input] stream into [output] using [password]. Does not close [output]. */
    fun encrypt(input: InputStream, output: OutputStream, password: String) {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val baseNonce = ByteArray(NONCE_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        val dos = DataOutputStream(output)
        dos.write(MAGIC)
        dos.write(salt)
        dos.write(baseNonce)
        dos.writeInt(CHUNK_SIZE)

        val buf = ByteArray(CHUNK_SIZE)
        var index = 0L
        while (true) {
            val n = readFully(input, buf)
            if (n == 0) break
            val nonce = deriveChunkNonce(baseNonce, index++)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
            val ct = cipher.doFinal(buf, 0, n)
            dos.writeInt(ct.size)
            dos.write(ct)
            if (n < CHUNK_SIZE) break
        }
        dos.flush()
    }

    /**
     * Decrypts an encrypted backup [input] stream into [output] using [password].
     * Throws [BackupPasswordException] if the password is wrong (GCM tag mismatch).
     * Does not close [output].
     */
    fun decrypt(input: InputStream, output: OutputStream, password: String) {
        val dis = DataInputStream(input)
        val magic = ByteArray(MAGIC_SIZE)
        dis.readFully(magic)
        require(magic.contentEquals(MAGIC)) { "Not an encrypted backup" }
        val salt = ByteArray(SALT_LEN)
        dis.readFully(salt)
        val baseNonce = ByteArray(NONCE_LEN)
        dis.readFully(baseNonce)
        @Suppress("UNUSED_VARIABLE")
        val chunkSize = dis.readInt()
        val key = deriveKey(password, salt)

        var index = 0L
        while (true) {
            val len = try {
                dis.readInt()
            } catch (_: EOFException) {
                break
            }
            val ct = ByteArray(len)
            dis.readFully(ct)
            val nonce = deriveChunkNonce(baseNonce, index++)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
            val plain = try {
                cipher.doFinal(ct)
            } catch (e: Exception) {
                // AEADBadTagException etc. → wrong password or corrupted data
                throw BackupPasswordException()
            }
            output.write(plain)
        }
        output.flush()
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun deriveChunkNonce(base: ByteArray, index: Long): ByteArray {
        val nonce = base.copyOf()
        var i = nonce.size - 1
        var idx = index
        var b = 0
        while (b < 8 && i >= 0) {
            nonce[i] = (nonce[i].toInt() xor (idx and 0xFF).toInt()).toByte()
            idx = idx ushr 8
            i--
            b++
        }
        return nonce
    }

    private fun readFully(input: InputStream, buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val r = input.read(buf, total, buf.size - total)
            if (r < 0) break
            total += r
        }
        return total
    }
}

/** Thrown when an encrypted backup cannot be decrypted with the supplied password. */
class BackupPasswordException : Exception("Incorrect backup password")
