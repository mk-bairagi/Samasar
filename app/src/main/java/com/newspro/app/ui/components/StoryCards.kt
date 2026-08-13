package com.newspro.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.newspro.app.data.Article
import com.newspro.app.ui.glass.GlassStyle
import com.newspro.app.ui.glass.PillCorner
import com.newspro.app.ui.glass.backdrop
import com.newspro.app.ui.glass.liquidGlass
import com.newspro.app.ui.glass.pressBounce
import com.newspro.app.ui.glass.rememberBackdrop
import com.newspro.app.ui.theme.NewsTheme
import com.newspro.app.ui.theme.categoryGradient
import androidx.compose.material3.Icon as M3Icon

/**
 * Neutral surface for feed content.
 *
 * Content is deliberately *not* glass. Glass belongs to the floating chrome; if every card
 * refracted as well, nothing would read as being in front of anything else and the hierarchy
 * would collapse. Cards get a quiet translucent plate instead.
 */
@Composable
fun Modifier.contentSurface(radius: Dp = 24.dp): Modifier {
    val colors = NewsTheme.colors
    val shape = RoundedCornerShape(radius)
    // Dark theme lifts the card off the page with a wash of the text colour; light theme has to
    // do the opposite and lay down white, or the "lift" reads as a grey smear over the page.
    val plate = if (colors.isDark) {
        colors.textPrimary.copy(alpha = 0.055f)
    } else {
        Color.White.copy(alpha = 0.58f)
    }
    return this
        .clip(shape)
        .background(plate)
        .border(1.dp, colors.divider, shape)
}

/**
 * The lead story.
 *
 * This card carries its own backdrop: the artwork is recorded as the source layer and the caption
 * panel is real liquid glass sampling it. So the panel bends the picture behind it — the same
 * effect as the chrome, scoped to one card.
 */
@Composable
fun HeroStoryCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val local = rememberBackdrop()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(430.dp)
            .pressBounce(pressedScale = 0.978f, onClick = onClick)
            .clip(RoundedCornerShape(30.dp)),
    ) {
        Box(Modifier.fillMaxSize().backdrop(local)) {
            ArticleArtwork(
                seed = article.id,
                category = article.category,
                modifier = Modifier.fillMaxSize(),
                scrimStrength = 0.55f,
            )
        }

        CategoryPill(
            category = article.category,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp),
        )

        if (article.isLive) {
            LiveBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .liquidGlass(
                    backdrop = local,
                    cornerRadius = 24.dp,
                    style = GlassStyle.Regular.copy(
                        blur = 24.dp,
                        refraction = 15.dp,
                        // Dark tint: the artwork underneath is bright, and the headline is white.
                        tintOverride = Color.Black,
                        tintAlpha = 0.34f,
                        saturation = 1.15f,
                        brightness = 0.98f,
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            MetaLine(article, tint = Color.White.copy(alpha = 0.72f))
        }
    }
}

/** Standard feed row: thumbnail, headline, meta. */
@Composable
fun StoryRow(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressBounce(pressedScale = 0.975f, onClick = onClick)
            .contentSurface(22.dp)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(15.dp)),
        ) {
            ArticleArtwork(seed = article.id, category = article.category)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = article.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = categoryGradient(article.category)[0],
            )
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            MetaLine(article, tint = colors.textTertiary)
        }
    }
}

/** Wide card used in horizontal carousels. */
@Composable
fun BriefingCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    Column(
        modifier = modifier
            .width(250.dp)
            .pressBounce(pressedScale = 0.965f, onClick = onClick)
            .contentSurface(24.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(130.dp),
        ) {
            ArticleArtwork(seed = article.id, category = article.category)
        }
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            MetaLine(article, tint = colors.textTertiary)
        }
    }
}

@Composable
fun MetaLine(
    article: Article,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = article.source,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            softWrap = false,
        )
        Dot(tint)
        Text(
            text = article.publishedAgo,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            softWrap = false,
        )
        Dot(tint)
        Text(
            text = "${article.readMinutes} min",
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun Dot(tint: Color) {
    Box(
        Modifier
            .size(3.dp)
            .clip(RoundedCornerShape(PillCorner))
            .background(tint.copy(alpha = 0.55f)),
    )
}

@Composable
fun CategoryPill(
    category: String,
    modifier: Modifier = Modifier,
) {
    val gradient = categoryGradient(category)
    Text(
        text = category.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(PillCorner))
            .background(gradient[0].copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(PillCorner))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

/** Pulsing indicator for stories still developing. */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(PillCorner))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(PillCorner))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .graphicsLayer { alpha = pulse }
                .clip(RoundedCornerShape(PillCorner))
                .background(Color(0xFFFF4D5E)),
        )
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = NewsTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
        if (action != null && onAction != null) {
            Row(
                modifier = Modifier.pressBounce(pressedScale = 0.94f, onClick = onAction),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent,
                )
                M3Icon(
                    imageVector = NewsIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
