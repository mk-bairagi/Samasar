package com.newspro.app.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newspro.app.ui.theme.NewsTheme

/**
 * Floating title bar.
 *
 * The glass thickens as content scrolls beneath it: at rest the bar is nearly clear, and once the
 * feed is moving underneath it deepens its blur and tint so headlines never collide with the
 * title. The panel geometry never changes, only the material — which is the point of building the
 * chrome out of a real optical model instead of a static background.
 */
@Composable
fun GlassTopBar(
    backdrop: BackdropState,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollProgress: Float = 0f,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = NewsTheme.colors
    val engaged by animateFloatAsState(
        targetValue = scrollProgress.coerceIn(0f, 1f),
        animationSpec = tween(220),
        label = "topBarEngaged",
    )

    val style = GlassStyle.Chrome.copy(
        blur = (22 + 20 * engaged).dp,
        tintAlpha = 0.08f + 0.10f * engaged,
        elevation = (10 + 18 * engaged).dp,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = 26.dp,
                style = style,
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}
