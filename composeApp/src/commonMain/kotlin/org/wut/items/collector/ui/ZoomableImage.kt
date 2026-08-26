package org.wut.items.collector.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage








@Composable
fun ZoomableImage(
    model: Any?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }

                        if (pointerCount >= 2) {
                            
                            val zoomChange = event.calculateZoom()
                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                            scale = newScale

                            if (newScale > 1f) {
                                val panChange = event.calculatePan()
                                offsetX += panChange.x
                                offsetY += panChange.y
                                val maxOffX = (newScale - 1f) * size.width / 2f
                                val maxOffY = (newScale - 1f) * size.height / 2f
                                offsetX = offsetX.coerceIn(-maxOffX, maxOffX)
                                offsetY = offsetY.coerceIn(-maxOffY, maxOffY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (pointerCount == 1 && scale > 1f) {
                            
                            val panChange = event.calculatePan()
                            offsetX += panChange.x
                            offsetY += panChange.y
                            val maxOffX = (scale - 1f) * size.width / 2f
                            val maxOffY = (scale - 1f) * size.height / 2f
                            offsetX = offsetX.coerceIn(-maxOffX, maxOffX)
                            offsetY = offsetY.coerceIn(-maxOffY, maxOffY)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                        
                        
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                placeholder = ColorPainter(Color.DarkGray),
                error = ColorPainter(Color(0xFF442222)),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        }
    }
}
