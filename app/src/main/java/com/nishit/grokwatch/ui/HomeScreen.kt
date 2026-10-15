package com.nishit.grokwatch.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.nishit.grokwatch.R

@Composable
fun HomeScreen(
    onPrompt: (String) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)

    val inputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val text = extractInput(result.data)
        if (text.isNullOrBlank()) {
            message = "Didn't catch that. Try again."
        } else {
            message = null
            onPrompt(text)
        }
    }

    WatchColumn(state = listState) {
        item {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Button(
                onClick = {
                    val intent = resolveVoiceIntent(context)
                    message = if (intent == null) {
                        "Voice input isn't available on this watch."
                    } else {
                        launchInput(inputLauncher, intent)
                    }
                },
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.cd_speak),
                    modifier = Modifier.size(ButtonDefaults.LargeIconSize),
                )
            }
        }
        item {
            Chip(
                label = { Text(stringResource(R.string.type)) },
                onClick = {
                    val intent = remoteInputIntent("Ask Grok")
                    message = if (!context.canResolve(intent)) {
                        "Text input isn't available on this watch."
                    } else {
                        launchInput(inputLauncher, intent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.primaryChipColors(),
            )
        }
        item {
            Chip(
                label = { Text(stringResource(R.string.history)) },
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        item {
            Chip(
                label = { Text(stringResource(R.string.settings)) },
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        message?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colors.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption1,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun launchInput(
    launcher: ActivityResultLauncher<Intent>,
    intent: Intent,
): String? {
    return try {
        launcher.launch(intent)
        null
    } catch (_: ActivityNotFoundException) {
        "That input isn't available on this watch."
    }
}
