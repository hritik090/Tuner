package com.hritik.tuner.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesTest {
    @Test
    fun namesAndFrequencies() {
        assertEquals("A4", Note(69).label)
        assertEquals("E2", Note(40).label)
        assertEquals("C♯3", Note(49).label)
        assertEquals(82.41, Note(40).frequency(), 0.01)
        assertEquals(329.63, Note(64).frequency(), 0.01)
        assertEquals(432.0, Note(69).frequency(432.0), 1e-9)
    }

    @Test
    fun nearestNoteAndCents() {
        assertEquals(Note(45), Note.nearest(112.0))
        assertEquals(Note(57), Note.nearest(220.0))
        assertEquals(100.0, cents(Note(70).frequency(), Note(69).frequency()), 1e-9)
        assertEquals(-1200.0, cents(220.0, 440.0), 1e-9)
    }
}
