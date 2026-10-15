package com.nishit.grokwatch.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.nishit.grokwatch.R
import com.nishit.grokwatch.viewmodel.GrokViewModel

@Composable
fun ReplyScreen(
    viewModel: GrokViewModel,
    onAskAgain: () -> Unit,
) {
    val job by viewModel.selectedJob.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val current = job

    WatchColumn(state = listState) {
        if (current == null) {
            item {
                Text(
                    text = "Nothing to show yet.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                )
            }
        } else {
            item {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    text = current.prompt,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }
            item {
                Text(
                    text = "Grok",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                val body = when {
                    current.reply.isNotBlank() -> current.reply
                    current.error.isNotBlank() -> current.error
                    else -> "No reply"
                }
                val isError = current.reply.isBlank() && current.error.isNotBlank()
                Text(
                    text = body,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Start,
                    color = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }
        }
        item {
            Chip(
                label = { Text(stringResource(R.string.ask_again)) },
                onClick = onAskAgain,
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.primaryChipColors(),
            )
        }
    }
}
