package com.dot.gallery.core.ml

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.security.MessageDigest

class BundledModelInstallTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun validBundledModelAtomicallyReplacesCorruptDestination() {
        val destination = temporaryFolder.newFile("model.onnx")
        val expected = "complete model".toByteArray()
        destination.writeText("partial model")

        installBundledModel(destination, expected.sha256()) { expected.inputStream() }

        assertArrayEquals(expected, destination.readBytes())
        assertFalse(temporaryFolder.root.resolve("model.onnx.bundled.tmp").exists())
    }

    @Test
    fun invalidBundledModelDoesNotReplaceExistingDestination() {
        val destination = temporaryFolder.newFile("model.onnx")
        val existing = "existing model".toByteArray()
        destination.writeBytes(existing)

        assertThrows(IOException::class.java) {
            installBundledModel(destination, "expected model".toByteArray().sha256()) {
                "corrupt model".byteInputStream()
            }
        }

        assertArrayEquals(existing, destination.readBytes())
        assertFalse(temporaryFolder.root.resolve("model.onnx.bundled.tmp").exists())
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }
}
