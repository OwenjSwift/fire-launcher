package com.swifty.glasshome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.util.Date

private val smooth = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    favourites: List<String>,
    loaded: Boolean,
    autoOpenOn: Boolean,
    wallpaper: ImageBitmap?,
    onLaunch: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSetUpAutoOpen: () -> Unit,
    onAmazonHome: () -> Unit,
) {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(60_000 - (System.currentTimeMillis() % 60_000) + 50)
        }
    }
    val weather by produceState<Weather?>(initialValue = null) {
        while (true) {
            val fresh = WeatherRepo.fetch()
            if (fresh != null) value = fresh
            delay(if (fresh != null) 30 * 60_000L else 2 * 60_000L)
        }
    }

    var showAll by remember { mutableStateOf(false) }
    val shelfFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    val shelfApps = remember(apps, favourites) { favourites.mapNotNull { byPkg[it] }.ifEmpty { apps.take(5) } }

    BackHandler(enabled = showAll) { showAll = false }

    Box(Modifier.fillMaxSize().background(Color(0xFF2A2035))) {
        // The photo, drawn once as a plain image
        wallpaper?.let {
            Image(
                it, contentDescription = null, contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High, modifier = Modifier.fillMaxSize(),
            )
        }

        Crossfade(targetState = showAll, animationSpec = tween(260), label = "page") { all ->
            if (!all) {
                HomePage(
                    now = now,
                    weather = weather,
                    shelfApps = shelfApps,
                    shelfFocus = shelfFocus,
                    onLaunch = onLaunch,
                    onToggleFavourite = onToggleFavourite,
                    onOpenAll = { showAll = true },
                )
            } else {
                AllAppsPage(
                    apps = apps,
                    favourites = favourites,
                    autoOpenOn = autoOpenOn,
                    gridFocus = gridFocus,
                    onLaunch = onLaunch,
                    onToggleFavourite = onToggleFavourite,
                    onOpenSettings = onOpenSettings,
                    onSetUpAutoOpen = onSetUpAutoOpen,
                    onAmazonHome = onAmazonHome,
                    onClose = { showAll = false },
                )
            }
        }
    }

    // Put the remote's focus in the right place whenever the page changes or apps load.
    LaunchedEffect(showAll, loaded) {
        if (!loaded || apps.isEmpty()) return@LaunchedEffect
        delay(80)
        runCatching { if (showAll) gridFocus.requestFocus() else shelfFocus.requestFocus() }
    }
}

/** The home page. Nothing on it scrolls, so the clock always stays put. */
@Composable
private fun HomePage(
    now: Date,
    weather: Weather?,
    shelfApps: List<AppEntry>,
    shelfFocus: FocusRequester,
    onLaunch: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onOpenAll: () -> Unit,
) {
    var focusedName by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.25f))))

        weather?.let {
            WeatherPill(it, Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = Glass.sidePadding))
        }

        GlassClock(now, Modifier.align(Alignment.TopCenter).padding(top = 44.dp))

        // Title + shelf, centred, the shelf only as wide as its apps (no empty gap)
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val shelfShape = RoundedCornerShape(Glass.shelfCorner)
            Column(horizontalAlignment = Alignment.Start) {
                AnimatedContent(
                    targetState = focusedName,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "title",
                    modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
                ) { name ->
                    BasicText(
                        name.ifEmpty { " " },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Glass.text, fontSize = 22.sp, fontWeight = FontWeight.Medium, shadow = softShadow),
                    )
                }
                Row(
                    Modifier
                        .clip(shelfShape)
                        .background(Glass.glass)
                        .border(1.dp, Glass.glassEdge, shelfShape)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(Glass.cardGap),
                ) {
                    shelfApps.forEachIndexed { i, app ->
                        AppCard(
                            app = app,
                            modifier = (if (i == 0) Modifier.focusRequester(shelfFocus) else Modifier)
                                .width(Glass.shelfCardWidth)
                                .onPreviewKeyEvent {
                                    if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown) { onOpenAll(); true } else false
                                },
                            onFocused = { focusedName = app.label },
                            onClick = { onLaunch(app.packageName) },
                            onMenu = { onToggleFavourite(app.packageName) },
                        )
                    }
                }
            }
            BasicText(
                "⌄  All apps",
                modifier = Modifier.padding(top = 8.dp),
                style = TextStyle(color = Glass.textSoft, fontSize = 13.sp, shadow = softShadow),
            )
        }
    }
}

