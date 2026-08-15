package com.samasar.app.data

import android.content.Context
import com.samasar.app.BuildConfig
import com.samasar.app.data.model.Feed
import com.samasar.app.data.model.Place
import com.samasar.app.data.model.PlaceIndex
import com.samasar.app.data.remote.FeedService
import com.samasar.app.data.remote.toDomain

/**
 * The app's single door to news data.
 *
 * Screens depend on this interface rather than on the network, so the feed can
 * be swapped for previews or tests without touching the UI.
 */
interface NewsRepository {
    suspend fun placeIndex(): Result<PlaceIndex>
    suspend fun feed(place: Place): Result<Feed>
}

class RemoteNewsRepository(
    context: Context,
    baseUrl: String = BuildConfig.FEED_BASE_URL,
) : NewsRepository {

    private val service = FeedService(context.applicationContext, baseUrl)

    override suspend fun placeIndex(): Result<PlaceIndex> =
        service.index().map { it.toDomain() }

    override suspend fun feed(place: Place): Result<Feed> =
        service.feed(place.file).map { it.toDomain() }
}
