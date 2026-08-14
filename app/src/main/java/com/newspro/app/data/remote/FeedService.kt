package com.newspro.app.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Fetches the pipeline's precomputed feed payloads.
 *
 * There is no Room database behind this, deliberately. The app reads immutable
 * JSON blobs, so OkHttp's own disk cache already gives offline reads, staleness
 * control and revalidation for free — a local database would duplicate all of it
 * and add schema migrations for nothing.
 */
class FeedService(context: Context, private val baseUrl: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "feeds"), MAX_CACHE_BYTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private suspend fun get(path: String, offline: Boolean): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(baseUrl.trimEnd('/') + "/" + path.trimStart('/'))
        if (offline) {
            builder.cacheControl(CacheControl.FORCE_CACHE)
        } else {
            // Payloads are regenerated every 15 minutes, so anything fresher than
            // a couple of minutes is not worth a round trip.
            builder.cacheControl(CacheControl.Builder().maxAge(2, TimeUnit.MINUTES).build())
        }
        client.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank()) {
                error("HTTP ${response.code} for $path")
            }
            body
        }
    }

    /**
     * Network first, cache on failure.
     *
     * A regional news reader is used on patchy connections, so a failed request
     * should show yesterday's headlines rather than an error screen.
     */
    private suspend fun <T> load(path: String, parse: (String) -> T): Result<T> = runCatching {
        try {
            parse(get(path, offline = false))
        } catch (_: Exception) {
            parse(get(path, offline = true))
        }
    }

    suspend fun index(): Result<IndexDto> =
        load("index.json") { json.decodeFromString(IndexDto.serializer(), it) }

    suspend fun feed(file: String): Result<FeedDto> =
        load(file) { json.decodeFromString(FeedDto.serializer(), it) }

    private companion object {
        const val MAX_CACHE_BYTES = 24L * 1024 * 1024
    }
}
