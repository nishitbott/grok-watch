package com.nishit.grokwatch.data

import android.net.Uri
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject

class GrokApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun submit(baseUrl: String, apiKey: String, prompt: String): WatchJob {
        val payload = JSONObject().put("prompt", prompt).toString()
        val request = Request.Builder()
            .url(endpoint(baseUrl, "/api/watch"))
            .header("Authorization", bearer(apiKey))
            .header("Accept", "application/json")
            .post(payload.toRequestBody(JSON_MEDIA))
            .build()
        val body = execute(request)
        val job = parseObject(body).toWatchJob(fallbackPrompt = prompt)
        if (job.id.isBlank()) {
            throw GrokException("Response did not include a job id")
        }
        return job.copy(prompt = job.prompt.ifBlank { prompt })
    }

    suspend fun fetchJob(baseUrl: String, apiKey: String, id: String): WatchJob {
        val request = Request.Builder()
            .url(endpoint(baseUrl, "/api/watch/${Uri.encode(id)}"))
            .header("Authorization", bearer(apiKey))
            .header("Accept", "application/json")
            .get()
            .build()
        val job = parseObject(execute(request)).toWatchJob()
        if (job.id.isBlank()) {
            return job.copy(id = id)
        }
        return job
    }

    suspend fun listJobs(baseUrl: String, apiKey: String): List<WatchJob> {
        val request = Request.Builder()
            .url(endpoint(baseUrl, "/api/watch"))
            .header("Authorization", bearer(apiKey))
            .header("Accept", "application/json")
            .get()
            .build()
        val root = parseObject(execute(request))
        val jobs = root.optJSONArray("jobs")
            ?: throw GrokException("History response did not include jobs")
        return buildList {
            for (index in 0 until jobs.length()) {
                val item = jobs.optJSONObject(index) ?: continue
                val job = item.toWatchJob()
                if (job.id.isNotBlank()) add(job)
            }
        }
    }

    private fun endpoint(baseUrl: String, path: String): String {
        val root = baseUrl.trim().trimEnd('/')
        if (!root.startsWith("https://") && !root.startsWith("http://")) {
            throw GrokException("Base URL must start with https://")
        }
        return root + path
    }

    private fun bearer(apiKey: String): String = "Bearer ${apiKey.trim()}"

    private fun parseObject(body: String): JSONObject {
        if (body.isBlank()) throw GrokException("Empty response from Grok")
        return try {
            JSONObject(body)
        } catch (_: JSONException) {
            throw GrokException("Unexpected response from Grok")
        }
    }

    private suspend fun execute(request: Request): String {
        return try {
            client.newCall(request).awaitBody()
        } catch (e: GrokException) {
            throw e
        } catch (_: IOException) {
            throw GrokException("Couldn't reach Grok. Check the connection and base URL.")
        }
    }

    private suspend fun Call.awaitBody(): String = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val text = response.body?.string().orEmpty()
                    if (!continuation.isActive) return
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(
                            GrokException(errorMessage(text, response.code)),
                        )
                    } else {
                        continuation.resume(text)
                    }
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                } finally {
                    response.close()
                }
            }
        })
    }

    private fun errorMessage(body: String, code: Int): String {
        val fromJson = try {
            val json = JSONObject(body)
            if (!json.has("error") || json.isNull("error")) "" else json.optString("error")
        } catch (_: JSONException) {
            ""
        }
        if (fromJson.isNotBlank()) return fromJson
        val trimmed = body.trim()
        if (trimmed.isNotBlank()) return trimmed.take(180)
        return "Request failed ($code)"
    }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
