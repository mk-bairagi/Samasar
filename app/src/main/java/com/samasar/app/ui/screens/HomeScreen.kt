package com.samasar.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samasar.app.data.model.Story
import com.samasar.app.ui.components.BriefingCard
import com.samasar.app.ui.components.HeroStoryCard
import com.samasar.app.ui.components.NewsIcons
import com.samasar.app.ui.components.SectionHeader
import com.samasar.app.ui.components.StoryRow
import com.samasar.app.ui.components.SwipeToHide
import com.samasar.app.ui.glass.ChromeState
import com.samasar.app.ui.glass.GlassButton
import com.samasar.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

@Composable
fun HomeScreen(
    stories: List<Story>,
    lang: String,
    stateName: String?,
    savedIds: Set<String>,
    compact: Boolean,
    loading: Boolean,
    error: String?,
    chrome: ChromeState,
    contentPadding: PaddingValues,
    onOpenStory: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onHide: (Story) -> Unit,
    onMore: (Story) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 160f).coerceIn(0f, 1f)
        }.collect { chrome.report(it) }
    }

    if (stories.isEmpty()) {
        FeedPlaceholder(
            loading = loading,
            error = error,
            contentPadding = contentPadding,
            onRetry = onRetry,
            modifier = modifier,
        )
        return
    }

    // A quiet district gets topped up with state coverage. Keep the two apart so
    // the reader is never shown state news dressed as local news.
    val local = stories.filter { it.origin != "state" }
    val fromState = stories.filter { it.origin == "state" }

    // When a district has nothing of its own, the feed is entirely state coverage.
    // Say so at the top rather than letting it pass as local news.
    val allFromState = local.isEmpty()
    val primary = local.ifEmpty { fromState }
    // Compact drops the hero card and the carousel entirely — those are what cost
    // the screen space, and "more headlines per screen" is the whole point.
    val hero = if (compact) null else primary.first()
    val rest = if (compact) primary else primary.drop(1)
    val briefing = if (compact) emptyList() else rest.take(5)
    val more = if (compact) rest else rest.drop(5)
    val stateOverflow = if (allFromState) emptyList() else fromState

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 14.dp),
    ) {
        if (allFromState) {
            item(key = "state-notice") {
                Text(
                    text = noLocalNotice(lang, stateName),
                    style = MaterialTheme.typography.labelLarge,
                    color = NewsTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }
        }

        if (hero != null) {
            item(key = "hero") {
                HeroStoryCard(
                    story = hero,
                    lang = lang,
                    onClick = { onOpenStory(hero.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (briefing.isNotEmpty()) {
            item(key = "briefing-header") {
                SectionHeader(
                    title = "Your briefing",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            item(key = "briefing") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(briefing, key = { it.id }) { story ->
                        BriefingCard(story, lang, onClick = { onOpenStory(story.id) })
                    }
                }
            }
        }

        if (more.isNotEmpty()) {
            item(key = "more-header") {
                SectionHeader(
                    title = "More stories",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            items(more, key = { it.id }) { story ->
                SwipeToHide(
                    onHide = { onHide(story) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    StoryRow(
                        story = story,
                        lang = lang,
                        onClick = { onOpenStory(story.id) },
                        saved = story.id in savedIds,
                        onToggleSave = { onToggleSave(story.id) },
                        onMore = { onMore(story) },
                        compact = compact,
                    )
                }
            }
        }

        if (stateOverflow.isNotEmpty()) {
            item(key = "state-header") {
                SectionHeader(
                    title = stateSectionTitle(lang, stateName),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            items(stateOverflow, key = { it.id }) { story ->
                SwipeToHide(
                    onHide = { onHide(story) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    StoryRow(
                        story = story,
                        lang = lang,
                        onClick = { onOpenStory(story.id) },
                        saved = story.id in savedIds,
                        onToggleSave = { onToggleSave(story.id) },
                        onMore = { onMore(story) },
                        compact = compact,
                    )
                }
            }
        }
    }
}

private fun noLocalNotice(lang: String, stateName: String?): String {
    val place = stateName?.takeIf { it.isNotBlank() }
    return when (lang) {
        "hi" -> if (place != null) "अभी कोई स्थानीय खबर नहीं — $place से समाचार" else "अभी कोई स्थानीय खबर नहीं"
        "gu" -> if (place != null) "હાલ કોઈ સ્થાનિક સમાચાર નથી — $place થી" else "હાલ કોઈ સ્થાનિક સમાચાર નથી"
        else -> if (place != null) "No local stories right now — showing $place" else "No local stories right now"
    }
}

private fun stateSectionTitle(lang: String, stateName: String?): String {
    val place = stateName?.takeIf { it.isNotBlank() }
    return when (lang) {
        "hi" -> if (place != null) "$place से" else "प्रदेश से"
        "gu" -> if (place != null) "$place થી" else "રાજ્યમાંથી"
        else -> if (place != null) "From $place" else "From the state"
    }
}

/** Shared empty / loading / error surface so every scope behaves the same. */
@Composable
fun FeedPlaceholder(
    loading: Boolean,
    error: String?,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                loading -> Text(
                    text = "Loading…",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary,
                )

                error != null -> {
                    M3Icon(
                        imageVector = NewsIcons.Close,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = "Could not load stories",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    GlassButton(
                        text = "Try again",
                        onClick = onRetry,
                        icon = NewsIcons.Refresh,
                        prominent = true,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                else -> Text(
                    text = "Nothing here yet",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
