package com.itantra.app.communication.packet

/**
 * Standard CRC-16-CCITT (Polynomial 0x1021, Initial Value 0xFFFF).
 * Used across low-bitrate radio links to detect bit errors and frame corruption.
 */
object CRC16 {
    private const val POLYNOMIAL = 0x1021
    private const val INITIAL_VALUE = 0xFFFF

    fun calculate(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
        var crc = INITIAL_VALUE
        for (i in offset until (offset + length)) {
            val b = data[i].toInt() and 0xFF
            crc = crc xor (b shl 8)
            for (j in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor POLYNOMIAL
                } else {
                    crc shl 1
                }
            }
        }
        return crc and 0xFFFF
    }
}
