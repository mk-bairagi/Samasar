package com.newspro.app.ui

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.layout.onSizeChanged
import com.newspro.app.data.SampleFeed
import com.newspro.app.data.matches
import com.newspro.app.ui.components.AmbientBackground
import com.newspro.app.ui.components.NewsIcons
import com.newspro.app.ui.glass.ChromeState
import com.newspro.app.ui.glass.GlassChip
import com.newspro.app.ui.glass.GlassIconButton
import com.newspro.app.ui.glass.GlassNavBar
import com.newspro.app.ui.glass.GlassSearchField
import com.newspro.app.ui.glass.GlassStyle
import com.newspro.app.ui.glass.GlassTopBar
import com.newspro.app.ui.glass.NavItem
import com.newspro.app.ui.glass.PillCorner
import com.newspro.app.ui.glass.backdrop
import com.newspro.app.ui.glass.liquidGlass
import com.newspro.app.ui.glass.rememberBackdrop
import com.newspro.app.ui.screens.ArticleScreen
import com.newspro.app.ui.screens.DiscoverScreen
import com.newspro.app.ui.screens.HomeScreen
import com.newspro.app.ui.screens.Preference
import com.newspro.app.ui.screens.ProfileScreen
import com.newspro.app.ui.screens.SavedScreen
import com.newspro.app.ui.theme.NewsProTheme

private const val RouteHome = "home"
private const val RouteDiscover = "discover"
private const val RouteSaved = "saved"
private const val RouteProfile = "profile"
private const val RouteArticle = "article/{id}"

private val Tabs = listOf(
    NavItem(RouteHome, "Today", NewsIcons.Home),
    NavItem(RouteDiscover, "Discover", NewsIcons.Discover),
    NavItem(RouteSaved, "Saved", NewsIcons.Bookmark),
    NavItem(RouteProfile, "You", NewsIcons.Profile),
)

@Composable
fun NewsProApp() {
    var darkTheme by rememberSaveable { mutableStateOf(true) }

    // The bars are transparent and content runs underneath them, so the icons have to invert with
    // the theme or they vanish against the page.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    NewsProTheme(darkTheme = darkTheme) {
        AppShell(
            darkTheme = darkTheme,
            onToggleTheme = { darkTheme = it },
        )
    }
}

/**
 * The shell owns the one backdrop everything refracts.
 *
 * Screen content lives *inside* the backdrop subtree; all glass chrome is a sibling drawn after
 * it. That ordering is load-bearing — a glass panel nested inside the layer it samples would
 * recursively feed on itself.
 */
