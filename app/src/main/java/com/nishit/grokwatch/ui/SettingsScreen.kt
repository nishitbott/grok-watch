package com.nishit.grokwatch.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.nishit.grokwatch.viewmodel.GrokViewModel

@Composable
fun SettingsScreen(viewModel: GrokViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    var message by remember { mutableStateOf<String?>(null) }

    val baseUrlLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val text = extractInput(result.data)
        if (text.isNullOrBlank()) {
            message = "Base URL was left blank."
        } else {
            message = null
            viewModel.updateBaseUrl(text)
        }
    }
    val apiKeyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val text = extractInput(result.data)
        if (text.isNullOrBlank()) {
            message = "API key was left blank."
        } else {
            message = null
            viewModel.updateApiKey(text)
        }
    }

    WatchColumn(state = listState) {
        item { ListHeader { Text("Settings") } }
        item {
            Chip(
                label = { Text("Base URL") },
                secondaryLabel = {
                    Text(
                        text = settings.baseUrl.ifBlank { "Not set" },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onClick = {
                    val intent = remoteInputIntent("Base URL")
                    message = if (!context.canResolve(intent)) {
                        "Text input isn't available on this watch."
                    } else {
                        try {
                            baseUrlLauncher.launch(intent)
                            null
                        } catch (_: ActivityNotFoundException) {
                            "Text input isn't available on this watch."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        item {
            Chip(
                label = { Text("API key") },
                secondaryLabel = {
                    Text(
                        text = maskApiKey(settings.apiKey),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onClick = {
                    val intent = remoteInputIntent("API key")
                    message = if (!context.canResolve(intent)) {
                        "Text input isn't available on this watch."
                    } else {
                        try {
                            apiKeyLauncher.launch(intent)
                            null
                        } catch (_: ActivityNotFoundException) {
                            "Text input isn't available on this watch."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        item {
            Text(
                text = "The key stays masked. Defaults come from the build until you change them here.",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        message?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colors.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption1,
                )
            }
        }
    }
}
