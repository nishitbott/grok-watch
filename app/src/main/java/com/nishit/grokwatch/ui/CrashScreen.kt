package com.nishit.grokwatch.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun CrashScreen(
    trace: String,
    onClear: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    val lines = remember(trace) { trace.lines() }

    WatchColumn(
        state = listState,
        horizontalAlignment = Alignment.Start,
    ) {
        item {
            Text(
                text = "App crashed",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3,
            )
        }
        items(lines) { line ->
            Text(
                text = line.ifEmpty { " " },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.error,
            )
        }
        item {
            Chip(
                label = { Text("Clear") },
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.primaryChipColors(),
            )
        }
    }
}
