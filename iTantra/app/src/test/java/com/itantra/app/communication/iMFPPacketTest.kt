package com.itantra.app.communication

import com.itantra.app.communication.packet.CRC16
import com.itantra.app.communication.packet.iMFPPacket
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.domain.model.MessagePriority
import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.StandardCharsets

class iMFPPacketTest {

    @Test
    fun testPacketSerializationAndParsingRoundtrip() {
        val payload = "వరద నీరు పెరుగుతోంది, సహాయం కావాలి".toByteArray(StandardCharsets.UTF_8)
        val original = iMFPPacket(
            version = 1,
            priority = MessagePriority.EMERGENCY,
            compressionType = iMFPPacket.CompressionType.RAW_UTF8,
            isAck = false,
            language = AppLanguage.TELUGU,
            sequenceNumber = 42,
            sourceNodeId = 0x1234.toShort(),
            destNodeId = 0x5678.toShort(),
            payload = payload
        )

        val bytes = original.toByteArray()
        assertEquals(14 + payload.size, bytes.size)
        assertEquals(iMFPPacket.MAGIC_1, bytes[0])
        assertEquals(iMFPPacket.MAGIC_2, bytes[1])

        val parsed = iMFPPacket.parse(bytes)
        assertNotNull(parsed)
        assertEquals(original.version, parsed!!.version)
        assertEquals(original.priority, parsed.priority)
        assertEquals(original.language, parsed.language)
        assertEquals(original.sequenceNumber, parsed.sequenceNumber)
        assertEquals(original.sourceNodeId, parsed.sourceNodeId)
        assertEquals(original.destNodeId, parsed.destNodeId)
        assertArrayEquals(original.payload, parsed.payload)
    }

    @Test
    fun testCorruptedPacketFailsCRCValidation() {
        val payload = "Test Message".toByteArray(StandardCharsets.UTF_8)
        val packet = iMFPPacket(
            sequenceNumber = 100,
            sourceNodeId = 0x01.toShort(),
            payload = payload
        )

        val bytes = packet.toByteArray()

        // Flip 1 bit in payload (simulate air-link RF noise)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0xFF).toByte()

        val parsed = iMFPPacket.parse(bytes)
        assertNull("Corrupted packet must fail CRC check and return null", parsed)
    }

    @Test
    fun testCrc16Consistency() {
        val data = "Tactical Data".toByteArray(StandardCharsets.UTF_8)
        val crc1 = CRC16.calculate(data)
        val crc2 = CRC16.calculate(data)
        assertEquals(crc1, crc2)
        assertTrue(crc1 in 0..0xFFFF)
    }
}
