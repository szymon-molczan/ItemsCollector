package org.wut.items.collector.ui

import androidx.compose.runtime.Composable















@Composable
expect fun ImageEditScreen(
    sourcePath: String,
    onResult: (resultPath: String) -> Unit,
    onCancel: () -> Unit,
    cropAspectRatio: Float? = null
)
