package com.nishit.grokwatch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nishit.grokwatch.BuildConfig
import com.nishit.grokwatch.data.AppSettings
import com.nishit.grokwatch.data.GrokApi
import com.nishit.grokwatch.data.GrokException
import com.nishit.grokwatch.data.GrokStore
import com.nishit.grokwatch.data.WatchJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Idle : SessionState

    data class Thinking(
        val prompt: String,
        val jobId: String?,
    ) : SessionState

    data class Ready(val job: WatchJob) : SessionState

    data class Failed(
        val message: String,
        val prompt: String,
    ) : SessionState
}

class GrokViewModel(application: Application) : AndroidViewModel(application) {
    private val store = GrokStore(application)
    private val api = GrokApi()
    private var pollJob: Job? = null

    val settings: StateFlow<AppSettings> = store.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(BuildConfig.GROK_BASE_URL, BuildConfig.GROK_API_KEY),
    )

    val history: StateFlow<List<WatchJob>> = store.history.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val session: StateFlow<SessionState> = sessionState.asStateFlow()

    private val selectedJobState = MutableStateFlow<WatchJob?>(null)
    val selectedJob: StateFlow<WatchJob?> = selectedJobState.asStateFlow()

    private val historyMessageState = MutableStateFlow<String?>(null)
    val historyMessage: StateFlow<String?> = historyMessageState.asStateFlow()

    fun ask(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        pollJob?.cancel()
        sessionState.value = SessionState.Thinking(trimmed, null)
        pollJob = viewModelScope.launch {
            try {
                val current = store.settings.first()
                if (current.baseUrl.isBlank()) {
                    sessionState.value = SessionState.Failed("Add a base URL in Settings", trimmed)
                    return@launch
                }
                if (current.apiKey.isBlank()) {
                    sessionState.value = SessionState.Failed("Add an API key in Settings", trimmed)
                    return@launch
                }
                val created = api.submit(current.baseUrl, current.apiKey, trimmed)
                sessionState.value = SessionState.Thinking(trimmed, created.id)
                val deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS
                while (isActive) {
                    if (System.currentTimeMillis() >= deadline) {
                        failAndStore(
                            id = created.id,
                            prompt = trimmed,
                            message = "Timed out after 3 minutes",
                        )
                        return@launch
                    }
                    val job = api.fetchJob(current.baseUrl, current.apiKey, created.id)
                    val enriched = job.copy(
                        prompt = job.prompt.ifBlank { trimmed },
                        createdAt = job.createdAt,
                    )
                    when (enriched.status) {
                        "done" -> {
                            publishReady(enriched)
                            return@launch
                        }
                        "error" -> {
                            publishFailure(
                                enriched,
                                enriched.error.ifBlank { "Grok couldn't answer that" },
                                trimmed,
                            )
                            return@launch
                        }
                        "pending" -> delay(POLL_INTERVAL_MS)
                        else -> {
                            when {
                                enriched.reply.isNotBlank() -> {
                                    publishReady(enriched.copy(status = "done"))
                                    return@launch
                                }
                                enriched.error.isNotBlank() -> {
                                    publishFailure(enriched, enriched.error, trimmed)
                                    return@launch
                                }
                                else -> delay(POLL_INTERVAL_MS)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GrokException) {
                sessionState.value = SessionState.Failed(
                    e.message ?: "Request failed",
                    trimmed,
                )
            } catch (e: Exception) {
                sessionState.value = SessionState.Failed(
                    e.message ?: "Something went wrong",
                    trimmed,
                )
            }
        }
    }

    fun cancelInFlight() {
        if (sessionState.value is SessionState.Thinking) {
            pollJob?.cancel()
            sessionState.value = SessionState.Idle
        }
    }

    fun open(job: WatchJob) {
        selectedJobState.value = job
    }

    fun dismissSession() {
        sessionState.value = SessionState.Idle
    }

    fun updateBaseUrl(baseUrl: String) {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { store.setBaseUrl(trimmed) }
    }

    fun updateApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { store.setApiKey(trimmed) }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            val current = store.settings.first()
            if (current.apiKey.isBlank() || current.baseUrl.isBlank()) {
                historyMessageState.value = null
                return@launch
            }
            try {
                val jobs = api.listJobs(current.baseUrl, current.apiKey)
                store.mergeRemote(jobs)
                historyMessageState.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                historyMessageState.value = e.message ?: "Couldn't refresh history"
            }
        }
    }

    private suspend fun publishReady(job: WatchJob) {
        store.upsert(job)
        selectedJobState.value = job
        sessionState.value = SessionState.Ready(job)
    }

    private suspend fun publishFailure(job: WatchJob, message: String, prompt: String) {
        store.upsert(
            job.copy(
                status = "error",
                error = message,
                prompt = job.prompt.ifBlank { prompt },
            ),
        )
        sessionState.value = SessionState.Failed(message, prompt)
    }

    private suspend fun failAndStore(id: String, prompt: String, message: String) {
        store.upsert(
            WatchJob(
                id = id,
                prompt = prompt,
                status = "error",
                reply = "",
                error = message,
                createdAt = System.currentTimeMillis(),
            ),
        )
        sessionState.value = SessionState.Failed(message, prompt)
    }

    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
        const val POLL_TIMEOUT_MS = 3 * 60 * 1_000L
    }
}
