package com.samasar.app.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.samasar.app.data.model.FeedScope
import com.samasar.app.ui.components.AmbientBackground
import com.samasar.app.ui.components.NewsIcons
import com.samasar.app.ui.components.StoryActionSheet
import com.samasar.app.ui.components.UndoBar
import com.samasar.app.ui.feed.FeedViewModel
import com.samasar.app.ui.glass.ChromeState
import com.samasar.app.ui.glass.GlassChip
import com.samasar.app.ui.glass.GlassIconButton
import com.samasar.app.ui.glass.GlassNavBar
import com.samasar.app.ui.glass.GlassSearchField
import com.samasar.app.ui.glass.GlassTopBar
import com.samasar.app.ui.glass.NavItem
import com.samasar.app.ui.glass.backdrop
import com.samasar.app.ui.glass.rememberBackdrop
import com.samasar.app.ui.screens.HomeScreen
import com.samasar.app.ui.screens.OnboardingScreen
import com.samasar.app.ui.screens.PlacesScreen
import com.samasar.app.ui.screens.Preference
import com.samasar.app.ui.screens.ProfileScreen
import com.samasar.app.ui.screens.SavedScreen
import com.samasar.app.ui.screens.StoryScreen
import com.samasar.app.ui.theme.NewsProTheme

private const val RouteHome = "home"
private const val RoutePlaces = "places"
private const val RouteSaved = "saved"
private const val RouteProfile = "profile"
private const val RouteStory = "story/{id}"

private val Tabs = listOf(
    NavItem(RouteHome, "Today", NewsIcons.Home),
    NavItem(RoutePlaces, "Places", NewsIcons.Discover),
    NavItem(RouteSaved, "Saved", NewsIcons.Bookmark),
    NavItem(RouteProfile, "You", NewsIcons.Profile),
)

@Composable
fun NewsProApp() {
    var darkTheme by rememberSaveable { mutableStateOf(true) }

    // Bars are transparent and content runs underneath, so their icons have to
    // invert with the theme or they disappear against the page.
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
        AppShell(darkTheme = darkTheme, onToggleTheme = { darkTheme = it })
    }
}

/**
 * The shell owns the one backdrop everything refracts.
 *
 * Screen content lives inside the backdrop subtree; all glass chrome is a sibling
 * drawn after it. That ordering is load-bearing — a glass panel nested inside the
 * layer it samples would feed on itself.
 */
