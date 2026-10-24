package com.hritik.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hritik.tuner.audio.MicPitchSource
import com.hritik.tuner.audio.Note
import com.hritik.tuner.audio.TUNINGS
import com.hritik.tuner.audio.Tuning
import com.hritik.tuner.audio.cents
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class Reading(
    val frequency: Double,
    val targetString: Int,
    val targetNote: Note,
    val targetFrequency: Double,
    val cents: Double,
    /** True once the string has stopped ringing; the last reading is kept on screen dimmed. */
    val stale: Boolean = false,
)

data class TunerUiState(
    val tuning: Tuning = TUNINGS.first(),
    val a4: Int = 440,
    /** String index the user locked onto, or null for automatic string detection. */
    val lockedString: Int? = null,
    val reading: Reading? = null,
    val error: String? = null,
)

class TunerViewModel : ViewModel() {
    private val _state = MutableStateFlow(TunerUiState())
    val state: StateFlow<TunerUiState> = _state.asStateFlow()

    private var job: Job? = null
    private val recent = ArrayDeque<Double>()
    private var silentFrames = 0

    fun start() {
        if (job?.isActive == true) return
        _state.update { it.copy(error = null) }
        job = viewModelScope.launch {
            MicPitchSource.pitches()
                .catch { e -> _state.update { it.copy(error = e.message ?: "Microphone error") } }
                .collect(::onPitch)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun selectTuning(tuning: Tuning) = _state.update {
        it.copy(tuning = tuning, lockedString = null, reading = null)
    }

    fun toggleLock(stringIndex: Int) = _state.update {
        it.copy(lockedString = if (it.lockedString == stringIndex) null else stringIndex)
    }

    fun adjustA4(delta: Int) = _state.update { it.copy(a4 = (it.a4 + delta).coerceIn(415, 466)) }

    private fun onPitch(frequency: Double?) {
        if (frequency == null) {
            if (++silentFrames == STALE_AFTER_FRAMES) {
                recent.clear()
                _state.update { s -> s.copy(reading = s.reading?.copy(stale = true)) }
            }
            return
        }
        silentFrames = 0

        // A jump of more than ~a semitone means a new note: drop the old history.
        recent.lastOrNull()?.let { if (abs(cents(frequency, it)) > 80) recent.clear() }
        recent.addLast(frequency)
        if (recent.size > SMOOTHING) recent.removeFirst()
        val smoothed = recent.sorted()[recent.size / 2]

        _state.update { s -> s.copy(reading = read(smoothed, s)) }
    }

    private fun read(frequency: Double, s: TunerUiState): Reading {
        val a4 = s.a4.toDouble()
        val index = s.lockedString ?: s.tuning.strings.indices.minBy { i ->
            abs(cents(frequency, s.tuning.strings[i].frequency(a4)))
        }
        val note = s.tuning.strings[index]
        val target = note.frequency(a4)
        return Reading(frequency, index, note, target, cents(frequency, target))
    }

    private companion object {
        const val SMOOTHING = 5
        const val STALE_AFTER_FRAMES = 20 // ~0.5 s at a 1024-sample hop
    }
}
