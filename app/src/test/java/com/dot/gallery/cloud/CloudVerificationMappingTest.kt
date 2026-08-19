package com.dot.gallery.cloud

import com.dot.gallery.cloud.ui.verifiedItemsByIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudVerificationMappingTest {

    @Test
    fun keepsVerificationIndexesAlignedWhenEarlierItemsAreMissing() {
        val hashedItems = listOf("second", "third")
        val result = mapOf("0" to true, "1" to false)

        assertEquals(listOf("second"), verifiedItemsByIndex(hashedItems, result))
    }

    @Test
    fun ignoresMissingAndUnexpectedVerificationIndexes() {
        val items = listOf("first", "second")
        val result = mapOf("1" to true, "9" to true)

        assertEquals(listOf("second"), verifiedItemsByIndex(items, result))
    }
}
