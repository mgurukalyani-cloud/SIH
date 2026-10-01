package com.itantra.app.communication

import com.itantra.app.communication.compression.TextCompressor
import com.itantra.app.communication.packet.iMFPPacket
import org.junit.Assert.*
import org.junit.Test

class TextCompressorTest {

    @Test
    fun testShortTextDoesNotExpand() {
        val shortText = "సహాయం (Help)"
        val (bytes, type) = TextCompressor.compress(shortText)
        assertEquals(iMFPPacket.CompressionType.RAW_UTF8, type)
        assertEquals(shortText, TextCompressor.decompress(bytes, type))
    }

    @Test
    fun testLongTextDeflateCompressesEffectively() {
        val longText = "బాధిత ప్రాంతాలలో తక్షణమే రక్షణ చర్యలు ప్రారంభించాలి. ఆహారం, తాగునీరు, మందులు మరియు వైద్య సహాయం వెంటనే అందించాలి. బాధిత ప్రాంతాలలో తక్షణమే రక్షణ చర్యలు ప్రారంభించాలి."
        val (bytes, type) = TextCompressor.compress(longText)

        val decompressed = TextCompressor.decompress(bytes, type)
        assertEquals(longText, decompressed)
    }

    @Test
    fun testBenchmarkProducesValidMetrics() {
        val text = "Emergency alert: All units move to Sector 7. Repeat, move to Sector 7."
        val benchmark = TextCompressor.benchmark(text)

        assertTrue(benchmark.originalSizeBytes > 0)
        assertTrue(benchmark.compressedSizeBytes > 0)
        assertTrue(benchmark.compressionRatio > 0f)
        assertTrue(benchmark.compressionTimeMs >= 0)
    }
}
