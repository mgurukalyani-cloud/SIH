package com.itantra.app.communication.packet

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

data class FragmentHeader(
    val messageHash: Int,
    val fragmentIndex: Int,
    val totalFragments: Int
) {
    companion object {
        const val SIZE = 6 // 4 bytes hash + 1 byte index + 1 byte total

        fun serialize(hash: Int, index: Int, total: Int): ByteArray {
            val buf = ByteBuffer.allocate(SIZE)
            buf.putInt(hash)
            buf.put(index.toByte())
            buf.put(total.toByte())
            return buf.array()
        }

        fun parse(bytes: ByteArray): FragmentHeader? {
            if (bytes.size < SIZE) return null
            val buf = ByteBuffer.wrap(bytes)
            val hash = buf.getInt()
            val index = buf.get().toInt() and 0xFF
            val total = buf.get().toInt() and 0xFF
            return FragmentHeader(hash, index, total)
        }
    }
}

class PacketFragmenter(
    private val mtu: Int = 256
) {
    private val pendingReassembly = ConcurrentHashMap<Int, MutableMap<Int, ByteArray>>()
    private val reassemblyTimestamps = ConcurrentHashMap<Int, Long>()

    fun fragment(packet: iMFPPacket): List<ByteArray> {
        val rawBytes = packet.toByteArray()
        if (rawBytes.size <= mtu) {
            return listOf(rawBytes)
        }

        val payloadCapacity = mtu - FragmentHeader.SIZE
        val totalFragments = (rawBytes.size + payloadCapacity - 1) / payloadCapacity
        val messageHash = rawBytes.contentHashCode()
        val fragments = mutableListOf<ByteArray>()

        var offset = 0
        for (i in 0 until totalFragments) {
            val len = minOf(payloadCapacity, rawBytes.size - offset)
            val chunk = ByteArray(len)
            System.arraycopy(rawBytes, offset, chunk, 0, len)

            val header = FragmentHeader.serialize(messageHash, i, totalFragments)
            val fullFragment = ByteArray(FragmentHeader.SIZE + len)
            System.arraycopy(header, 0, fullFragment, 0, FragmentHeader.SIZE)
            System.arraycopy(chunk, 0, fullFragment, FragmentHeader.SIZE, len)

            fragments.add(fullFragment)
            offset += len
        }

        return fragments
    }

    fun handleIncomingFragment(bytes: ByteArray): iMFPPacket? {
        // Check if unfragmented iMFP frame
        if (bytes.size >= 2 && bytes[0] == iMFPPacket.MAGIC_1 && bytes[1] == iMFPPacket.MAGIC_2) {
            return iMFPPacket.parse(bytes)
        }

        // Fragmented frame
        val header = FragmentHeader.parse(bytes) ?: return null
        val payloadChunk = ByteArray(bytes.size - FragmentHeader.SIZE)
        System.arraycopy(bytes, FragmentHeader.SIZE, payloadChunk, 0, payloadChunk.size)

        val fragmentsMap = pendingReassembly.computeIfAbsent(header.messageHash) { ConcurrentHashMap() }
        fragmentsMap[header.fragmentIndex] = payloadChunk
        reassemblyTimestamps[header.messageHash] = System.currentTimeMillis()

        if (fragmentsMap.size == header.totalFragments) {
            // All fragments received - reassemble
            val totalSize = (0 until header.totalFragments).sumOf { fragmentsMap[it]?.size ?: 0 }
            val assembledBytes = ByteArray(totalSize)
            var destPos = 0
            for (i in 0 until header.totalFragments) {
                val part = fragmentsMap[i] ?: return null
                System.arraycopy(part, 0, assembledBytes, destPos, part.size)
                destPos += part.size
            }

            pendingReassembly.remove(header.messageHash)
            reassemblyTimestamps.remove(header.messageHash)

            return iMFPPacket.parse(assembledBytes)
        }

        return null
    }

    fun cleanExpiredFragments(timeoutMs: Long = 30000L) {
        val now = System.currentTimeMillis()
        reassemblyTimestamps.entries.removeIf { (hash, time) ->
            if (now - time > timeoutMs) {
                pendingReassembly.remove(hash)
                true
            } else {
                false
            }
        }
    }
}
