package moe.nemesiss.hostman.ui

import java.math.BigDecimal
import java.math.RoundingMode

object NetworkSpeed {

    const val KB = 1024L

    const val MB = 1024L * KB

    const val GB = 1024L * MB


    fun format(bytes: Long): String {
        var bi = BigDecimal.valueOf(bytes)
        var unit = "KB"
        // KB
        bi = bi.divide(BigDecimal.valueOf(KB), 3, RoundingMode.HALF_EVEN)
        if (bytes > MB) {
            // MB
            unit = "MB"
            bi = bi.divide(BigDecimal.valueOf(KB), 3, RoundingMode.HALF_EVEN)
        }
        if (bytes > GB) {
            // MB
            unit = "GB"
            bi = bi.divide(BigDecimal.valueOf(KB), 3, RoundingMode.HALF_EVEN)
        }
        // keep 2 digits.
        bi = bi.setScale(2, RoundingMode.HALF_EVEN)
        return bi.stripTrailingZeros().toPlainString() + "${unit}/s"
    }
}