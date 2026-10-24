package com.hritik.tuner.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class YinPitchDetectorTest {
    private val sampleRate = 44100
    private val size = 4096
    private val detector = YinPitchDetector(sampleRate, size)

    /** A plucked-string-like tone: fundamental plus decaying harmonics. */
    private fun tone(freq: Double, harmonics: Int = 6, noise: Double = 0.0): FloatArray {
        val rnd = Random(42)
        return FloatArray(size) { i ->
            var v = 0.0
            for (h in 1..harmonics) v += sin(2 * PI * freq * h * i / sampleRate) / h
            (0.3 * v + noise * (rnd.nextDouble() - 0.5)).toFloat()
        }
    }

    @Test
    fun detectsEveryStandardTuningString() {
        TUNINGS.first().strings.forEach { note ->
            val expected = note.frequency()
            val detected = detector.detect(tone(expected))!!
            assertEquals("${note.label}", 0.0, cents(detected, expected), 1.0)
        }
    }

    @Test
    fun detectsLowDropCAndHighFrets() {
        for (freq in listOf(65.41, 329.63 * 2, 1046.5)) {
            val detected = detector.detect(tone(freq, harmonics = 3))!!
            assertEquals(0.0, cents(detected, freq), 1.0)
        }
    }

    @Test
    fun resolvesSmallDetuning() {
        val target = Note(45).frequency()
        val sharp = target * Math.pow(2.0, 7 / 1200.0)
        assertEquals(7.0, cents(detector.detect(tone(sharp))!!, target), 0.5)
    }

    @Test
    fun toleratesNoise() {
        val detected = detector.detect(tone(110.0, noise = 0.1))!!
        assertEquals(0.0, cents(detected, 110.0), 2.0)
    }

    @Test
    fun silenceHasNoPitch() {
        assertNull(detector.detect(FloatArray(size)))
    }

    @Test
    fun whiteNoiseHasNoPitch() {
        val rnd = Random(1)
        val noise = FloatArray(size) { (rnd.nextDouble() - 0.5).toFloat() }
        assertNull(detector.detect(noise))
    }
}
