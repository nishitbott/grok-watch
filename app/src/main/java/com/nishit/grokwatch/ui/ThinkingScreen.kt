package com.nishit.grokwatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.nishit.grokwatch.R
import com.nishit.grokwatch.viewmodel.GrokViewModel
import com.nishit.grokwatch.viewmodel.SessionState

@Composable
fun ThinkingScreen(
    viewModel: GrokViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val view = LocalView.current

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            viewModel.cancelInFlight()
        }
    }

    LaunchedEffect(session) {
        if (session is SessionState.Ready) onDone()
    }

    val prompt = when (val current = session) {
        is SessionState.Thinking -> current.prompt
        is SessionState.Failed -> current.prompt
        is SessionState.Ready -> current.job.prompt
        SessionState.Idle -> ""
    }
    val failure = (session as? SessionState.Failed)?.message

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (failure == null) {
                CircularProgressIndicator(modifier = Modifier.fillMaxSize())
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (prompt.isNotBlank()) {
                    Text(
                        text = prompt,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.body2,
                    )
                }
                if (failure == null) {
                    Text(
                        text = stringResource(R.string.thinking),
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = failure,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body2,
                    )
                    Chip(
                        label = { Text(stringResource(R.string.back)) },
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.secondaryChipColors(),
                    )
                }
            }
        }
    }
}
