package org.wut.items.collector.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter as AndroidColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID















@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ImageEditScreen(
    sourcePath: String,
    onResult: (resultPath: String) -> Unit,
    onCancel: () -> Unit,
    cropAspectRatio: Float?
) {
    val context = LocalContext.current

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var rotation by remember { mutableStateOf(0) }     
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var grayscale by remember { mutableStateOf(0f) }   
    var brightness by remember { mutableStateOf(0f) }   
    var contrast by remember { mutableStateOf(0f) }     

    var selectedTab by remember { mutableStateOf(0) } 

    
    var cropL by remember { mutableStateOf(0.05f) }
    var cropT by remember { mutableStateOf(0.05f) }
    var cropR by remember { mutableStateOf(0.95f) }
    var cropB by remember { mutableStateOf(0.95f) }

    var saving by remember { mutableStateOf(false) }

    
    LaunchedEffect(sourcePath) {
        try {
            sourceBitmap = withContext(Dispatchers.Default) {
                loadBitmapDownscaled(sourcePath, maxDim = 2048)
            }
            if (sourceBitmap == null) loadError = "Nie udało się wczytać obrazu"
        } catch (t: Throwable) {
            loadError = "Błąd wczytywania: ${t.message}"
        }
    }

    
    LaunchedEffect(sourceBitmap, rotation, flipH, flipV, cropAspectRatio) {
        val bmp = sourceBitmap ?: return@LaunchedEffect
        if (cropAspectRatio != null) {
            val rotated = rotateAndFlipBitmap(bmp, rotation, flipH, flipV)
            val bmpRatio = rotated.width.toFloat() / rotated.height.toFloat()
            if (bmpRatio > cropAspectRatio) {
                
                val w = cropAspectRatio / bmpRatio
                cropL = (1f - w) / 2f
                cropR = 1f - cropL
                cropT = 0f
                cropB = 1f
            } else {
                
                val h = bmpRatio / cropAspectRatio
                cropT = (1f - h) / 2f
                cropB = 1f - cropT
                cropL = 0f
                cropR = 1f
            }
        }
    }

    
    val colorFilter by remember(brightness, contrast, grayscale) {
        derivedStateOf { buildComposeColorFilter(brightness, contrast, grayscale) }
    }

    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color(0xFF1A1A1E))) {
        Scaffold(
            containerColor = Color(0xFF121214),
            topBar = {
                TopAppBar(
                    title = { Text("Edytor zdjęcia", color = Color(0xFFF3F4F6)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF121214),
                        titleContentColor = Color(0xFFF3F4F6),
                        navigationIconContentColor = Color(0xFFF3F4F6),
                        actionIconContentColor = Color(0xFFF3F4F6)
                    ),
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            rotation = 0
                            flipH = false
                            flipV = false
                            grayscale = 0f
                            brightness = 0f
                            contrast = 0f
                            cropL = 0.05f; cropT = 0.05f; cropR = 0.95f; cropB = 0.95f
                        }) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color(0xFFF3F4F6))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF121214)),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val bmp = sourceBitmap
                when {
                    loadError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(loadError!!, color = Color.Red)
                    }
                    bmp == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Wczytywanie...", color = Color(0xFFF3F4F6))
                    }
                    else -> {
                        val rotated = remember(bmp, rotation, flipH, flipV) {
                            rotateAndFlipBitmap(bmp, rotation, flipH, flipV)
                        }
                        val imgBitmap = remember(rotated) { rotated.asImageBitmap() }
                        val ratio = rotated.width.toFloat() / rotated.height.toFloat()
                        val painter = remember(imgBitmap) { BitmapPainter(imgBitmap) }

                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                            ) {
                        
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            colorFilter = colorFilter
                        )
                        
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(rotated) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        val pos = change.position
                                        val rectL = cropL * w
                                        val rectT = cropT * h
                                        val rectR = cropR * w
                                        val rectB = cropB * h
                                        val touchSlop = 60f
                                        val nearL = kotlin.math.abs(pos.x - rectL) < touchSlop
                                        val nearR = kotlin.math.abs(pos.x - rectR) < touchSlop
                                        val nearT = kotlin.math.abs(pos.y - rectT) < touchSlop
                                        val nearB = kotlin.math.abs(pos.y - rectB) < touchSlop

                                        if (cropAspectRatio == null) {
                                            
                                            if (nearL) cropL = (cropL + drag.x / w).coerceIn(0f, cropR - 0.05f)
                                            else if (nearR) cropR = (cropR + drag.x / w).coerceIn(cropL + 0.05f, 1f)
                                            if (nearT) cropT = (cropT + drag.y / h).coerceIn(0f, cropB - 0.05f)
                                            else if (nearB) cropB = (cropB + drag.y / h).coerceIn(cropT + 0.05f, 1f)

                                            if (!nearL && !nearR && !nearT && !nearB) {
                                                val dxN = drag.x / w
                                                val dyN = drag.y / h
                                                val newL = (cropL + dxN).coerceIn(0f, 1f - (cropR - cropL))
                                                val newT = (cropT + dyN).coerceIn(0f, 1f - (cropB - cropT))
                                                cropR = newL + (cropR - cropL)
                                                cropB = newT + (cropB - cropT)
                                                cropL = newL
                                                cropT = newT
                                            }
                                        } else {
                                            
                                            val isDraggingEdge = nearL || nearR || nearT || nearB
                                            if (isDraggingEdge) {
                                                
                                                val dxN = drag.x / w
                                                val dyN = drag.y / h
                                                
                                                
                                                
                                                
                                                if (nearL) {
                                                    val newL = (cropL + dxN).coerceIn(0f, cropR - 0.05f)
                                                    val deltaW = newL - cropL
                                                    val deltaH = (deltaW * w / h) / cropAspectRatio
                                                    if (cropT - deltaH >= 0 && cropB <= 1f) {
                                                        cropL = newL
                                                        cropT -= deltaH
                                                    }
                                                } else if (nearR) {
                                                    val newR = (cropR + dxN).coerceIn(cropL + 0.05f, 1f)
                                                    val deltaW = newR - cropR
                                                    val deltaH = (deltaW * w / h) / cropAspectRatio
                                                    if (cropB + deltaH <= 1f && cropT >= 0) {
                                                        cropR = newR
                                                        cropB += deltaH
                                                    }
                                                } else if (nearT) {
                                                    val newT = (cropT + dyN).coerceIn(0f, cropB - 0.05f)
                                                    val deltaH = newT - cropT
                                                    val deltaW = (deltaH * h / w) * cropAspectRatio
                                                    if (cropL - deltaW >= 0 && cropR <= 1f) {
                                                        cropT = newT
                                                        cropL -= deltaW
                                                    }
                                                } else if (nearB) {
                                                    val newB = (cropB + dyN).coerceIn(cropT + 0.05f, 1f)
                                                    val deltaH = newB - cropB
                                                    val deltaW = (deltaH * h / w) * cropAspectRatio
                                                    if (cropR + deltaW <= 1f && cropL >= 0) {
                                                        cropB = newB
                                                        cropR += deltaW
                                                    }
                                                }
                                            } else {
                                                
                                                val dxN = drag.x / w
                                                val dyN = drag.y / h
                                                val newL = (cropL + dxN).coerceIn(0f, 1f - (cropR - cropL))
                                                val newT = (cropT + dyN).coerceIn(0f, 1f - (cropB - cropT))
                                                cropR = newL + (cropR - cropL)
                                                cropB = newT + (cropB - cropT)
                                                cropL = newL
                                                cropT = newT
                                            }
                                        }
                                    }
                                }
                        ) {
                            
                            val rL = cropL * size.width
                            val rT = cropT * size.height
                            val rR = cropR * size.width
                            val rB = cropB * size.height
                            
                            val dim = Color(0xFF121214).copy(alpha = 0.4f)
                            drawRect(color = dim, topLeft = Offset(0f, 0f), size = Size(size.width, rT))
                            drawRect(color = dim, topLeft = Offset(0f, rB), size = Size(size.width, size.height - rB))
                            drawRect(color = dim, topLeft = Offset(0f, rT), size = Size(rL, rB - rT))
                            drawRect(color = dim, topLeft = Offset(rR, rT), size = Size(size.width - rR, rB - rT))
                            
                            drawRect(
                                color = Color(0xFFF3F4F6),
                                topLeft = Offset(rL, rT),
                                size = Size(rR - rL, rB - rT),
                                style = Stroke(width = 4f)
                            )
                            
                            val third = Color(0xFFF3F4F6).copy(alpha = 0.5f)
                            val cw = rR - rL
                            val ch = rB - rT
                            drawRect(
                                color = third,
                                topLeft = Offset(rL + cw / 3f, rT),
                                size = Size(1f, ch)
                            )
                            drawRect(
                                color = third,
                                topLeft = Offset(rL + 2f * cw / 3f, rT),
                                size = Size(1f, ch)
                            )
                            drawRect(
                                color = third,
                                topLeft = Offset(rL, rT + ch / 3f),
                                size = Size(cw, 1f)
                            )
                            drawRect(
                                color = third,
                                topLeft = Offset(rL, rT + 2f * ch / 3f),
                                size = Size(cw, 1f)
                            )
                        }
                    }
                }

                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF26262B),
                        shape = MaterialTheme.shapes.large
                    ) {
                            Column {
                                TabRow(
                                    selectedTabIndex = selectedTab,
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFFF3F4F6),
                                    divider = {}
                                ) {
                                    Tab(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = { Icon(Icons.Default.Crop, null) },
                                        text = { Text("Transformuj") }
                                    )
                                    Tab(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = { Icon(Icons.Default.Tune, null) },
                                        text = { Text("Korekta") }
                                    )
                                    Tab(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = { Icon(Icons.Default.AutoFixHigh, null) },
                                        text = { Text("Filtry") }
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .height(120.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    when (selectedTab) {
                                        0 -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                IconButton(onClick = { rotation = (rotation + 270) % 360 }) {
                                                    Icon(Icons.Default.RotateLeft, "Obrót w lewo", tint = Color(0xFFF3F4F6))
                                                }
                                                IconButton(onClick = { rotation = (rotation + 90) % 360 }) {
                                                    Icon(Icons.Default.RotateRight, "Obrót w prawo", tint = Color(0xFFF3F4F6))
                                                }
                                                IconButton(onClick = { flipH = !flipH }) {
                                                    Icon(Icons.Default.Flip, "Lustro poziome", tint = Color(0xFFF3F4F6), modifier = Modifier.rotate(90f))
                                                }
                                                IconButton(onClick = { flipV = !flipV }) {
                                                    Icon(Icons.Default.Flip, "Lustro pionowe", tint = Color(0xFFF3F4F6))
                                                }
                                                OutlinedButton(
                                                    onClick = { cropL = 0.05f; cropT = 0.05f; cropR = 0.95f; cropB = 0.95f },
                                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF3F4F6))),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Text("Resetuj kadrowanie", color = Color(0xFFF3F4F6))
                                                }
                                            }
                                        }
                                        1 -> {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Jasność", color = Color(0xFFF3F4F6), modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium)
                                                    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -100f..100f, modifier = Modifier.weight(1f))
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Kontrast", color = Color(0xFFF3F4F6), modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium)
                                                    Slider(value = contrast, onValueChange = { contrast = it }, valueRange = -100f..100f, modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                        2 -> {
                                            Column {
                                                Text("Czarno-biały", color = Color(0xFFF3F4F6), style = MaterialTheme.typography.labelMedium)
                                                Slider(value = grayscale, onValueChange = { grayscale = it }, valueRange = 0f..1f)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onCancel,
                                        modifier = Modifier.weight(1f),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF3F4F6)))
                                    ) { Text("Anuluj", color = Color(0xFFF3F4F6)) }
                                    Button(
                                        enabled = !saving,
                                        onClick = {
                                            saving = true
                                            try {
                                                val src = sourceBitmap
                                                if (src == null) {
                                                    onResult(sourcePath)
                                                    return@Button
                                                }
                                                val rotatedSrc = rotateAndFlipBitmap(src, rotation, flipH, flipV)
                                                val cropX = (cropL * rotatedSrc.width).toInt().coerceIn(0, rotatedSrc.width - 1)
                                                val cropY = (cropT * rotatedSrc.height).toInt().coerceIn(0, rotatedSrc.height - 1)
                                                val cropW = ((cropR - cropL) * rotatedSrc.width).toInt().coerceIn(1, rotatedSrc.width - cropX)
                                                val cropH = ((cropB - cropT) * rotatedSrc.height).toInt().coerceIn(1, rotatedSrc.height - cropY)
                                                val cropped = Bitmap.createBitmap(rotatedSrc, cropX, cropY, cropW, cropH)
                                                val withFilters = applyFilters(cropped, brightness, contrast, grayscale)

                                                val dir = File(context.filesDir, "media").apply { mkdirs() }
                                                val out = File(dir, "edit_${UUID.randomUUID()}.jpg")
                                                FileOutputStream(out).use { fos ->
                                                    withFilters.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                                                }
                                                onResult(out.absolutePath)
                                            } catch (t: Throwable) {
                                                loadError = "Błąd zapisu: ${t.message}"
                                                saving = false
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) { Text(if (saving) "Zapisywanie..." else "Zapisz") }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}






private fun loadBitmapDownscaled(path: String, maxDim: Int): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, opts)
    if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
    var sample = 1
    val w = opts.outWidth
    val h = opts.outHeight
    while (w / sample > maxDim || h / sample > maxDim) sample *= 2
    val real = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeFile(path, real)
}

