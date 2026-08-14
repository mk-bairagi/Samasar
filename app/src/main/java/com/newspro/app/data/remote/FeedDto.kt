package com.newspro.app.data.remote

import com.newspro.app.data.model.Feed
import com.newspro.app.data.model.FeedScope
import com.newspro.app.data.model.Place
import com.newspro.app.data.model.PlaceIndex
import com.newspro.app.data.model.Story
import com.newspro.app.data.model.StorySource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Wire shapes, kept separate from the domain model so a change on the pipeline
 * side does not ripple through the UI.
 */

@Serializable
data class StoryDto(
    val id: String,
    val title: String,
    val summary: String = "",
    val url: String,
    val image: String? = null,
    val source: String = "",
    @SerialName("published_at") val publishedAt: Long = 0,
    @SerialName("source_count") val sourceCount: Int = 1,
    @SerialName("single_source") val singleSource: Boolean = true,
    @SerialName("primary_source") val primarySource: Boolean = false,
    @SerialName("lead_eligible") val leadEligible: Boolean = false,
    val origin: String = "",
    val sources: List<SourceDto> = emptyList(),
)

@Serializable
data class SourceDto(val name: String = "", val url: String = "")

@Serializable
data class FeedDto(
    val scope: String = "national",
    val title: String = "",
    @SerialName("generated_at") val generatedAt: Long = 0,
    val stories: List<StoryDto> = emptyList(),
)

@Serializable
data class PlaceDto(
    val state: String? = null,
    val district: String? = null,
    val lang: String = "en",
    val title: String = "",
    val file: String = "",
    val aliases: List<String> = emptyList(),
)

@Serializable
data class IndexDto(
    val national: List<PlaceDto> = emptyList(),
    val states: List<PlaceDto> = emptyList(),
    val districts: List<PlaceDto> = emptyList(),
    @SerialName("active_states") val activeStates: List<String> = emptyList(),
    @SerialName("generated_at") val generatedAt: Long = 0,
)

// ------------------------------------------------------------------ mapping

private fun scopeOf(raw: String): FeedScope = when (raw) {
    "district" -> FeedScope.DISTRICT
    "state" -> FeedScope.STATE
    else -> FeedScope.NATIONAL
}

fun StoryDto.toDomain(): Story = Story(
    id = id,
    title = title,
    summary = summary,
    url = url,
    imageUrl = image?.takeIf { it.isNotBlank() },
    source = source,
    publishedAt = publishedAt,
    sourceCount = sourceCount,
    singleSource = singleSource,
    primarySource = primarySource,
    leadEligible = leadEligible,
    origin = origin,
    sources = sources.map { StorySource(it.name, it.url) },
)

fun FeedDto.toDomain(): Feed = Feed(
    scope = scopeOf(scope),
    title = title,
    generatedAt = generatedAt,
    stories = stories.map { it.toDomain() },
)

private fun PlaceDto.toDomain(scope: FeedScope): Place =
    Place(scope, state, district, lang, title, file, aliases)

fun IndexDto.toDomain(): PlaceIndex = PlaceIndex(
    national = national.map { it.toDomain(FeedScope.NATIONAL) },
    states = states.map { it.toDomain(FeedScope.STATE) },
    districts = districts.map { it.toDomain(FeedScope.DISTRICT) },
    activeStates = activeStates,
    generatedAt = generatedAt,
)

/** Domain → wire, used to persist saved stories locally. */
fun Story.toDto(): StoryDto = StoryDto(
    id = id,
    title = title,
    summary = summary,
    url = url,
    image = imageUrl,
    source = source,
    publishedAt = publishedAt,
    sourceCount = sourceCount,
    singleSource = singleSource,
    primarySource = primarySource,
    leadEligible = leadEligible,
    origin = origin,
    sources = sources.map { SourceDto(it.name, it.url) },
)
