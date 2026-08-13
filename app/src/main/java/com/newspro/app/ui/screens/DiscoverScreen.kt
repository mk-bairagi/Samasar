package com.newspro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.newspro.app.data.Article
import com.newspro.app.data.Topic
import com.newspro.app.ui.components.ArticleArtwork
import com.newspro.app.ui.components.NewsIcons
import com.newspro.app.ui.components.SectionHeader
import com.newspro.app.ui.components.StoryRow
import com.newspro.app.ui.components.contentSurface
import com.newspro.app.ui.glass.ChromeState
import com.newspro.app.ui.glass.pressBounce
import com.newspro.app.ui.theme.NewsTheme
import com.newspro.app.ui.theme.categoryGradient
import androidx.compose.material3.Icon as M3Icon

@Composable
fun DiscoverScreen(
    articles: List<Article>,
    trending: List<Topic>,
    publishers: List<String>,
    query: String,
    chrome: ChromeState,
    contentPadding: PaddingValues,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 160f).coerceIn(0f, 1f)
        }.collect { chrome.report(it) }
    }

    if (query.isNotBlank() && articles.isEmpty() && trending.isEmpty() && publishers.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                M3Icon(
                    imageVector = NewsIcons.Search,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = "No results for “$query”",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Try a different topic, source or keyword.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Sections appear only when they have something to show, so a narrow search does not
        // leave a column of empty headings behind.
        if (trending.isNotEmpty()) {
            item(key = "trending-header") {
                SectionHeader(
                    title = "Trending now",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            item(key = "trending") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    trending.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { topic ->
                                TopicTile(
                                    topic = topic,
                                    modifier = Modifier.weight(1f),
                                    onClick = {},
                                )
                            }
                            if (pair.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (publishers.isNotEmpty()) {
            item(key = "sources-header") {
                SectionHeader(
                    title = "Sources",
                    action = "Manage",
                    onAction = {},
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            items(publishers, key = { it }) { publisher ->
                PublisherRow(
                    name = publisher,
                    storyCount = articles.count { it.source == publisher },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        val stories = if (query.isBlank()) articles.takeLast(6) else articles
        if (stories.isNotEmpty()) {
            item(key = "everything-header") {
                SectionHeader(
                    title = if (query.isBlank()) "Everything else" else "Stories",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            items(stories, key = { it.id }) { article ->
                StoryRow(
                    article = article,
                    onClick = { onOpenArticle(article.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun TopicTile(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val gradient = categoryGradient(topic.category)

    Box(
        modifier = modifier
            .height(104.dp)
            .pressBounce(pressedScale = 0.96f, onClick = onClick)
            .clip(RoundedCornerShape(22.dp)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            gradient[0].copy(alpha = 0.55f),
                            gradient[1].copy(alpha = 0.28f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            M3Icon(
                imageVector = NewsIcons.Trending,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(17.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${topic.storyCount} stories",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun PublisherRow(
    name: String,
    storyCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressBounce(pressedScale = 0.98f) {}
            .contentSurface(20.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp)),
        ) {
            ArticleArtwork(seed = name, category = "Top")
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = "$storyCount stories today",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
            )
        }
        M3Icon(
            imageVector = NewsIcons.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(17.dp),
        )
    }
}
