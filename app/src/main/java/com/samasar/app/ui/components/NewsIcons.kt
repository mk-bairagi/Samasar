package com.samasar.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Hand-built icon set.
 *
 * Drawn as strokes on a 24dp grid with one consistent weight so the chrome reads as a single
 * family, and kept in-tree so the app carries no icon dependency.
 */
private fun circlePath(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r ${r} 0 1 0 ${r * 2} 0a$r ${r} 0 1 0 ${-r * 2} 0"

private fun strokeIcon(name: String, vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { data ->
        builder.addPath(
            pathData = PathParser().parsePathString(data).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

private fun filledIcon(name: String, vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { data ->
        builder.addPath(
            pathData = PathParser().parsePathString(data).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }
    return builder.build()
}

object NewsIcons {
    val Home: ImageVector = strokeIcon(
        "Home",
        "M3.2 9.6 L12 2.8 L20.8 9.6 V19.4 A1.9 1.9 0 0 1 18.9 21.3 H5.1 A1.9 1.9 0 0 1 3.2 19.4 Z",
        "M9.2 21.3 V13.2 H14.8 V21.3",
    )

    val Discover: ImageVector = strokeIcon(
        "Discover",
        circlePath(12f, 12f, 9.2f),
        "M16.3 7.7 L14.1 14.1 L7.7 16.3 L9.9 9.9 Z",
    )

    val Bookmark: ImageVector = strokeIcon(
        "Bookmark",
        "M18.8 21.2 L12 16.3 L5.2 21.2 V4.9 A2 2 0 0 1 7.2 2.9 H16.8 A2 2 0 0 1 18.8 4.9 Z",
    )

    val BookmarkFilled: ImageVector = filledIcon(
        "BookmarkFilled",
        "M18.8 21.2 L12 16.3 L5.2 21.2 V4.9 A2 2 0 0 1 7.2 2.9 H16.8 A2 2 0 0 1 18.8 4.9 Z",
    )

    val Profile: ImageVector = strokeIcon(
        "Profile",
        circlePath(12f, 7.6f, 4.1f),
        "M4.2 21.2 V19.6 A4.8 4.8 0 0 1 9 14.8 H15 A4.8 4.8 0 0 1 19.8 19.6 V21.2",
    )

    val Search: ImageVector = strokeIcon(
        "Search",
        circlePath(10.8f, 10.8f, 7.2f),
        "M16.2 16.2 L21 21",
    )

    val Bell: ImageVector = strokeIcon(
        "Bell",
        "M18 8.6 A6 6 0 0 0 6 8.6 C6 15.2 3.4 17.2 3.4 17.2 H20.6 C20.6 17.2 18 15.2 18 8.6 Z",
        "M13.9 20.7 A2.2 2.2 0 0 1 10.1 20.7",
    )

    val Share: ImageVector = strokeIcon(
        "Share",
        "M4.4 12.6 V19.6 A1.9 1.9 0 0 0 6.3 21.5 H17.7 A1.9 1.9 0 0 0 19.6 19.6 V12.6",
        "M16 6.3 L12 2.3 L8 6.3",
        "M12 2.3 V15.2",
    )

    val Back: ImageVector = strokeIcon(
        "Back",
        "M19.2 12 H5.2",
        "M11.8 19 L4.8 12 L11.8 5",
    )

    val ChevronRight: ImageVector = strokeIcon(
        "ChevronRight",
        "M9.2 5.2 L16 12 L9.2 18.8",
    )

    val Sun: ImageVector = strokeIcon(
        "Sun",
        circlePath(12f, 12f, 4.6f),
        "M12 1.4 V3.6", "M12 20.4 V22.6",
        "M3.9 3.9 L5.5 5.5", "M18.5 18.5 L20.1 20.1",
        "M1.4 12 H3.6", "M20.4 12 H22.6",
        "M3.9 20.1 L5.5 18.5", "M18.5 5.5 L20.1 3.9",
    )

    val Moon: ImageVector = strokeIcon(
        "Moon",
        "M21 13.1 A9.3 9.3 0 1 1 10.9 3 A7.2 7.2 0 0 0 21 13.1 Z",
    )

    val Close: ImageVector = strokeIcon(
        "Close",
        "M18.4 5.6 L5.6 18.4",
        "M5.6 5.6 L18.4 18.4",
    )

    val Check: ImageVector = strokeIcon(
        "Check",
        "M20.2 6.2 L9.4 17 L3.8 11.4",
    )

    val Trending: ImageVector = strokeIcon(
        "Trending",
        "M22.4 6.4 L13.6 15.2 L8.8 10.4 L1.6 17.6",
        "M16.8 6.4 H22.4 V12",
    )

    val Clock: ImageVector = strokeIcon(
        "Clock",
        circlePath(12f, 12f, 9.2f),
        "M12 6.4 V12 L15.8 14.2",
    )

    val Listen: ImageVector = strokeIcon(
        "Listen",
        "M3.4 17.6 V12 A8.6 8.6 0 0 1 20.6 12 V17.6",
        "M20.6 18.6 A2 2 0 0 1 18.6 20.6 H17.8 A2 2 0 0 1 15.8 18.6 V16.2 A2 2 0 0 1 17.8 14.2 H20.6 Z",
        "M3.4 18.6 A2 2 0 0 0 5.4 20.6 H6.2 A2 2 0 0 0 8.2 18.6 V16.2 A2 2 0 0 0 6.2 14.2 H3.4 Z",
    )

    val Spark: ImageVector = strokeIcon(
        "Spark",
        "M12 2.6 L13.9 9 L20.3 10.9 L13.9 12.8 L12 19.2 L10.1 12.8 L3.7 10.9 L10.1 9 Z",
    )

    val More: ImageVector = filledIcon(
        "More",
        circlePath(5.2f, 12f, 1.85f),
        circlePath(12f, 12f, 1.85f),
        circlePath(18.8f, 12f, 1.85f),
    )

    val Refresh: ImageVector = strokeIcon(
        "Refresh",
        "M20.6 12 A8.6 8.6 0 1 1 18.1 5.9",
        "M20.6 3.4 V9.2 H14.8",
    )

    val Grid: ImageVector = strokeIcon(
        "Grid",
        "M3.6 3.6 H10 V10 H3.6 Z",
        "M14 3.6 H20.4 V10 H14 Z",
        "M14 14 H20.4 V20.4 H14 Z",
        "M3.6 14 H10 V20.4 H3.6 Z",
    )

    val Sliders: ImageVector = strokeIcon(
        "Sliders",
        "M4 21 V14", "M4 10 V3",
        "M12 21 V12", "M12 8 V3",
        "M20 21 V16", "M20 12 V3",
        "M1.4 14 H6.6", "M9.4 8 H14.6", "M17.4 16 H22.6",
    )
}
