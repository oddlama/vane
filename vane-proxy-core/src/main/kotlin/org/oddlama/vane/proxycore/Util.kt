package org.oddlama.vane.proxycore

import java.util.*

/** Utility helpers used by proxy-core. */
object Util {
    /**
     * Adds [i] to [uuid] by incrementing the least-significant bits with carry handling.
     *
     * @param uuid base UUID.
     * @param i increment value.
     * @return incremented UUID.
     */
    fun addUuid(uuid: UUID, i: Long): UUID {
        var msb = uuid.mostSignificantBits
        var lsb = uuid.leastSignificantBits

        lsb += i
        if (lsb < uuid.leastSignificantBits) {
            msb++
        }

        return UUID(msb, lsb)
    }
}
