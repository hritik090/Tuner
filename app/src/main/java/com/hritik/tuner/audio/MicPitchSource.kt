package com.hritik.tuner.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/** Streams detected pitches (Hz, or null for no clear pitch) from the microphone. */
object MicPitchSource {
    private const val SAMPLE_RATE = 44100
    private const val WINDOW = 4096
    private const val HOP = 1024

    /** Callers must hold RECORD_AUDIO before collecting. */
    @SuppressLint("MissingPermission")
    fun pitches(): Flow<Double?> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        // VOICE_RECOGNITION disables automatic gain control on most devices.
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, WINDOW * 2),
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("Microphone unavailable")
        }

        val detector = YinPitchDetector(SAMPLE_RATE, WINDOW)
        val window = FloatArray(WINDOW)
        val chunk = ShortArray(HOP)
        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = record.read(chunk, 0, HOP)
                if (read <= 0) continue
                // Slide the analysis window and append the new samples.
                System.arraycopy(window, read, window, 0, WINDOW - read)
                for (i in 0 until read) window[WINDOW - read + i] = chunk[i] / 32768f
                emit(detector.detect(window))
            }
        } finally {
            record.stop()
            record.release()
        }
    }.flowOn(Dispatchers.Default)
}
