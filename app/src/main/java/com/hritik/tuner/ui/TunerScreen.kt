package com.hritik.tuner.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hritik.tuner.Reading
import com.hritik.tuner.TunerUiState
import com.hritik.tuner.TunerViewModel
import com.hritik.tuner.audio.TUNINGS
import com.hritik.tuner.audio.Tuning
import kotlin.math.abs

private const val IN_TUNE_CENTS = 5.0
private const val CLOSE_CENTS = 15.0
private const val GAUGE_RANGE_CENTS = 50f
private const val GAUGE_SWEEP_DEGREES = 60f

@Composable
fun TunerScreen(viewModel: TunerViewModel = viewModel()) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }

    // Listen only while the app is visible.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, granted) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (granted) viewModel.start()
                Lifecycle.Event.ON_STOP -> viewModel.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        if (granted) {
            Tuner(state, viewModel)
        } else {
            PermissionPrompt { launcher.launch(Manifest.permission.RECORD_AUDIO) }
        }
    }
}

@Composable
private fun Tuner(state: TunerUiState, viewModel: TunerViewModel) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Header(state.tuning, onSelect = viewModel::selectTuning)
        Spacer(Modifier.weight(1f))

        val reading = state.reading
        val cents = reading?.cents ?: 0.0
        val color by animateColorAsState(colorFor(reading), label = "color")

        Gauge(cents = if (reading == null) 0f else cents.toFloat(), color = color, active = reading != null)
        Spacer(Modifier.height(8.dp))
        NoteReadout(reading, color)

        Spacer(Modifier.weight(1f))
        state.error?.let {
            Text(it, color = Off, modifier = Modifier.padding(bottom = 12.dp))
        }
        Strings(state, onTap = viewModel::toggleLock)
        Spacer(Modifier.height(20.dp))
        Calibration(state.a4, onAdjust = viewModel::adjustA4)
    }
}

@Composable
private fun Header(tuning: Tuning, onSelect: (Tuning) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Tuner",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Box {
            OutlinedButton(
                onClick = { open = true },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text("${tuning.name}  ▾", color = MaterialTheme.colorScheme.onBackground)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                TUNINGS.forEach { t ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(t.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    t.strings.joinToString(" ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onSelect(t)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Gauge(cents: Float, color: Color, active: Boolean) {
    val needle by animateFloatAsState(
        targetValue = cents.coerceIn(-GAUGE_RANGE_CENTS, GAUGE_RANGE_CENTS),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 120f),
        label = "needle",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val tick = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
    ) {
        val pivot = Offset(size.width / 2, size.height * 0.95f)
        val radius = minOf(size.width / 2, size.height) * 0.9f

        // Tick marks every 5 cents, longer every 10, longest at 0.
        for (c in -50..50 step 5) {
            val major = c % 10 == 0
            val length = when {
                c == 0 -> 34f
                major -> 22f
                else -> 12f
            }
            rotate(c / GAUGE_RANGE_CENTS * GAUGE_SWEEP_DEGREES, pivot) {
                drawLine(
                    color = if (abs(c) <= IN_TUNE_CENTS) InTune else tick.copy(alpha = if (major) 0.8f else 0.4f),
                    start = Offset(pivot.x, pivot.y - radius),
                    end = Offset(pivot.x, pivot.y - radius + length),
                    strokeWidth = if (major) 5f else 3f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Arc track.
        val arcRadius = radius + 18f
        drawArc(
            color = track,
            startAngle = -90f - GAUGE_SWEEP_DEGREES,
            sweepAngle = GAUGE_SWEEP_DEGREES * 2,
            useCenter = false,
            topLeft = Offset(pivot.x - arcRadius, pivot.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = 6f, cap = StrokeCap.Round),
        )

        // Needle.
        rotate(needle / GAUGE_RANGE_CENTS * GAUGE_SWEEP_DEGREES, pivot) {
            drawLine(
                color = if (active) color else tick.copy(alpha = 0.3f),
                start = pivot,
                end = Offset(pivot.x, pivot.y - radius + 6f),
                strokeWidth = 8f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(color = if (active) color else track, radius = 16f, center = pivot)
    }
}

@Composable
private fun NoteReadout(reading: Reading?, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (reading?.stale == true) 0.45f else 1f),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                reading?.targetNote?.name ?: "–",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = if (reading == null) MaterialTheme.colorScheme.onSurfaceVariant else color,
            )
            reading?.let {
                Text(
                    "${it.targetNote.octave}",
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 22.dp, start = 2.dp),
                )
            }
        }
        Text(
            when {
                reading == null -> "Play a string"
                abs(reading.cents) <= IN_TUNE_CENTS -> "In tune ✓"
                reading.cents < 0 -> "Too low · tune up ↑"
                else -> "Too high · tune down ↓"
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (reading == null) MaterialTheme.colorScheme.onSurfaceVariant else color,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            reading?.let {
                "%.1f Hz  ·  %+.0f cents  ·  target %.1f Hz".format(it.frequency, it.cents, it.targetFrequency)
            } ?: " ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Strings(state: TunerUiState, onTap: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            state.tuning.strings.forEachIndexed { i, note ->
                val locked = state.lockedString == i
                val target = state.reading?.takeIf { !it.stale }?.targetString == i
                val bg = when {
                    target -> colorFor(state.reading)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .border(
                            width = 2.dp,
                            color = if (locked) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable { onTap(i) },
                ) {
                    Text(
                        note.name,
                        fontWeight = FontWeight.Bold,
                        color = if (target) Color.Black else MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            state.lockedString?.let { "Locked to ${state.tuning.strings[it].label} · tap again for auto" }
                ?: "Auto string detection · tap a string to lock",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Calibration(a4: Int, onAdjust: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onAdjust(-1) }) { Text("−", fontSize = 22.sp) }
        Text(
            "A4 = $a4 Hz",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onAdjust(440 - a4) },
        )
        TextButton(onClick = { onAdjust(1) }) { Text("+", fontSize = 22.sp) }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🎸", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "The tuner needs your microphone to hear your guitar.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Audio is analysed on your phone and never recorded or sent anywhere.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("Allow microphone") }
    }
}

private fun colorFor(reading: Reading?): Color {
    val c = abs(reading?.cents ?: return Color.Gray)
    return when {
        c <= IN_TUNE_CENTS -> InTune
        c <= CLOSE_CENTS -> Close
        else -> Off
    }
}
