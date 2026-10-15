package com.nishit.grokwatch.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.nishit.grokwatch.data.WatchJob
import com.nishit.grokwatch.viewmodel.GrokViewModel

@Composable
fun HistoryScreen(
    viewModel: GrokViewModel,
    onOpen: (WatchJob) -> Unit,
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val message by viewModel.historyMessage.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        viewModel.refreshHistory()
    }

    WatchColumn(state = listState) {
        item {
            ListHeader {
                Text("History")
            }
        }
        if (history.isEmpty()) {
            item {
                Text(
                    text = "No questions yet.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        } else {
            items(history, key = { it.id }) { job ->
                Chip(
                    label = {
                        Text(
                            text = job.prompt.ifBlank { "Untitled" },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = historyStatus(job),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = { onOpen(job) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
        }
        message?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colors.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption2,
                )
            }
        }
    }
}

private fun historyStatus(job: WatchJob): String {
    return when (job.status) {
        "done" -> "Done"
        "error" -> job.error.ifBlank { "Error" }
        "pending" -> "Pending"
        else -> job.status.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
}
