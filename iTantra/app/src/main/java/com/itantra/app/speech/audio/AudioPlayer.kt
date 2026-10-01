package com.itantra.app.speech.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.itantra.app.core.common.AppConstants
import com.itantra.app.core.logging.TacticalLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioPlayer(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
) {
    private var audioTrack: AudioTrack? = null
    private var playJob: Job? = null

    companion object {
        private const val TAG = "AudioPlayer"
    }

    suspend fun playPcm(
        pcmData: ShortArray,
        sampleRate: Int = AppConstants.AUDIO_SAMPLE_RATE,
        onComplete: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        stop()

        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufSize, pcmData.size * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            TacticalLogger.i(TAG, "AudioTrack playing ${pcmData.size} samples at $sampleRate Hz")

            audioTrack?.write(pcmData, 0, pcmData.size)

            // Let audio drain
            val durationMs = (pcmData.size.toFloat() / sampleRate * 1000).toLong()
            kotlinx.coroutines.delay(durationMs + 100)

            onComplete()
        } catch (e: Exception) {
            TacticalLogger.e(TAG, "AudioTrack playback error", e)
        } finally {
            stop()
        }
    }

    fun stop() {
        playJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
    }
}
