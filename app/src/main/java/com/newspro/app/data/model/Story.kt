package com.newspro.app.data.model

import androidx.compose.runtime.Immutable

/**
 * One story, as told by one or more publishers.
 *
 * Note what is absent: article body. The pipeline never stores or serves full
 * text — the app shows the publisher's own summary and links out to read. That
 * is the defensible aggregator model, and it is why [url] and [sources] matter
 * more here than any content field would.
 */
@Immutable
data class Story(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val imageUrl: String?,
    val source: String,
    val publishedAt: Long, // epoch seconds
    val sourceCount: Int,
    val singleSource: Boolean,
    val primarySource: Boolean,
    /** Whether this story may hold a headline slot — see the pipeline's corroboration rule. */
    val leadEligible: Boolean,
    val sources: List<StorySource>,
)

@Immutable
data class StorySource(
    val name: String,
    val url: String,
)

enum class FeedScope { NATIONAL, STATE, DISTRICT }

/**
 * A place the reader can follow: the country, a state, or a district.
 *
 * [aliases] exists because places get renamed and publishers do not keep up.
 * Amar Ujala still serves Prayagraj under the slug `allahabad`, so a reader
 * searching either name has to find it.
 */
@Immutable
data class Place(
    val scope: FeedScope,
    val state: String?,
    val district: String?,
    val lang: String,
    val title: String,
    val file: String,
    val aliases: List<String> = emptyList(),
) {
    val key: String = file.removeSuffix(".json")

    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isBlank()) return true
        return title.contains(q, ignoreCase = true) ||
            aliases.any { it.contains(q, ignoreCase = true) } ||
            district?.contains(q, ignoreCase = true) == true
    }
}

@Immutable
data class PlaceIndex(
    val national: List<Place> = emptyList(),
    val states: List<Place> = emptyList(),
    val districts: List<Place> = emptyList(),
    val activeStates: List<String> = emptyList(),
    val generatedAt: Long = 0L,
) {
    fun districtsIn(state: String, lang: String): List<Place> =
        districts.filter { it.state == state && it.lang == lang }.sortedBy { it.title }

    fun stateFeed(state: String, lang: String): Place? =
        states.firstOrNull { it.state == state && it.lang == lang }

    fun nationalFeed(lang: String): Place? =
        national.firstOrNull { it.lang == lang } ?: national.firstOrNull()
}

@Immutable
data class Feed(
    val scope: FeedScope,
    val title: String,
    val generatedAt: Long,
    val stories: List<Story>,
)
