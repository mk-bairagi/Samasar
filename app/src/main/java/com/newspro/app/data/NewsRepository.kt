package com.newspro.app.data

import android.content.Context
import com.newspro.app.BuildConfig
import com.newspro.app.data.model.Feed
import com.newspro.app.data.model.Place
import com.newspro.app.data.model.PlaceIndex
import com.newspro.app.data.remote.FeedService
import com.newspro.app.data.remote.toDomain

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
