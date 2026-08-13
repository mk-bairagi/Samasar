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
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newspro.app.data.Article
import com.newspro.app.ui.components.ArticleArtwork
import com.newspro.app.ui.components.CategoryPill
import com.newspro.app.ui.components.LiveBadge
import com.newspro.app.ui.components.MetaLine
import com.newspro.app.ui.components.NewsIcons
import com.newspro.app.ui.components.SectionHeader
import com.newspro.app.ui.components.StoryRow
import com.newspro.app.ui.components.contentSurface
import com.newspro.app.ui.glass.ChromeState
import com.newspro.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

@Composable
fun ArticleScreen(
    article: Article,
    related: List<Article>,
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
                    .height(400.dp),
            ) {
                ArticleArtwork(
                    seed = article.id,
                    category = article.category,
                    modifier = Modifier
                        .fillMaxSize()
                        // Parallax: the picture drifts at just under half the scroll rate, so the
                        // headline peels off it rather than sliding with it.
                        .graphicsLayer {
                            val offset = if (listState.firstVisibleItemIndex == 0) {
                                listState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                size.height
                            }
                            translationY = offset * 0.42f
                        },
                )

                // The scrim stays put while the picture drifts, so the headline keeps its contrast
                // through the whole parallax travel. The last stop dissolves the artwork into the
                // page instead of ending it on a hard horizontal seam.
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Black.copy(alpha = 0.30f),
                                0.32f to Color.Transparent,
                                0.68f to Color.Black.copy(alpha = 0.45f),
                                0.92f to Color.Black.copy(alpha = 0.70f),
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryPill(article.category)
                        if (article.isLive) LiveBadge()
                    }
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                    )
                    MetaLine(article, tint = Color.White.copy(alpha = 0.78f))
                }
            }
        }

        item(key = "byline") {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                M3Icon(
                    imageVector = NewsIcons.Profile,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = "By ${article.author}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
            }
        }

        item(key = "standfirst") {
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        item(key = "keypoints") {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .contentSurface(22.dp)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    M3Icon(
                        imageVector = NewsIcons.Spark,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = "KEY POINTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                    )
                }
                article.body.take(3).forEach { point ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textTertiary,
                        )
                        Text(
                            text = point.take(96).substringBeforeLast(' ') + "…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }

        items(article.body, key = { it.take(24) }) { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        item(key = "related-header") {
            SectionHeader(
                title = "Related",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        items(related, key = { "rel-${it.id}" }) { item ->
            StoryRow(
                article = item,
                onClick = { onOpenArticle(item.id) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
