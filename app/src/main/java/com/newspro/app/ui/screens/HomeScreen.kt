package com.newspro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.newspro.app.data.Article
import com.newspro.app.ui.components.BriefingCard
import com.newspro.app.ui.components.HeroStoryCard
import com.newspro.app.ui.components.SectionHeader
import com.newspro.app.ui.components.StoryRow
import com.newspro.app.ui.glass.ChromeState

@Composable
fun HomeScreen(
    articles: List<Article>,
    chrome: ChromeState,
    contentPadding: PaddingValues,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Feed the top bar a 0..1 signal so its glass can thicken once content is moving under it.
    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 160f).coerceIn(0f, 1f)
        }.collect { chrome.report(it) }
    }

    val hero = articles.firstOrNull()
    val rest = if (hero != null) articles.drop(1) else articles
    val briefing = rest.take(5)
    val top = rest.drop(5).take(4)
    val more = rest.drop(9)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (hero != null) {
            item(key = "hero") {
                HeroStoryCard(
                    article = hero,
                    onClick = { onOpenArticle(hero.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item(key = "briefing-header") {
            SectionHeader(
                title = "Your briefing",
                action = "All",
                onAction = {},
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        item(key = "briefing") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(briefing, key = { it.id }) { article ->
                    BriefingCard(article = article, onClick = { onOpenArticle(article.id) })
                }
            }
        }

        item(key = "top-header") {
            SectionHeader(
                title = "Top stories",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        items(top, key = { it.id }) { article ->
            StoryRow(
                article = article,
                onClick = { onOpenArticle(article.id) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item(key = "more-header") {
            SectionHeader(
                title = "More to read",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        items(more, key = { it.id }) { article ->
            StoryRow(
                article = article,
                onClick = { onOpenArticle(article.id) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