@Composable
private fun AppShell(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
) {
    val backdrop = rememberBackdrop()
    val chrome = remember { ChromeState() }
    val navController = rememberNavController()
    val density = LocalDensity.current

    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: RouteHome
    val isArticle = route == RouteArticle
    val selectedTab = Tabs.indexOfFirst { it.route == route }.coerceAtLeast(0)

    var category by rememberSaveable { mutableStateOf("Top") }
    var query by rememberSaveable { mutableStateOf("") }
    val savedIds = remember { mutableStateListOf("a3", "a6") }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var autoplay by rememberSaveable { mutableStateOf(false) }
    var compact by rememberSaveable { mutableStateOf(false) }

    var chromeHeight by remember { mutableStateOf(0.dp) }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(
        top = chromeHeight + 14.dp,
        bottom = bottomInset + 68.dp + 34.dp,
    )

    val openArticle: (String) -> Unit = { id ->
        navController.navigate("article/$id") { launchSingleTop = true }
    }

    Box(Modifier.fillMaxSize()) {

        // ---- Backdrop: ambient field + whatever screen is showing --------------------------
        Box(
            Modifier
                .fillMaxSize()
                .backdrop(backdrop),
        ) {
            AmbientBackground(Modifier.fillMaxSize())

            NavHost(
                navController = navController,
                startDestination = RouteHome,
                enterTransition = { fadeIn(tween(260)) + scaleIn(initialScale = 0.975f, animationSpec = tween(300)) },
                exitTransition = { fadeOut(tween(170)) },
                popEnterTransition = { fadeIn(tween(240)) },
                popExitTransition = { fadeOut(tween(170)) + scaleOut(targetScale = 0.985f, animationSpec = tween(220)) },
            ) {
                composable(RouteHome) {
                    HomeScreen(
                        articles = SampleFeed.byCategory(category),
                        chrome = chrome,
                        contentPadding = contentPadding,
                        onOpenArticle = openArticle,
                    )
                }
                composable(RouteDiscover) {
                    DiscoverScreen(
                        articles = SampleFeed.search(query),
                        trending = SampleFeed.trending.filter { it.matches(query) },
                        publishers = SampleFeed.publishers.filter {
                            query.isBlank() || it.contains(query, ignoreCase = true)
                        },
                        query = query,
                        chrome = chrome,
                        contentPadding = contentPadding,
                        onOpenArticle = openArticle,
                    )
                }
                composable(RouteSaved) {
                    SavedScreen(
                        saved = SampleFeed.articles.filter { it.id in savedIds },
                        chrome = chrome,
                        contentPadding = contentPadding,
                        onOpenArticle = openArticle,
                        onUnsave = { savedIds.remove(it) },
                        onBrowse = {
                            navController.navigate(RouteHome) {
                                popUpTo(RouteHome) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(RouteProfile) {
                    ProfileScreen(
                        readCount = 128,
                        savedCount = savedIds.size,
                        streakDays = 23,
                        preferences = listOf(
                            Preference(
                                icon = NewsIcons.Moon,
                                title = "Dark appearance",
                                subtitle = "Glass tuned for low light",
                                checked = darkTheme,
                                onChange = onToggleTheme,
                            ),
                            Preference(
                                icon = NewsIcons.Bell,
                                title = "Breaking alerts",
                                subtitle = "Only for stories you follow",
                                checked = notifications,
                                onChange = { notifications = it },
                            ),
                            Preference(
                                icon = NewsIcons.Listen,
                                title = "Autoplay audio",
                                subtitle = "Start narration on open",
                                checked = autoplay,
                                onChange = { autoplay = it },
                            ),
                            Preference(
                                icon = NewsIcons.Sliders,
                                title = "Compact feed",
                                subtitle = "More headlines per screen",
                                checked = compact,
                                onChange = { compact = it },
                            ),
                        ),
                        chrome = chrome,
                        contentPadding = contentPadding,
                    )
                }
                composable(
                    route = RouteArticle,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    enterTransition = {
                        slideInVertically(tween(340)) { it / 5 } + fadeIn(tween(240))
                    },
                    popExitTransition = {
                        slideOutVertically(tween(280)) { it / 6 } + fadeOut(tween(200))
                    },
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id").orEmpty()
                    val article = SampleFeed.byId(id)
                    ArticleScreen(
                        article = article,
                        related = SampleFeed.articles
                            .filter { it.category == article.category && it.id != article.id }
                            .take(3),
                        chrome = chrome,
                        contentPadding = contentPadding,
                        onOpenArticle = openArticle,
                    )
                }
            }
        }

        // ---- Floating chrome ---------------------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .onSizeChanged { chromeHeight = with(density) { it.height.toDp() } },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isArticle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassIconButton(
                        icon = NewsIcons.Back,
                        contentDescription = "Back",
                        onClick = { navController.popBackStack() },
                    )
                    Spacer(Modifier.weight(1f))
                }
            } else {
                GlassTopBar(
                    backdrop = backdrop,
                    title = titleFor(route),
                    subtitle = subtitleFor(route),
                    scrollProgress = chrome.scrolled,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    trailing = {
                        GlassIconButton(
                            icon = if (darkTheme) NewsIcons.Sun else NewsIcons.Moon,
                            contentDescription = "Toggle theme",
                            onClick = { onToggleTheme(!darkTheme) },
                            diameter = 40.dp,
                        )
                        GlassIconButton(
                            icon = NewsIcons.Bell,
                            contentDescription = "Notifications",
                            onClick = {},
                            diameter = 40.dp,
                        )
                    },
                )

                when (route) {
                    RouteHome -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        items(SampleFeed.categories) { name ->
                            GlassChip(
                                label = name,
                                selected = name == category,
                                onClick = { category = name },
                            )
                        }
                    }

                    RouteDiscover -> GlassSearchField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        // ---- Bottom chrome ------------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (isArticle) {
                val id = entry?.arguments?.getString("id").orEmpty()
                ArticleActionBar(
                    backdrop = backdrop,
                    saved = id in savedIds,
                    onToggleSave = { if (id in savedIds) savedIds.remove(id) else savedIds.add(id) },
                )
            } else {
                GlassNavBar(
                    backdrop = backdrop,
                    items = Tabs,
                    selectedIndex = selectedTab,
                    onSelect = { index ->
                        val target = Tabs[index].route
                        if (target != route) {
                            navController.navigate(target) {
                                popUpTo(RouteHome) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ArticleActionBar(
    backdrop: com.newspro.app.ui.glass.BackdropState,
    saved: Boolean,
    onToggleSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = PillCorner,
                style = GlassStyle.Chrome,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(NewsIcons.Listen, "Listen", {})
        GlassIconButton(
            icon = if (saved) NewsIcons.BookmarkFilled else NewsIcons.Bookmark,
            contentDescription = if (saved) "Remove from saved" else "Save",
            onClick = onToggleSave,
        )
        GlassIconButton(NewsIcons.Share, "Share", {})
    }
}

private fun titleFor(route: String): String = when (route) {
    RouteDiscover -> "Discover"
    RouteSaved -> "Saved"
    RouteProfile -> "You"
    else -> "Today"
}

private fun subtitleFor(route: String): String? = when (route) {
    RouteHome -> "Thursday, 13 August"
    RouteDiscover -> "Topics and sources"
    RouteSaved -> "Your reading list"
    RouteProfile -> "Account and preferences"
    else -> null
}
