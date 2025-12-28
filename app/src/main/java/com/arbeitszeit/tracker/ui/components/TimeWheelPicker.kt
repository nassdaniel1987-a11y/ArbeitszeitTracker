package com.arbeitszeit.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Time Wheel Picker - Eleganter Zeit-Picker mit gebogenem Rad
 *
 * Features:
 * - Visuell ansprechende Zeit-Auswahl via Rad (04-24 Uhr)
 * - Drag & Drop für Start und End Marker
 * - Zeitspanne wird visuell dargestellt
 * - Unterstützt Pause-Eingabe
 */
@Composable
fun TimeWheelPicker(
    startTime: Int?,  // Minuten seit Mitternacht (z.B. 510 = 08:30)
    endTime: Int?,
    pauseMinuten: Int,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onPauseChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingStart by remember { mutableStateOf(false) }
    var isDraggingEnd by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zeit-Anzeige Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start Zeit
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Start",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceColor.copy(alpha = 0.6f)
                )
                Text(
                    text = startTime?.let { formatMinutesToTime(it) } ?: "--:--",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDraggingStart) primaryColor else onSurfaceColor
                )
            }

            // End Zeit
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "End",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceColor.copy(alpha = 0.6f)
                )
                Text(
                    text = endTime?.let { formatMinutesToTime(it) } ?: "--:--",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDraggingEnd) primaryColor else onSurfaceColor
                )
            }
        }

        // Time Wheel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            TimeWheel(
                startTime = startTime,
                endTime = endTime,
                onStartTimeChange = onStartTimeChange,
                onEndTimeChange = onEndTimeChange,
                onDraggingStartChange = { isDraggingStart = it },
                onDraggingEndChange = { isDraggingEnd = it },
                primaryColor = primaryColor,
                surfaceColor = surfaceColor,
                onSurfaceColor = onSurfaceColor
            )
        }

        // Pause Picker
        Spacer(modifier = Modifier.height(16.dp))
        PauseSlider(
            pauseMinuten = pauseMinuten,
            onPauseChange = onPauseChange,
            primaryColor = primaryColor
        )

        // Dauer Anzeige
        if (startTime != null && endTime != null) {
            val durationMinutes = calculateDuration(startTime, endTime, pauseMinuten)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Arbeitszeit: ${formatMinutesToHours(durationMinutes)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }
    }
}

@Composable
private fun TimeWheel(
    startTime: Int?,
    endTime: Int?,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onDraggingStartChange: (Boolean) -> Unit,
    onDraggingEndChange: (Boolean) -> Unit,
    primaryColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var isDraggingStart by remember { mutableStateOf(false) }
    var isDraggingEnd by remember { mutableStateOf(false) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
                        val radius = min(canvasSize.width, canvasSize.height) / 2.5f

                        // Check if touching start or end marker
                        val startAngle = startTime?.let { timeToAngle(it) }
                        val endAngle = endTime?.let { timeToAngle(it) }

                        val distanceToStart = startAngle?.let {
                            val markerPos = angleToPosition(it, radius, center)
                            distance(offset, markerPos)
                        } ?: Float.MAX_VALUE

                        val distanceToEnd = endAngle?.let {
                            val markerPos = angleToPosition(it, radius, center)
                            distance(offset, markerPos)
                        } ?: Float.MAX_VALUE

                        when {
                            distanceToStart < 50f && distanceToStart < distanceToEnd -> {
                                isDraggingStart = true
                                onDraggingStartChange(true)
                            }
                            distanceToEnd < 50f -> {
                                isDraggingEnd = true
                                onDraggingEndChange(true)
                            }
                            else -> {
                                // Create new start time
                                val angle = positionToAngle(offset, Offset(canvasSize.width / 2, canvasSize.height / 2))
                                val time = angleToTime(angle)
                                if (startTime == null) {
                                    onStartTimeChange(time)
                                } else if (endTime == null) {
                                    onEndTimeChange(time)
                                }
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
                        val angle = positionToAngle(change.position, center)
                        val time = angleToTime(angle)

                        if (isDraggingStart) {
                            onStartTimeChange(time)
                        } else if (isDraggingEnd) {
                            onEndTimeChange(time)
                        }
                    },
                    onDragEnd = {
                        isDraggingStart = false
                        isDraggingEnd = false
                        onDraggingStartChange(false)
                        onDraggingEndChange(false)
                    }
                )
            }
    ) {
        canvasSize = size
        val center = Offset(size.width / 2, size.height / 2)
        val radius = min(size.width, size.height) / 2.5f

        // Draw background arc
        drawArc(
            color = onSurfaceColor.copy(alpha = 0.1f),
            startAngle = -135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 40f, cap = StrokeCap.Round)
        )

        // Draw hour markers (04-24)
        for (hour in 4..24 step 2) {
            val angle = hourToAngle(hour)
            val markerStart = angleToPosition(angle, radius - 50f, center)
            val markerEnd = angleToPosition(angle, radius - 20f, center)

            drawLine(
                color = onSurfaceColor.copy(alpha = 0.3f),
                start = markerStart,
                end = markerEnd,
                strokeWidth = 2f
            )

            // Hour labels
            val labelPos = angleToPosition(angle, radius + 30f, center)
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = onSurfaceColor.copy(alpha = 0.6f).toArgb()
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(
                    hour.toString().padStart(2, '0'),
                    labelPos.x,
                    labelPos.y + 10f,
                    paint
                )
            }
        }

        // Draw time span arc (if both start and end are set)
        if (startTime != null && endTime != null) {
            val startAngle = timeToAngle(startTime)
            val endAngle = timeToAngle(endTime)
            val sweepAngle = calculateSweepAngle(startAngle, endAngle)

            drawArc(
                color = primaryColor.copy(alpha = 0.8f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 40f, cap = StrokeCap.Round)
            )
        }

        // Draw start marker
        startTime?.let { time ->
            val angle = timeToAngle(time)
            val markerPos = angleToPosition(angle, radius, center)

            drawCircle(
                color = Color.White,
                radius = 20f,
                center = markerPos,
                style = Stroke(width = 4f)
            )
            drawCircle(
                color = if (isDraggingStart) primaryColor else primaryColor.copy(alpha = 0.8f),
                radius = 16f,
                center = markerPos
            )
        }

        // Draw end marker
        endTime?.let { time ->
            val angle = timeToAngle(time)
            val markerPos = angleToPosition(angle, radius, center)

            drawCircle(
                color = Color.White,
                radius = 20f,
                center = markerPos,
                style = Stroke(width = 4f)
            )
            drawCircle(
                color = if (isDraggingEnd) primaryColor else primaryColor.copy(alpha = 0.8f),
                radius = 16f,
                center = markerPos
            )
        }
    }
}

