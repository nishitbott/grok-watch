package com.nishit.grokwatch.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class WatchJob(
    val id: String,
    val prompt: String,
    val status: String,
    val reply: String,
    val error: String,
    val createdAt: Long,
)

data class AppSettings(
    val baseUrl: String,
    val apiKey: String,
)

class GrokException(message: String) : Exception(message)

fun JSONObject.toWatchJob(fallbackPrompt: String = ""): WatchJob {
    val created = if (has("createdAt") && !isNull("createdAt")) optLong("createdAt", 0L) else 0L
    return WatchJob(
        id = optText("id"),
        prompt = optText("prompt").ifBlank { fallbackPrompt },
        status = optText("status").ifBlank { "pending" },
        reply = optText("reply"),
        error = optText("error"),
        createdAt = if (created > 0L) created else System.currentTimeMillis(),
    )
}

private fun JSONObject.optText(name: String): String {
    if (!has(name) || isNull(name)) return ""
    return optString(name)
}

fun WatchJob.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("prompt", prompt)
        .put("status", status)
        .put("reply", reply)
        .put("error", error)
        .put("createdAt", createdAt)
}

fun encodeJobs(jobs: List<WatchJob>): String {
    val array = JSONArray()
    jobs.forEach { job -> array.put(job.toJson()) }
    return array.toString()
}

fun decodeJobs(raw: String): List<WatchJob> {
    if (raw.isBlank()) return emptyList()
    return try {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val job = item.toWatchJob()
                if (job.id.isNotBlank()) add(job)
            }
        }.distinctBy { it.id }
    } catch (_: JSONException) {
        emptyList()
    }
}
