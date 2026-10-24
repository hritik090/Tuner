package com.hritik.tuner.audio

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private val NOTE_NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

/** A note identified by its MIDI number (A4 = 69). */
data class Note(val midi: Int) {
    val name: String get() = NOTE_NAMES[Math.floorMod(midi, 12)]
    val octave: Int get() = Math.floorDiv(midi, 12) - 1
    val label: String get() = "$name$octave"

    fun frequency(a4: Double = 440.0): Double = a4 * 2.0.pow((midi - 69) / 12.0)

    companion object {
        fun nearest(frequency: Double, a4: Double = 440.0): Note =
            Note((69 + 12 * log2(frequency / a4)).roundToInt())
    }
}

/** Distance in cents from [reference] to [frequency]; positive means sharp. */
fun cents(frequency: Double, reference: Double): Double = 1200 * log2(frequency / reference)
