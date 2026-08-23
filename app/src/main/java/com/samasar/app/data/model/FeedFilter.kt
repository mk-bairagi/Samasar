package com.samasar.app.data.model

import androidx.compose.runtime.Immutable

/**
 * What the reader has asked not to see.
 *
 * All of this is local. Nothing about a reader's dislikes leaves the device.
 */
@Immutable
data class FeedFilter(
    /**
     * Hidden stories are recorded by id *and* by URL.
     *
     * The id is the cluster root, which is the oldest article in the cluster — so
     * when that article ages out of the freshness window the id changes and a
     * story hidden yesterday would walk back into the feed. The URL survives that,
     * and the id covers the case where a different article becomes the lead.
     */
    val hiddenIds: Set<String> = emptySet(),
    val hiddenUrls: Set<String> = emptySet(),
    val mutedSources: Set<String> = emptySet(),
    val mutedKeywords: Set<String> = emptySet(),
) {
    /** Everything the reader has chosen not to see, for a single summary count. */
    val totalFiltered: Int
        get() = hiddenIds.size + mutedSources.size + mutedKeywords.size

    val isEmpty: Boolean
        get() = hiddenIds.isEmpty() && hiddenUrls.isEmpty() &&
            mutedSources.isEmpty() && mutedKeywords.isEmpty()

    fun allows(story: Story): Boolean {
        if (story.id in hiddenIds || story.url in hiddenUrls) return false
        if (isSourceMuted(story)) return false
        if (mutedKeywords.isNotEmpty()) {
            val haystack = "${story.title} ${story.summary}"
            if (mutedKeywords.any { it.isNotBlank() && haystack.contains(it, ignoreCase = true) }) {
                return false
            }
        }
        return true
    }

    /**
     * Muting a publisher hides a story only when *every* source carrying it is
     * muted. Silencing Patrika should not cost the reader a story the Times of
     * India also ran — the point of clustering is that a story is bigger than any
     * one outlet.
     */
    private fun isSourceMuted(story: Story): Boolean {
        if (mutedSources.isEmpty()) return false
        val names = story.sources.map { it.name }.ifEmpty { listOf(story.source) }
        return names.all { it in mutedSources }
    }
}

/** Persisted shape of [FeedFilter]. Sets are not directly serializable. */
@kotlinx.serialization.Serializable
data class FilterDto(
    val hiddenIds: List<String> = emptyList(),
    val hiddenUrls: List<String> = emptyList(),
    val mutedSources: List<String> = emptyList(),
    val mutedKeywords: List<String> = emptyList(),
)
