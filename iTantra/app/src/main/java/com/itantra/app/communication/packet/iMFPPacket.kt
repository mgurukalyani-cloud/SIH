package com.itantra.app.communication.packet

import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.domain.model.MessagePriority
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * iTantra Micro-Framing Protocol (iMFP) Frame.
 * Designed for ultra-low bitrate tactical links (LoRa 1.2 kbps, BLE Coded PHY, HF/VHF).
 *
 * Header: 14 Bytes Fixed
 * [0..1]   : Magic Delimiter 0x7E 0x1A
 * [2]      : Version (3 bits) | Priority (2 bits) | Compression (2 bits) | IsAck (1 bit)
 * [3]      : Language ID (4 bits) | TTL / Fragment info (4 bits)
 * [4..5]   : Sequence Counter (16 bits)
 * [6..7]   : Source Node ID (16-bit hash)
 * [8..9]   : Destination Node ID (16-bit hash, 0xFFFF = Broadcast)
 * [10..11] : Payload Length M (16 bits)
 * [12..13] : CRC-16 Checksum (Calculated over Header[2..11] + Payload)
 * [14..14+M]: Compact Text Payload (UTF-8 or Compressed)
 */
data class iMFPPacket(
    val version: Int = 1,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val compressionType: CompressionType = CompressionType.RAW_UTF8,
    val isAck: Boolean = false,
    val language: AppLanguage = AppLanguage.TELUGU,
    val ttl: Int = 5,
    val sequenceNumber: Int,
    val sourceNodeId: Short,
    val destNodeId: Short = 0xFFFF.toShort(),
    val payload: ByteArray
) {
    enum class CompressionType(val value: Int) {
        RAW_UTF8(0),
        DEFLATE(1),
        BPE_TOKENS(2)
    }

    val totalFrameSize: Int get() = HEADER_SIZE + payload.size

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE + payload.size).order(ByteOrder.BIG_ENDIAN)

        // Magic bytes
        buffer.put(MAGIC_1)
        buffer.put(MAGIC_2)

        // Control Byte 1
        val ackBit = if (isAck) 1 else 0
        val ctrl1 = ((version and 0x07) shl 5) or
                    ((priority.value and 0x03) shl 3) or
                    ((compressionType.value and 0x03) shl 1) or
                    (ackBit and 0x01)
        buffer.put(ctrl1.toByte())

        // Control Byte 2
        val ctrl2 = ((language.id and 0x0F) shl 4) or (ttl and 0x0F)
        buffer.put(ctrl2.toByte())

        // Sequence
        buffer.putShort(sequenceNumber.toShort())

        // Node IDs
        buffer.putShort(sourceNodeId)
        buffer.putShort(destNodeId)

        // Payload Length
        buffer.putShort(payload.size.toShort())

        // CRC-16 Calculation over ctrl1, ctrl2, seq, src, dst, len, and payload
        val crcData = ByteBuffer.allocate(10 + payload.size).order(ByteOrder.BIG_ENDIAN)
        crcData.put(ctrl1.toByte())
        crcData.put(ctrl2.toByte())
        crcData.putShort(sequenceNumber.toShort())
        crcData.putShort(sourceNodeId)
        crcData.putShort(destNodeId)
        crcData.putShort(payload.size.toShort())
        crcData.put(payload)

        val crc16 = CRC16.calculate(crcData.array())
        buffer.putShort(crc16.toShort())

        // Payload data
        buffer.put(payload)

        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as iMFPPacket
        if (sequenceNumber != other.sequenceNumber) return false
        if (sourceNodeId != other.sourceNodeId) return false
        if (destNodeId != other.destNodeId) return false
        return payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = sequenceNumber
        result = 31 * result + sourceNodeId
        result = 31 * result + destNodeId
        result = 31 * result + payload.contentHashCode()
        return result
    }

    companion object {
        const val HEADER_SIZE = 14
        const val MAGIC_1: Byte = 0x7E
        const val MAGIC_2: Byte = 0x1A

        fun parse(bytes: ByteArray): iMFPPacket? {
            if (bytes.size < HEADER_SIZE) return null
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

            if (buffer.get() != MAGIC_1 || buffer.get() != MAGIC_2) {
                return null // Corrupted magic delimiter
            }

            val ctrl1 = buffer.get().toInt() and 0xFF
            val version = (ctrl1 shr 5) and 0x07
            val prioVal = (ctrl1 shr 3) and 0x03
            val compVal = (ctrl1 shr 1) and 0x03
            val isAck = (ctrl1 and 0x01) == 1

            val ctrl2 = buffer.get().toInt() and 0xFF
            val langId = (ctrl2 shr 4) and 0x0F
            val ttl = ctrl2 and 0x0F

            val seq = buffer.getShort().toInt() and 0xFFFF
            val src = buffer.getShort()
            val dst = buffer.getShort()
            val payloadLen = buffer.getShort().toInt() and 0xFFFF
            val receivedCrc = buffer.getShort().toInt() and 0xFFFF

            if (bytes.size < HEADER_SIZE + payloadLen) {
                return null // Truncated frame
            }

            val payload = ByteArray(payloadLen)
            buffer.get(payload)

            // Validate CRC-16 Checksum
            val crcData = ByteBuffer.allocate(10 + payloadLen).order(ByteOrder.BIG_ENDIAN)
            crcData.put(ctrl1.toByte())
            crcData.put(ctrl2.toByte())
            crcData.putShort(seq.toShort())
            crcData.putShort(src)
            crcData.putShort(dst)
            crcData.putShort(payloadLen.toShort())
            crcData.put(payload)

            val computedCrc = CRC16.calculate(crcData.array())
            if (computedCrc != receivedCrc) {
                return null // CRC check failed - packet corrupted over air link
            }

            return iMFPPacket(
                version = version,
                priority = MessagePriority.fromValue(prioVal),
                compressionType = CompressionType.entries.firstOrNull { it.value == compVal } ?: CompressionType.RAW_UTF8,
                isAck = isAck,
                language = AppLanguage.fromId(langId),
                ttl = ttl,
                sequenceNumber = seq,
                sourceNodeId = src,
                destNodeId = dst,
                payload = payload
            )
        }
    }
}