/** Every app in a grid, on its own page. Back (or Up from the top row) returns home. */
@Composable
private fun AllAppsPage(
    apps: List<AppEntry>,
    favourites: List<String>,
    autoOpenOn: Boolean,
    gridFocus: FocusRequester,
    onLaunch: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSetUpAutoOpen: () -> Unit,
    onAmazonHome: () -> Unit,
    onClose: () -> Unit,
) {
    val cols = Glass.appsPerRow
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(horizontal = Glass.sidePadding, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(Glass.cardGap),
            verticalArrangement = Arrangement.spacedBy(Glass.cardGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("All apps", "Press ☰ on an app to add it to, or take it off, the home shelf. Press Back to go home.")
            }
            itemsIndexed(apps, key = { _, a -> a.packageName }) { i, app ->
                AppCard(
                    app = app,
                    showStar = app.packageName in favourites,
                    modifier = (if (i == 0) Modifier.focusRequester(gridFocus) else Modifier)
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            if (i < cols && it.key == Key.DirectionUp && it.type == KeyEventType.KeyDown) { onClose(); true } else false
                        },
                    onFocused = {},
                    onClick = { onLaunch(app.packageName) },
                    onMenu = { onToggleFavourite(app.packageName) },
                    showLabel = true,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Tools", null, Modifier.padding(top = 12.dp))
            }
            item { ToolCard("Settings", onOpenSettings) }
            item { ToolCard("Downloader") { onLaunch(AppRepository.DOWNLOADER) } }
            item { ToolCard("Amazon Home", onAmazonHome) }
            if (!autoOpenOn) {
                item(span = { GridItemSpan(maxLineSpan) }) { SetupCard(onSetUpAutoOpen) }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, hint: String?, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 2.dp, bottom = 2.dp)) {
        BasicText(title, style = TextStyle(color = Glass.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, shadow = softShadow))
        if (hint != null) BasicText(hint, style = TextStyle(color = Glass.textSoft, fontSize = 12.sp))
    }
}

@Composable
private fun AppCard(
    app: AppEntry,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    showStar: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            focused = focused,
            modifier = Modifier
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .onPreviewKeyEvent {
                    if (it.key == Key.Menu && it.type == KeyEventType.KeyUp) { onMenu(); true } else false
                },
            background = if (app.banner != null) Color.Black else app.tint,
            onClick = onClick,
        ) {
            if (app.banner != null) {
                Image(app.banner, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                // No TV artwork: a card in the app's own colour with its icon and name.
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent))))
                Column(
                    Modifier.fillMaxSize().padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    app.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        app.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            if (showStar) {
                BasicText(
                    "★", style = TextStyle(color = Color.White, fontSize = 11.sp, shadow = softShadow),
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                )
            }
        }
        if (showLabel) {
            BasicText(
                app.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
                style = TextStyle(color = if (focused) Glass.text else Glass.textSoft, fontSize = 12.sp),
            )
        }
    }
}

@Composable
private fun ToolCard(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Card(
        focused = focused,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        background = Color.White.copy(alpha = 0.22f),
        onClick = onClick,
    ) {
        BasicText(
            label,
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/** A rounded 16:9 card. When selected it grows a little and lifts on a soft shadow. */
@Composable
private fun Card(
    focused: Boolean,
    modifier: Modifier,
    background: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Glass.cardCorner)
    val scale by animateFloatAsState(if (focused) Glass.focusScale else 1f, smooth, label = "scale")
    val lift by animateDpAsState(if (focused) 14.dp else 0.dp, tween(220, easing = FastOutSlowInEasing), label = "lift")

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (lift > 0.dp) Modifier.shadow(lift, shape) else Modifier)
            .clip(shape)
            .background(background)
            .then(if (focused) Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), shape) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        content = content,
    )
}

@Composable
private fun SetupCard(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(Glass.cardCorner)
    Box(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = if (focused) 0.35f else 0.2f))
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column {
            BasicText("Turn on auto-open", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
            BasicText(
                "So Glass Home shows up by itself. Press select, then turn on Glass Home under Accessibility.",
                style = TextStyle(color = Glass.textSoft, fontSize = 12.sp),
            )
        }
    }
}
