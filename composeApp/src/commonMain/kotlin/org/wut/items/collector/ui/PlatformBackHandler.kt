package org.wut.items.collector.ui

import androidx.compose.runtime.Composable





@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
