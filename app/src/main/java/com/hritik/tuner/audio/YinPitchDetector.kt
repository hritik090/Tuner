package com.hritik.tuner.audio

import kotlin.math.sqrt

/**
 * Monophonic pitch detector based on the YIN algorithm
 * (de Cheveigné & Kawahara, 2002).
 *
 * Only lags corresponding to [minFrequency]..[maxFrequency] are searched, which keeps
 * the cost low enough to run many times per second on a phone.
 */
class YinPitchDetector(
    private val sampleRate: Int,
    private val bufferSize: Int,
    minFrequency: Double = 60.0,
    maxFrequency: Double = 1400.0,
    private val threshold: Double = 0.12,
    private val silenceRms: Double = 0.008,
) {
    private val minTau = (sampleRate / maxFrequency).toInt().coerceAtLeast(2)
    private val maxTau = (sampleRate / minFrequency).toInt()
    private val window = bufferSize - maxTau
    private val yin = DoubleArray(maxTau + 1)

    init {
        require(window > maxTau) { "bufferSize $bufferSize is too small for minFrequency $minFrequency" }
    }

    /** Returns the detected fundamental in Hz, or null if there is no clear pitch. */
    fun detect(samples: FloatArray): Double? {
        require(samples.size >= bufferSize)
        if (rms(samples) < silenceRms) return null

        // Difference function.
        for (tau in 1..maxTau) {
            var sum = 0.0
            for (i in 0 until window) {
                val d = samples[i] - samples[i + tau]
                sum += d * d
            }
            yin[tau] = sum
        }

        // Cumulative mean normalised difference.
        yin[0] = 1.0
        var running = 0.0
        for (tau in 1..maxTau) {
            running += yin[tau]
            yin[tau] = if (running == 0.0) 1.0 else yin[tau] * tau / running
        }

        // Absolute threshold: first dip below the threshold, followed to its local minimum.
        var tau = minTau
        var found = -1
        while (tau < maxTau) {
            if (yin[tau] < threshold) {
                while (tau + 1 < maxTau && yin[tau + 1] < yin[tau]) tau++
                found = tau
                break
            }
            tau++
        }
        if (found == -1) return null

        return sampleRate / parabolicInterpolation(found)
    }

    private fun parabolicInterpolation(tau: Int): Double {
        if (tau <= 1 || tau >= maxTau) return tau.toDouble()
        val s0 = yin[tau - 1]
        val s1 = yin[tau]
        val s2 = yin[tau + 1]
        val denom = 2 * (2 * s1 - s2 - s0)
        return if (denom == 0.0) tau.toDouble() else tau + (s2 - s0) / denom
    }

    private fun rms(samples: FloatArray): Double {
        var sum = 0.0
        for (i in 0 until bufferSize) sum += samples[i] * samples[i]
        return sqrt(sum / bufferSize)
    }
}
