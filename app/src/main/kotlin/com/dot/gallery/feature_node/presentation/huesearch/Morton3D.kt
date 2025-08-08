package com.dot.gallery.feature_node.presentation.huesearch

object Morton3D {
    /** Spread the low 21 bits of v so there are two zeros between each bit. */
    private fun part1By2(v: Int): Long {
        var x = v.toLong() and 0x1FFFFFL
        x = (x or (x shl 32)) and 0x1F00000000FFFFL
        x = (x or (x shl 16)) and 0x1F0000FF0000FFL
        x = (x or (x shl 8))  and 0x100F00F00F00F00FL
        x = (x or (x shl 4))  and 0x10C30C30C30C30C3L
        x = (x or (x shl 2))  and 0x1249249249249249L
        return x
    }

    /** Compute a 3D Morton code from integer grid coords gx, gy, gz. */
    fun encode(gx: Int, gy: Int, gz: Int): Long {
        return (part1By2(gx)      ) or
                (part1By2(gy) shl 1) or
                (part1By2(gz) shl 2)
    }

    /** Given grid coords and a neighbor radius r, return all neighbor codes within ±r */
    fun neighbors(gx: Int, gy: Int, gz: Int, r: Int = 2): List<Long> {
        val list = ArrayList<Long>()
        for (dx in -r..r) for (dy in -r..r) for (dz in -r..r) {
            list += encode(gx+dx, gy+dy, gz+dz)
        }
        return list
    }
}
