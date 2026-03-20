package dev.perfoverlay.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.perfoverlay.data.StatSample
import dev.perfoverlay.ui.theme.*

/**
 * A single performance graph showing one metric over time.
 * Renders as a smooth line chart with gradient fill.
 */
@Composable
fun PerformanceGraph(
    samples: List<StatSample>,
    valueExtractor: (StatSample) -> Float,
    maxValue: Float = 100f,
    label: String,
    unit: String = "",
    lineColor: Color = AccentBlue,
    modifier: Modifier = Modifier
) {
    if (samples.isEmpty()) return

    val values = remember(samples) {
        samples.map { valueExtractor(it).coerceIn(0f, maxValue) }
    }

    val timestamps = remember(samples) {
        samples.map { it.timestamp }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )
            val lastVal = values.lastOrNull() ?: 0f
            val avgVal = if (values.isNotEmpty()) values.average().toFloat() else 0f
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${formatValue(lastVal)}$unit",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = lineColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "avg ${formatValue(avgVal)}$unit",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Graph
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            if (values.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val stepX = width / (values.size - 1).coerceAtLeast(1)

            // Draw grid lines
            drawGrid(height, maxValue)

            // Draw the line
            val path = Path()
            val fillPath = Path()

            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value / maxValue * height)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    // Smooth curve using quadratic bezier
                    val prevX = (index - 1) * stepX
                    val prevY = height - (values[index - 1] / maxValue * height)
                    val midX = (prevX + x) / 2f
                    path.quadraticBezierTo(prevX, prevY, midX, (prevY + y) / 2f)
                    fillPath.quadraticBezierTo(prevX, prevY, midX, (prevY + y) / 2f)
                }
            }
            // Final point
            val lastX = (values.size - 1) * stepX
            val lastY = height - (values.last() / maxValue * height)
            path.lineTo(lastX, lastY)
            fillPath.lineTo(lastX, lastY)
            fillPath.lineTo(lastX, height)
            fillPath.close()

            // Gradient fill under the line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.25f),
                        lineColor.copy(alpha = 0.02f)
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 2.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // End dot
            drawCircle(
                color = lineColor,
                radius = 4f,
                center = Offset(lastX, lastY)
            )
        }
    }
}

/**
 * Multi-metric graph overlay — shows multiple series on one chart.
 */
@Composable
fun MultiMetricGraph(
    samples: List<StatSample>,
    series: List<GraphSeries>,
    modifier: Modifier = Modifier
) {
    if (samples.isEmpty() || series.isEmpty()) return

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp)
    ) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            series.forEach { s ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(s.color)
                    )
                    Text(
                        text = s.label,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Graph
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            val width = size.width
            val height = size.height

            drawGrid(height, 100f)

            series.forEach { s ->
                val values = samples.map { s.extractor(it).coerceIn(0f, s.maxValue) }
                if (values.size < 2) return@forEach

                val stepX = width / (values.size - 1).coerceAtLeast(1)
                val path = Path()

                values.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = height - (value / s.maxValue * height)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevX = (index - 1) * stepX
                        val prevY = height - (values[index - 1] / s.maxValue * height)
                        val midX = (prevX + x) / 2f
                        path.quadraticBezierTo(prevX, prevY, midX, (prevY + y) / 2f)
                    }
                }

                val lastX = (values.size - 1) * stepX
                val lastY = height - (values.last() / s.maxValue * height)
                path.lineTo(lastX, lastY)

                drawPath(
                    path = path,
                    color = s.color,
                    style = Stroke(
                        width = 2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                drawCircle(
                    color = s.color,
                    radius = 3f,
                    center = Offset(lastX, lastY)
                )
            }
        }
    }
}

data class GraphSeries(
    val label: String,
    val color: Color,
    val maxValue: Float = 100f,
    val extractor: (StatSample) -> Float
)

/**
 * Time axis labels at the bottom of a graph group.
 */
@Composable
fun TimeAxisLabels(
    samples: List<StatSample>,
    modifier: Modifier = Modifier
) {
    if (samples.size < 2) return

    val duration = samples.last().timestamp - samples.first().timestamp
    val labelCount = 5

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0 until labelCount) {
            val timeMs = samples.first().timestamp + (duration * i / (labelCount - 1))
            Text(
                text = formatDuration(timeMs),
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.3f),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// --- Helpers ---

private fun DrawScope.drawGrid(height: Float, maxValue: Float) {
    val gridColor = Color.White.copy(alpha = 0.06f)
    val lines = 4
    for (i in 1 until lines) {
        val y = height * i / lines
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }
}

private fun formatValue(value: Float): String {
    return when {
        value >= 100 -> value.toInt().toString()
        value >= 10 -> String.format("%.1f", value)
        else -> String.format("%.1f", value)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m${seconds.toString().padStart(2, '0')}s"
    else "${seconds}s"
}
