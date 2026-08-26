package org.wut.items.collector.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect





@Composable
actual fun ImageEditScreen(
    sourcePath: String,
    onResult: (resultPath: String) -> Unit,
    onCancel: () -> Unit,
    cropAspectRatio: Float?
) {
    LaunchedEffect(Unit) { onResult(sourcePath) }
}
