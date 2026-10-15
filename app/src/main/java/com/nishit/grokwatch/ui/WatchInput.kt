package com.nishit.grokwatch.ui

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.wear.input.RemoteInputIntentHelper

const val INPUT_KEY = "grok_input"

fun remoteInputIntent(label: String): Intent {
    val remoteInputs = listOf(
        RemoteInput.Builder(INPUT_KEY)
            .setLabel(label)
            .build(),
    )
    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
    return RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
}

fun speechIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Grok")
    }
}

fun resolveVoiceIntent(context: Context): Intent? {
    val remote = remoteInputIntent("Speak to Grok")
    if (context.canResolve(remote)) return remote
    val speech = speechIntent()
    if (context.canResolve(speech)) return speech
    return null
}

fun extractInput(intent: Intent?): String? {
    if (intent == null) return null
    val remote = RemoteInput.getResultsFromIntent(intent)
        ?.getCharSequence(INPUT_KEY)
        ?.toString()
        ?.trim()
    if (!remote.isNullOrEmpty()) return remote
    val spoken = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
    return spoken?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

fun maskApiKey(apiKey: String): String {
    if (apiKey.isBlank()) return "Not set"
    if (apiKey.length <= 4) return "••••"
    return "••••${apiKey.takeLast(4)}"
}

fun Context.canResolve(intent: Intent): Boolean {
    return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
}
