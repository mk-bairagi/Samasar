package com.newspro.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newspro.app.data.model.Story
import com.newspro.app.ui.components.NewsIcons
import com.newspro.app.ui.components.SectionHeader
import com.newspro.app.ui.components.SourceCountBadge
import com.newspro.app.ui.components.StoryImage
import com.newspro.app.ui.components.StoryMeta
import com.newspro.app.ui.components.contentSurface
import com.newspro.app.ui.glass.ChromeState
import com.newspro.app.ui.glass.GlassButton
import com.newspro.app.ui.glass.pressBounce
import com.newspro.app.ui.theme.NewsTheme
import com.newspro.app.ui.util.openStoryUrl
import androidx.compose.material3.Icon as M3Icon

/**
 * Story detail.
 *
 * Not an article reader. The pipeline never stores full text, so this shows the
 * publisher's own summary and then hands the reader to the publisher. Where a
 * story was carried by several outlets, all of them are listed — that transparency
 * is the point of clustering, and it lets the reader choose whose account to read.
 */
@Composable
fun StoryScreen(
    story: Story,
    lang: String,
    chrome: ChromeState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 260f).coerceIn(0f, 1f)
        }.collect { chrome.report(it) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "header") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    // Box does not clip by default, and the parallax pushes the
                    // image past its bounds — without this it paints over the
                    // story text below.
                    .clipToBounds(),
            ) {
                StoryImage(
                    story,
                    Modifier
                        .fillMaxSize()
                        // Parallax at just under half the scroll rate, so the
                        // headline peels off the picture rather than riding it.
                        .graphicsLayer {
                            val offset = if (listState.firstVisibleItemIndex == 0) {
                                listState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                size.height
                            }
                            translationY = offset * 0.42f
                        },
                )

                // The scrim stays put while the picture drifts, so contrast holds
                // through the whole travel, and dissolves the image into the page.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Black.copy(alpha = 0.32f),
                                0.34f to Color.Transparent,
                                0.70f to Color.Black.copy(alpha = 0.48f),
                                0.93f to Color.Black.copy(alpha = 0.72f),
                                1.00f to colors.canvas,
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (story.sourceCount > 1) SourceCountBadge(story.sourceCount)
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                    )
                    StoryMeta(story, lang, tint = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        if (story.summary.isNotBlank()) {
            item(key = "summary") {
                Text(
                    text = story.summary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        item(key = "read") {
            GlassButton(
                text = "Read at ${story.source}",
                onClick = { openStoryUrl(context, story.url, colors.canvas) },
                icon = NewsIcons.Share,
                prominent = true,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (story.singleSource) {
            item(key = "single-source-note") {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .contentSurface(18.dp)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    M3Icon(
                        imageVector = NewsIcons.Spark,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Reported by a single source so far. No other outlet in " +
                            "News Pro is carrying this story yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        if (story.sources.size > 1) {
            item(key = "sources-header") {
                SectionHeader(
                    title = "All sources",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }
            items(story.sources, key = { it.url }) { source ->
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .pressBounce(pressedScale = 0.98f) {
                            openStoryUrl(context, source.url, colors.canvas)
                        }
                        .contentSurface(18.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    M3Icon(
                        imageVector = NewsIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
