package com.itantra.app.communication

import com.itantra.app.communication.packet.PacketFragmenter
import com.itantra.app.communication.packet.iMFPPacket
import com.itantra.app.domain.model.AppLanguage
import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.StandardCharsets

class PacketFragmenterTest {

    @Test
    fun testSmallPacketDoesNotFragment() {
        val fragmenter = PacketFragmenter(mtu = 256)
        val packet = iMFPPacket(
            sequenceNumber = 1,
            sourceNodeId = 0x01.toShort(),
            payload = "Small message".toByteArray(StandardCharsets.UTF_8)
        )

        val fragments = fragmenter.fragment(packet)
        assertEquals(1, fragments.size)

        val reassembled = fragmenter.handleIncomingFragment(fragments[0])
        assertNotNull(reassembled)
        assertEquals(packet.sequenceNumber, reassembled!!.sequenceNumber)
    }

    @Test
    fun testLargePacketFragmentationAndReassembly() {
        val fragmenter = PacketFragmenter(mtu = 64) // Small MTU to force fragmentation
        val largeText = "వరద నీరు చాలా వేగంగా పెరుగుతోంది. రక్షణ బృందాలు వెంటనే 4వ సెక్టార్ వద్దకు చేరుకోవాలి. ఆహార ప్యాకెట్లు మరియు తాగునీరు అత్యవసరం."
        val packet = iMFPPacket(
            sequenceNumber = 99,
            sourceNodeId = 0x0A.toShort(),
            language = AppLanguage.TELUGU,
            payload = largeText.toByteArray(StandardCharsets.UTF_8)
        )

        val fragments = fragmenter.fragment(packet)
        assertTrue(fragments.size > 1)

        // Deliver fragments in reverse order to test out-of-order reassembly
        var finalPacket: iMFPPacket? = null
        for (f in fragments.reversed()) {
            finalPacket = fragmenter.handleIncomingFragment(f)
        }

        assertNotNull("Reassembly must succeed even when fragments arrive out of order", finalPacket)
        val decodedText = String(finalPacket!!.payload, StandardCharsets.UTF_8)
        assertEquals(largeText, decodedText)
    }
}
