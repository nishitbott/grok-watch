package com.nishit.grokwatch.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nishit.grokwatch.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.grokDataStore by preferencesDataStore(name = "grok")

class GrokStore(context: Context) {
    private val dataStore = context.applicationContext.grokDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            baseUrl = prefs[BASE_URL] ?: BuildConfig.GROK_BASE_URL,
            apiKey = prefs[API_KEY] ?: BuildConfig.GROK_API_KEY,
        )
    }

    val history: Flow<List<WatchJob>> = dataStore.data.map { prefs ->
        decodeJobs(prefs[HISTORY] ?: "[]")
    }

    suspend fun setBaseUrl(baseUrl: String) {
        dataStore.edit { prefs -> prefs[BASE_URL] = baseUrl.trim() }
    }

    suspend fun setApiKey(apiKey: String) {
        dataStore.edit { prefs -> prefs[API_KEY] = apiKey.trim() }
    }

    suspend fun upsert(job: WatchJob) {
        if (job.id.isBlank()) return
        dataStore.edit { prefs ->
            val current = decodeJobs(prefs[HISTORY] ?: "[]")
            val updated = (listOf(job) + current.filterNot { it.id == job.id }).take(HISTORY_LIMIT)
            prefs[HISTORY] = encodeJobs(updated)
        }
    }

    suspend fun mergeRemote(remote: List<WatchJob>) {
        dataStore.edit { prefs ->
            val local = decodeJobs(prefs[HISTORY] ?: "[]")
            val localById = local.associateBy { it.id }
            val seen = linkedSetOf<String>()
            val merged = buildList {
                remote.forEach { job ->
                    if (job.id.isBlank() || !seen.add(job.id)) return@forEach
                    val existing = localById[job.id]
                    add(
                        job.copy(
                            prompt = job.prompt.ifBlank { existing?.prompt.orEmpty() },
                            createdAt = existing?.createdAt ?: job.createdAt,
                        ),
                    )
                }
                local.forEach { job ->
                    if (seen.add(job.id)) add(job)
                }
            }
            prefs[HISTORY] = encodeJobs(merged.take(HISTORY_LIMIT))
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 30
        val BASE_URL = stringPreferencesKey("base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val HISTORY = stringPreferencesKey("history_json")
    }
}
