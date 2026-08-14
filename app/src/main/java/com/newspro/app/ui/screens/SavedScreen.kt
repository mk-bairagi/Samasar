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
import com.newspro.app.data.model.Story
import com.newspro.app.ui.components.NewsIcons
import com.newspro.app.ui.components.StoryRow
import com.newspro.app.ui.components.contentSurface
import com.newspro.app.ui.glass.ChromeState
import com.newspro.app.ui.glass.GlassButton
import com.newspro.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

@Composable
fun SavedScreen(
    saved: List<Story>,
    lang: String,
    chrome: ChromeState,
    contentPadding: PaddingValues,
    onOpenStory: (String) -> Unit,
    onToggleSave: (String) -> Unit,
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
                        "readable offline.",
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
                    imageVector = NewsIcons.Bookmark,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "${saved.size} saved",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
            }
        }

        items(saved, key = { it.id }) { story ->
            StoryRow(
                story = story,
                lang = lang,
                onClick = { onOpenStory(story.id) },
                saved = true,
                onToggleSave = { onToggleSave(story.id) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item(key = "tail") { Box(Modifier.height(4.dp)) }
    }
}