private fun rotateAndFlipBitmap(src: Bitmap, deg: Int, flipH: Boolean, flipV: Boolean): Bitmap {
    if (deg % 360 == 0 && !flipH && !flipV) return src
    val matrix = Matrix().apply {
        postRotate(deg.toFloat())
        val sx = if (flipH) -1f else 1f
        val sy = if (flipV) -1f else 1f
        postScale(sx, sy)
    }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}







private fun buildComposeColorFilter(brightness: Float, contrast: Float, grayscale: Float): ColorFilter? {
    if (brightness == 0f && contrast == 0f && grayscale == 0f) return null
    val b = brightness.coerceIn(-100f, 100f) * 2.55f
    val c = (contrast.coerceIn(-100f, 100f) + 100f) / 100f
    val translate = (-0.5f * c + 0.5f) * 255f + b

    val cm = ColorMatrix().apply {
        if (grayscale > 0f) {
            setToSaturation(1f - grayscale)
        }
    }

    val array = cm.values
    
    
    
    for (i in 0..2) {
        for (j in 0..3) {
            array[i * 5 + j] *= c
        }
        array[i * 5 + 4] += translate
    }

    return ColorFilter.colorMatrix(ColorMatrix(array))
}




private fun applyFilters(src: Bitmap, brightness: Float, contrast: Float, grayscale: Float): Bitmap {
    if (brightness == 0f && contrast == 0f && grayscale == 0f) return src
    val b = brightness.coerceIn(-100f, 100f) * 2.55f
    val c = (contrast.coerceIn(-100f, 100f) + 100f) / 100f
    val translate = (-0.5f * c + 0.5f) * 255f + b

    val cm = AndroidColorMatrix()
    if (grayscale > 0f) {
        cm.setSaturation(1f - grayscale)
    }

    val array = cm.array
    for (i in 0..2) {
        for (j in 0..3) {
            array[i * 5 + j] *= c
        }
        array[i * 5 + 4] += translate
    }

    val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(output)
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        colorFilter = AndroidColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return output
}
