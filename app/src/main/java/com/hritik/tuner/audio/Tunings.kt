package com.hritik.tuner.audio

/** A guitar tuning, strings listed from lowest (6th) to highest (1st). */
data class Tuning(val name: String, val strings: List<Note>)

private fun tuning(name: String, vararg midi: Int) = Tuning(name, midi.map(::Note))

val TUNINGS = listOf(
    tuning("Standard", 40, 45, 50, 55, 59, 64),
    tuning("Drop D", 38, 45, 50, 55, 59, 64),
    tuning("Half step down", 39, 44, 49, 54, 58, 63),
    tuning("Full step down", 38, 43, 48, 53, 57, 62),
    tuning("Drop C", 36, 43, 48, 53, 57, 62),
    tuning("Open G", 38, 43, 50, 55, 59, 62),
    tuning("Open D", 38, 45, 50, 54, 57, 62),
    tuning("DADGAD", 38, 45, 50, 55, 57, 62),
)
