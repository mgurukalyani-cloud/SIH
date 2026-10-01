package com.itantra.app.communication.compression

import com.itantra.app.communication.packet.iMFPPacket.CompressionType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.Inflater

data class CompressionBenchmark(
    val originalSizeBytes: Int,
    val compressedSizeBytes: Int,
    val compressionRatio: Float, // e.g. 0.85 = 15% reduction, > 1.0 = negative savings
    val compressionTimeMs: Long,
    val decompressionTimeMs: Long,
    val isEfficientForShortMessage: Boolean,
    val selectedMethod: CompressionType
)

object TextCompressor {

    fun compress(text: String): Pair<ByteArray, CompressionType> {
        val rawBytes = text.toByteArray(StandardCharsets.UTF_8)
        if (rawBytes.size < 35) {
            // Short messages under ~35 bytes generally expand under Deflate due to dictionary headers
            return Pair(rawBytes, CompressionType.RAW_UTF8)
        }

        val compressed = deflate(rawBytes)
        return if (compressed.size < rawBytes.size) {
            Pair(compressed, CompressionType.DEFLATE)
        } else {
            Pair(rawBytes, CompressionType.RAW_UTF8)
        }
    }

    fun decompress(bytes: ByteArray, type: CompressionType): String {
        return when (type) {
            CompressionType.RAW_UTF8 -> String(bytes, StandardCharsets.UTF_8)
            CompressionType.DEFLATE -> {
                val decompressed = inflate(bytes)
                String(decompressed, StandardCharsets.UTF_8)
            }
            CompressionType.BPE_TOKENS -> String(bytes, StandardCharsets.UTF_8)
        }
    }

    fun benchmark(text: String): CompressionBenchmark {
        val rawBytes = text.toByteArray(StandardCharsets.UTF_8)
        val t0 = System.nanoTime()
        val deflated = deflate(rawBytes)
        val compTimeMs = (System.nanoTime() - t0) / 1_000_000

        val t1 = System.nanoTime()
        inflate(deflated)
        val decompTimeMs = (System.nanoTime() - t1) / 1_000_000

        val ratio = deflated.size.toFloat() / rawBytes.size.toFloat()
        val efficient = deflated.size < rawBytes.size
        val selected = if (efficient) CompressionType.DEFLATE else CompressionType.RAW_UTF8

        return CompressionBenchmark(
            originalSizeBytes = rawBytes.size,
            compressedSizeBytes = if (efficient) deflated.size else rawBytes.size,
            compressionRatio = ratio,
            compressionTimeMs = compTimeMs,
            decompressionTimeMs = decompTimeMs,
            isEfficientForShortMessage = efficient,
            selectedMethod = selected
        )
    }

    private fun deflate(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(input)
        deflater.finish()

        val outputStream = ByteArrayOutputStream(input.size)
        val buffer = ByteArray(128)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        deflater.end()
        return outputStream.toByteArray()
    }

    private fun inflate(input: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(input)

        val outputStream = ByteArrayOutputStream(input.size * 2)
        val buffer = ByteArray(128)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        inflater.end()
        return outputStream.toByteArray()
    }
}