@Composable
private fun AppShell(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
) {
    val vm: FeedViewModel = viewModel()
    val ui by vm.state.collectAsStateWithLifecycle()

    val backdrop = rememberBackdrop()
    val chrome = remember { ChromeState() }
    val navController = rememberNavController()
    val density = LocalDensity.current

    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: RouteHome
    val isStory = route == RouteStory
    val selectedTab = Tabs.indexOfFirst { it.route == route }.coerceAtLeast(0)

    var placeQuery by rememberSaveable { mutableStateOf("") }
    var actionStory by remember { mutableStateOf<com.samasar.app.data.model.Story?>(null) }

    var chromeHeight by remember { mutableStateOf(0.dp) }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(
        top = chromeHeight + 14.dp,
        bottom = bottomInset + 68.dp + 34.dp,
    )

    val openStory: (String) -> Unit = { id ->
        navController.navigate("story/$id") { launchSingleTop = true }
    }

    // First launch asks for a district before anything else. Everything below
    // assumes the reader has a place; without one the feed has nothing to show.
    if (ui.needsOnboarding) {
        Box(Modifier.fillMaxSize()) {
            AmbientBackground(Modifier.fillMaxSize())
            OnboardingScreen(
                index = ui.index,
                lang = ui.lang,
                loading = ui.loading,
                error = ui.error,
                onSelect = vm::selectDistrict,
                onRetry = vm::retry,
            )
        }
        return
    }

    Box(Modifier.fillMaxSize()) {

        // ---- Backdrop: ambient field + current screen --------------------------------
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
                        stories = ui.stories,
                        lang = ui.lang,
                        stateName = ui.statePlace()?.title,
                        savedIds = ui.savedIds,
                        compact = ui.compact,
                        loading = ui.loading,
                        error = ui.error,
                        chrome = chrome,
                        contentPadding = contentPadding,
                        onOpenStory = openStory,
                        onToggleSave = vm::toggleSaved,
                        onHide = vm::hideStory,
                        onMore = { actionStory = it },
                        onRetry = vm::retry,
                    )
                }
                composable(RoutePlaces) {
                    PlacesScreen(
                        index = ui.index,
                        query = placeQuery,
                        lang = ui.lang,
                        selectedState = ui.stateCode,
                        selectedDistrict = ui.districtSlug,
                        chrome = chrome,
                        contentPadding = contentPadding,
                        onSelect = { place ->
                            vm.selectDistrict(place)
                            navController.navigate(RouteHome) {
                                popUpTo(RouteHome) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(RouteSaved) {
                    SavedScreen(
                        saved = ui.saved,
                        lang = ui.lang,
                        chrome = chrome,
                        contentPadding = contentPadding,
                        onOpenStory = openStory,
                        onToggleSave = vm::toggleSaved,
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
                        savedCount = ui.saved.size,
                        placeName = ui.districtPlace()?.title ?: "—",
                        sourceCount = ui.stories.sumOf { it.sourceCount },
                        preferences = listOf(
                            Preference(
                                icon = NewsIcons.Moon,
                                title = "Dark appearance",
                                subtitle = "Glass tuned for low light",
                                checked = darkTheme,
                                onChange = onToggleTheme,
                            ),
                            // Breaking alerts and autoplay audio used to sit here
                            // and did nothing at all. A control that lies is worse
                            // than a missing one, so they are gone until there is
                            // a push service and an audio player to back them.
                            Preference(
                                icon = NewsIcons.Sliders,
                                title = "Compact feed",
                                subtitle = "More headlines per screen",
                                checked = ui.compact,
                                onChange = vm::setCompact,
                            ),
                        ),
                        filter = ui.filter,
                        knownSources = vm.knownSources(),
                        onMuteSource = vm::muteSource,
                        onUnmuteSource = vm::unmuteSource,
                        onMuteKeyword = vm::muteKeyword,
                        onUnmuteKeyword = vm::unmuteKeyword,
                        onClearHidden = vm::clearHidden,
                        chrome = chrome,
                        contentPadding = contentPadding,
                    )
                }
                composable(
                    route = RouteStory,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    enterTransition = { slideInVertically(tween(340)) { it / 5 } + fadeIn(tween(240)) },
                    popExitTransition = { slideOutVertically(tween(280)) { it / 6 } + fadeOut(tween(200)) },
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id").orEmpty()
                    val story = vm.storyById(id)
                    if (story != null) {
                        StoryScreen(
                            story = story,
                            lang = ui.lang,
                            chrome = chrome,
                            contentPadding = contentPadding,
                        )
                    }
                }
            }
        }

        // ---- Floating chrome ---------------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                // onSizeChanged must sit *outside* statusBarsPadding. Placed after
                // it, it measures only the padded content and reports a chrome
                // height short by the status bar inset — which pushes every feed's
                // first item up underneath the scope chips.
                .onSizeChanged { chromeHeight = with(density) { it.height.toDp() } }
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isStory) {
                val story = vm.storyById(entry?.arguments?.getString("id").orEmpty())
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
                    if (story != null) {
                        GlassIconButton(
                            icon = if (story.id in ui.savedIds) NewsIcons.BookmarkFilled else NewsIcons.Bookmark,
                            contentDescription = "Save",
                            onClick = { vm.toggleSaved(story.id) },
                        )
                    }
                }
            } else {
                GlassTopBar(
                    backdrop = backdrop,
                    title = titleFor(route, ui.currentTitle),
                    subtitle = subtitleFor(route, ui),
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
                            icon = NewsIcons.Refresh,
                            contentDescription = "Refresh",
                            onClick = vm::refresh,
                            diameter = 40.dp,
                        )
                    },
                )

                when (route) {
                    // Scope tabs: local → state → national. This is the primary
                    // navigation for a regional reader, so it sits in the chrome.
                    RouteHome -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        items(ui.tabs, key = { it.first.name }) { (scope, title) ->
                            GlassChip(
                                label = title,
                                selected = scope == ui.scope,
                                onClick = { vm.selectScope(scope) },
                            )
                        }
                    }

                    RoutePlaces -> GlassSearchField(
                        value = placeQuery,
                        onValueChange = { placeQuery = it },
                        placeholder = "Search district or city",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        // ---- Bottom chrome ------------------------------------------------------------
        if (!isStory) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
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

        // Undo sits above the nav bar so a hidden story is always recoverable.
        UndoBar(
            story = ui.undoHidden,
            backdrop = backdrop,
            bottomPadding = bottomInset + 92.dp,
            onUndo = vm::undoHide,
            onDismiss = vm::dismissUndo,
        )

        StoryActionSheet(
            story = actionStory,
            backdrop = backdrop,
            onDismiss = { actionStory = null },
            onHide = {
                vm.hideStory(it)
                actionStory = null
            },
            onMuteSource = {
                vm.muteSource(it)
                actionStory = null
            },
        )
    }
}

private fun titleFor(route: String, placeTitle: String): String = when (route) {
    RoutePlaces -> "Places"
    RouteSaved -> "Saved"
    RouteProfile -> "You"
    else -> placeTitle
}

private fun subtitleFor(route: String, ui: com.samasar.app.ui.feed.FeedUiState): String? = when (route) {
    RouteHome -> when (ui.scope) {
        FeedScope.DISTRICT -> "Your district"
        FeedScope.STATE -> "Across the state"
        FeedScope.NATIONAL -> "Across India"
    }
    RoutePlaces -> "Choose where your news comes from"
    RouteSaved -> "Your reading list"
    RouteProfile -> "Account and preferences"
    else -> null
}
