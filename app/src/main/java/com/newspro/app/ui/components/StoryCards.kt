package com.newspro.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.newspro.app.data.model.Story
import com.newspro.app.ui.glass.GlassStyle
import com.newspro.app.ui.glass.PillCorner
import com.newspro.app.ui.glass.backdrop
import com.newspro.app.ui.glass.liquidGlass
import com.newspro.app.ui.glass.pressBounce
import com.newspro.app.ui.glass.rememberBackdrop
import com.newspro.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

/**
 * Neutral surface for feed content.
 *
 * Content is deliberately not glass. Glass belongs to the floating chrome; if every
 * card refracted as well, nothing would read as being in front of anything else.
 */
@Composable
fun Modifier.contentSurface(radius: Dp = 24.dp): Modifier {
    val colors = NewsTheme.colors
    val shape = RoundedCornerShape(radius)
    // Dark theme lifts a card off the page with a wash of the text colour; light
    // theme has to do the opposite and lay down white, or the lift reads as grey.
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
 * A story's picture, falling back to generated artwork.
 *
 * Many RSS items carry no image at all, and a grey placeholder would read as a
 * broken card. The generated fallback keeps every card looking deliberate.
 */
@Composable
fun StoryImage(
    story: Story,
    modifier: Modifier = Modifier,
    scrimStrength: Float = 0f,
) {
    val fallback: @Composable () -> Unit = {
        StoryArtwork(
            seed = story.id,
            paletteKey = story.source,
            scrimStrength = scrimStrength,
        )
    }

    if (story.imageUrl.isNullOrBlank()) {
        Box(modifier) { fallback() }
        return
    }

    Box(modifier) {
        SubcomposeAsyncImage(
            model = story.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { fallback() },
            error = { fallback() },
        )
        if (scrimStrength > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0.35f to Color.Transparent,
                            1.00f to Color.Black.copy(alpha = 0.75f * scrimStrength),
                        ),
                    ),
            )
        }
    }
}

/**
 * The lead story.
 *
 * This card carries its own backdrop: the image is recorded as the source layer
 * and the caption panel is real liquid glass sampling it, so the panel bends the
 * picture behind it.
 */
@Composable
fun HeroStoryCard(
    story: Story,
    lang: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val local = rememberBackdrop()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(410.dp)
            .pressBounce(pressedScale = 0.978f, onClick = onClick)
            .clip(RoundedCornerShape(30.dp)),
    ) {
        Box(Modifier.fillMaxSize().backdrop(local)) {
            StoryImage(story, Modifier.fillMaxSize(), scrimStrength = 0.5f)
        }

        if (story.sourceCount > 1) {
            SourceCountBadge(
                count = story.sourceCount,
                modifier = Modifier
                    .align(Alignment.TopStart)
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
                        // The picture underneath is bright and the headline is white.
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
                text = story.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (story.summary.isNotBlank()) {
                Text(
                    text = story.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StoryMeta(story, lang, tint = Color.White.copy(alpha = 0.75f))
        }
    }
}

/** Standard feed row. */
@Composable
fun StoryRow(
    story: Story,
    lang: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    saved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
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
        StoryImage(
            story,
            Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(15.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            StoryMeta(story, lang, tint = colors.textTertiary)
        }
        if (onToggleSave != null) {
            val scale by animateFloatAsState(
                targetValue = if (saved) 1f else 0.92f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
                label = "saveScale",
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .pressBounce(pressedScale = 0.85f) { onToggleSave() },
                contentAlignment = Alignment.Center,
            ) {
                M3Icon(
                    imageVector = if (saved) NewsIcons.BookmarkFilled else NewsIcons.Bookmark,
                    contentDescription = if (saved) "Remove from saved" else "Save",
                    tint = if (saved) colors.accent else colors.textTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                )
            }
        }
    }
}

/** Wide card used in the horizontal carousel. */
@Composable
fun BriefingCard(
    story: Story,
    lang: String,
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
        StoryImage(
            story,
            Modifier
                .fillMaxWidth()
                .height(130.dp),
        )
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            StoryMeta(story, lang, tint = colors.textTertiary)
        }
    }
}

@Composable
fun StoryMeta(
    story: Story,
    lang: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = story.source,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            softWrap = false,
        )
        val time = relativeTime(story.publishedAt, lang)
        if (time.isNotEmpty()) {
            Dot(tint)
            Text(
                text = time,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                maxLines = 1,
                softWrap = false,
            )
        }
        if (story.sourceCount > 1) {
            Dot(tint)
            Text(
                text = "${story.sourceCount} sources",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
                softWrap = false,
            )
        }
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

/**
 * How many independent publishers carried this story.
 *
 * This is the corroboration signal surfaced honestly. A story ten papers agree on
 * is a different thing from one nobody else has, and the reader gets to see which.
 */
@Composable
fun SourceCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(PillCorner))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(PillCorner))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        M3Icon(
            imageVector = NewsIcons.Check,
            contentDescription = null,
            tint = Color(0xFF6BE39A),
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = "$count SOURCES",
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