@Composable
private fun PauseSlider(
    pauseMinuten: Int,
    onPauseChange: (Int) -> Unit,
    primaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pause",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = formatMinutesToHours(pauseMinuten),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pause buttons (0, 15, 30, 45, 60 min)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 15, 30, 45, 60).forEach { minutes ->
                PauseButton(
                    minutes = minutes,
                    isSelected = pauseMinuten == minutes,
                    onClick = { onPauseChange(minutes) },
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PauseButton(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = if (minutes == 0) "0" else minutes.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ========== Helper Functions ==========

/**
 * Konvertiert Minuten seit Mitternacht zu HH:MM Format
 */
private fun formatMinutesToTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return "${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}"
}

/**
 * Konvertiert Minuten zu Stunden-Format (z.B. "7:30h")
 */
private fun formatMinutesToHours(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return "${hours}:${mins.toString().padStart(2, '0')}h"
}

/**
 * Konvertiert Uhrzeit (Minuten seit Mitternacht) zu Winkel auf dem Rad
 * 04:00 Uhr = -135°, 24:00 Uhr = +135° (270° Spanne)
 */
private fun timeToAngle(minutes: Int): Float {
    // Normalisiere auf 04:00-24:00 (20 Stunden = 1200 Minuten)
    val startMinutes = 4 * 60  // 04:00 = 240 min
    val endMinutes = 24 * 60   // 24:00 = 1440 min
    val totalMinutes = endMinutes - startMinutes  // 1200 min

    val clampedMinutes = minutes.coerceIn(startMinutes, endMinutes)
    val normalizedMinutes = clampedMinutes - startMinutes

    // Map to -135° bis +135° (270° total)
    val progress = normalizedMinutes.toFloat() / totalMinutes.toFloat()
    return -135f + (progress * 270f)
}

/**
 * Konvertiert Stunde zu Winkel
 */
private fun hourToAngle(hour: Int): Float {
    return timeToAngle(hour * 60)
}

/**
 * Konvertiert Winkel zurück zu Minuten seit Mitternacht
 */
private fun angleToTime(angle: Float): Int {
    val startMinutes = 4 * 60
    val totalMinutes = 20 * 60  // 20 Stunden

    // Normalisiere Winkel zu 0-270° Range
    val normalizedAngle = ((angle + 135f) % 360f + 360f) % 360f
    val progress = (normalizedAngle / 270f).coerceIn(0f, 1f)

    val minutes = startMinutes + (progress * totalMinutes).toInt()

    // Runde auf 15-Minuten-Schritte
    return ((minutes / 15) * 15).coerceIn(startMinutes, 24 * 60)
}

/**
 * Konvertiert Winkel zu Position auf dem Rad
 */
private fun angleToPosition(angle: Float, radius: Float, center: Offset): Offset {
    val radians = Math.toRadians(angle.toDouble())
    return Offset(
        center.x + (radius * cos(radians)).toFloat(),
        center.y + (radius * sin(radians)).toFloat()
    )
}

/**
 * Konvertiert Touch-Position zu Winkel
 */
private fun positionToAngle(position: Offset, center: Offset): Float {
    val dx = position.x - center.x
    val dy = position.y - center.y
    return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
}

/**
 * Berechnet Distanz zwischen zwei Punkten
 */
private fun distance(p1: Offset, p2: Offset): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    return sqrt(dx * dx + dy * dy)
}

/**
 * Berechnet Sweep-Winkel zwischen Start und End
 */
private fun calculateSweepAngle(startAngle: Float, endAngle: Float): Float {
    var sweep = endAngle - startAngle
    if (sweep < 0) sweep += 360f
    return sweep
}

/**
 * Berechnet Arbeitsdauer in Minuten
 */
private fun calculateDuration(startTime: Int, endTime: Int, pauseMinuten: Int): Int {
    var duration = endTime - startTime
    if (duration < 0) duration += 24 * 60  // Über Mitternacht
    return (duration - pauseMinuten).coerceAtLeast(0)
}
