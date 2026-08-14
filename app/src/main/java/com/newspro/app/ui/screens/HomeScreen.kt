package com.newspro.app.ui.screens

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
import com.newspro.app.data.model.Story
import com.newspro.app.ui.components.BriefingCard
import com.newspro.app.ui.components.HeroStoryCard
import com.newspro.app.ui.components.NewsIcons
import com.newspro.app.ui.components.SectionHeader
import com.newspro.app.ui.components.StoryRow
import com.newspro.app.ui.glass.ChromeState
import com.newspro.app.ui.glass.GlassButton
import com.newspro.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

@Composable
fun HomeScreen(
    stories: List<Story>,
    lang: String,
    savedIds: Set<String>,
    loading: Boolean,
    error: String?,
    chrome: ChromeState,
    contentPadding: PaddingValues,
    onOpenStory: (String) -> Unit,
    onToggleSave: (String) -> Unit,
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

    val hero = stories.first()
    val rest = stories.drop(1)
    val briefing = rest.take(5)
    val more = rest.drop(5)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "hero") {
            HeroStoryCard(
                story = hero,
                lang = lang,
                onClick = { onOpenStory(hero.id) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
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
                StoryRow(
                    story = story,
                    lang = lang,
                    onClick = { onOpenStory(story.id) },
                    saved = story.id in savedIds,
                    onToggleSave = { onToggleSave(story.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
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
