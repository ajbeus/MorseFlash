package com.example.morseflash

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Plays pulses as a sine tone. Streaming the samples keeps the timing exact. */
object Beeper {
    private const val RATE = 44100
    private const val FREQ_HZ = 700.0
    private const val CHUNK = RATE / 50 // 20 ms, so Stop reacts quickly

    suspend fun play(pulses: List<Pulse>, unitMs: Long) = withContext(Dispatchers.IO) {
        val minBuf = AudioTrack.getMinBufferSize(
            RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.play()
            var written = 0
            for (p in pulses) {
                val n = (RATE * p.units * unitMs / 1000).toInt()
                val samples = if (p.on) tone(n) else ShortArray(n)
                var i = 0
                while (i < n) {
                    ensureActive()
                    val len = min(CHUNK, n - i)
                    track.write(samples, i, len)
                    i += len
                }
                written += n
            }
            // write() returns once samples are buffered; wait for them to actually play.
            while (track.playbackHeadPosition < written) delay(10)
        } finally {
            track.pause()
            track.flush()
            track.release()
        }
    }

    /** Sine wave with 5 ms fades at each end so beeps don't click. */
    private fun tone(n: Int): ShortArray {
        val ramp = min(RATE * 5 / 1000, n / 2)
        return ShortArray(n) { i ->
            val env = when {
                i < ramp -> i.toDouble() / ramp
                i >= n - ramp -> (n - 1 - i).toDouble() / ramp
                else -> 1.0
            }
            (sin(2 * PI * FREQ_HZ * i / RATE) * env * 0.6 * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
