package com.newspro.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newspro.app.data.model.Story
import com.newspro.app.ui.glass.BackdropState
import com.newspro.app.ui.glass.GlassStyle
import com.newspro.app.ui.glass.PillCorner
import com.newspro.app.ui.glass.liquidGlass
import com.newspro.app.ui.glass.pressBounce
import com.newspro.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

/**
 * Per-story actions.
 *
 * Rendered as an overlay in the app shell rather than a ModalBottomSheet, because
 * a sheet lives in its own window and could not sample the backdrop — it would be
 * the one opaque surface in an app made of glass.
 */
@Composable
fun BoxScope.StoryActionSheet(
    story: Story?,
    backdrop: BackdropState,
    onDismiss: () -> Unit,
    onHide: (Story) -> Unit,
    onMuteSource: (String) -> Unit,
) {
    val colors = NewsTheme.colors

    AnimatedVisibility(
        visible = story != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }

    AnimatedVisibility(
        visible = story != null,
        enter = slideInVertically(spring(dampingRatio = 0.78f)) { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        val current = story ?: return@AnimatedVisibility
        val publisher = current.sources.firstOrNull()?.name ?: current.source

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .liquidGlass(
                    backdrop = backdrop,
                    cornerRadius = 30.dp,
                    // Pull the tint toward the page colour. Chrome glass over a
                    // saturated backdrop washes out label text, and this panel is
                    // nothing but labels.
                    style = GlassStyle.Chrome.copy(
                        tintOverride = colors.canvas,
                        tintAlpha = 0.42f,
                        saturation = 1.1f,
                    ),
                )
                .padding(vertical = 10.dp),
        ) {
            Text(
                text = current.title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
            )

            ActionRow(NewsIcons.Close, "इस खबर को छुपाएं", "Hide this story") { onHide(current) }

            if (publisher.isNotBlank()) {
                ActionRow(
                    icon = NewsIcons.Sliders,
                    primary = "$publisher से कम दिखाएं",
                    secondary = "Show fewer stories from $publisher",
                ) { onMuteSource(publisher) }
            }

            ActionRow(NewsIcons.ChevronRight, "रद्द करें", "Cancel", onClick = onDismiss)
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    primary: String,
    secondary: String,
    onClick: () -> Unit,
) {
    val colors = NewsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressBounce(pressedScale = 0.985f, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        M3Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(19.dp),
        )
        Column {
            Text(
                text = primary,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = secondary,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
            )
        }
    }
}

/**
 * Undo for a hidden story.
 *
 * Hiding is instant and silent everywhere else; this is the only thing standing
 * between a mis-swipe and a story the reader never sees again.
 */
@Composable
fun BoxScope.UndoBar(
    story: Story?,
    backdrop: BackdropState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = NewsTheme.colors

    AnimatedVisibility(
        visible = story != null,
        enter = slideInVertically(spring(dampingRatio = 0.7f)) { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = bottomPadding)
                .fillMaxWidth()
                .liquidGlass(
                    backdrop = backdrop,
                    cornerRadius = PillCorner,
                    style = GlassStyle.Control.copy(
                        tintOverride = colors.canvas,
                        tintAlpha = 0.42f,
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "खबर छुपाई गई",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "UNDO",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent,
                    modifier = Modifier.pressBounce(pressedScale = 0.9f, onClick = onUndo),
                )
                M3Icon(
                    imageVector = NewsIcons.Close,
                    contentDescription = "Dismiss",
                    tint = colors.textTertiary,
                    modifier = Modifier
                        .size(16.dp)
                        .pressBounce(pressedScale = 0.85f, onClick = onDismiss),
                )
            }
        }
    }
}
