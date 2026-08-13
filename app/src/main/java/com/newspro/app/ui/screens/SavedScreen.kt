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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newspro.app.data.Article
import com.newspro.app.ui.components.NewsIcons
import com.newspro.app.ui.components.StoryRow
import com.newspro.app.ui.components.contentSurface
import com.newspro.app.ui.glass.ChromeState
import com.newspro.app.ui.glass.GlassButton
import com.newspro.app.ui.glass.pressBounce
import com.newspro.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

@Composable
fun SavedScreen(
    saved: List<Article>,
    chrome: ChromeState,
    contentPadding: PaddingValues,
    onOpenArticle: (String) -> Unit,
    onUnsave: (String) -> Unit,
    onBrowse: () -> Unit,
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

    if (saved.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                M3Icon(
                    imageVector = NewsIcons.Bookmark,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(42.dp),
                )
                Text(
                    text = "Nothing saved yet",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Tap the bookmark on any story and it will wait for you here, " +
                        "available offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                GlassButton(
                    text = "Browse today",
                    onClick = onBrowse,
                    icon = NewsIcons.Home,
                    prominent = true,
                    modifier = Modifier.padding(top = 6.dp),
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
        item(key = "summary") {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .contentSurface(22.dp)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                M3Icon(
                    imageVector = NewsIcons.Clock,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(21.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${saved.size} saved",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "${saved.sumOf { it.readMinutes }} minutes of reading",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textTertiary,
                    )
                }
            }
        }

        items(saved, key = { it.id }) { article ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StoryRow(
                    article = article,
                    onClick = { onOpenArticle(article.id) },
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .pressBounce(pressedScale = 0.88f) { onUnsave(article.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    M3Icon(
                        imageVector = NewsIcons.BookmarkFilled,
                        contentDescription = "Remove from saved",
                        tint = colors.accent,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        item(key = "tail") { Box(Modifier.height(4.dp)) }
    }
}
