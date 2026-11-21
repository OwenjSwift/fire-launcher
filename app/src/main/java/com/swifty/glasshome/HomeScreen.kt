package com.swifty.glasshome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    favourites: List<String>,
    loaded: Boolean,
    autoOpenOn: Boolean,
    wallpaper: WallpaperSet?,
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

    var focusedName by remember { mutableStateOf("") }
    val firstCard = remember { FocusRequester() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    val shelfApps = favourites.mapNotNull { byPkg[it] }.ifEmpty { apps.take(6) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF9CC9DD))) {
        val screen = IntSize(constraints.maxWidth, constraints.maxHeight)
        val pageHeight = maxHeight

        CompositionLocalProvider(LocalBackdrop provides Backdrop(wallpaper?.frost, wallpaper?.glass, screen)) {
            // The photo, softly blurred
            wallpaper?.let {
                Image(it.soft, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            // A light shade at the top and bottom keeps white text readable on bright photos
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.18f),
                        0.35f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.22f),
                    )
                )
            )

            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                // ---------- Page 1: clock and the shelf ----------
                Box(Modifier.fillMaxWidth().height(pageHeight)) {
                    Row(
                        Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = Glass.sidePadding),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        weather?.let { WeatherPill(it) }
                    }

                    GlassClock(now, Modifier.align(Alignment.TopCenter).padding(top = 22.dp))

                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 26.dp)) {
                        AnimatedContent(
                            targetState = focusedName,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                            label = "title",
                            modifier = Modifier.padding(start = Glass.sidePadding + 8.dp, bottom = 10.dp),
                        ) { name ->
                            BasicText(
                                name.ifEmpty { " " },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(color = Glass.text, fontSize = 30.sp, fontWeight = FontWeight.Medium, shadow = softShadow),
                            )
                        }
                        // The frosted glass shelf
                        val shelfShape = RoundedCornerShape(Glass.shelfCorner)
                        Box(
                            Modifier
                                .padding(horizontal = Glass.sidePadding - 16.dp)
                                .fillMaxWidth()
                                .frosted(shelfShape, Glass.shelfTint)
                                .border(1.dp, Glass.shelfEdge, shelfShape)
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(Glass.cardGap),
                            ) {
                                itemsIndexed(shelfApps, key = { _, a -> a.packageName }) { i, app ->
                                    AppCard(
                                        app = app,
                                        width = Glass.shelfCardWidth,
                                        isFavourite = app.packageName in favourites,
                                        modifier = if (i == 0) Modifier.focusRequester(firstCard) else Modifier,
                                        onFocused = {
                                            focusedName = app.label
                                            scope.launch { scroll.animateScrollTo(0) }
                                        },
                                        onClick = { onLaunch(app.packageName) },
                                        onMenu = { onToggleFavourite(app.packageName) },
                                    )
                                }
                            }
                        }
                    }
                }

                // ---------- Page 2: every app, on a frosted panel ----------
                val panelShape = RoundedCornerShape(Glass.shelfCorner)
                Column(
                    Modifier
                        .padding(horizontal = Glass.sidePadding - 16.dp)
                        .padding(bottom = 40.dp)
                        .fillMaxWidth()
                        .frosted(panelShape, Glass.panelTint)
                        .border(1.dp, Glass.shelfEdge, panelShape)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    if (!autoOpenOn) {
                        SetupCard(onSetUpAutoOpen)
                        Spacer(Modifier.height(18.dp))
                    }
                    SectionTitle("All apps  ·  press ☰ on an app to add it to the shelf")
                    apps.chunked(Glass.appsPerRow).forEach { row ->
                        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(Glass.cardGap)) {
                            row.forEach { app ->
                                AppCard(
                                    app = app,
                                    width = Glass.gridCardWidth,
                                    isFavourite = app.packageName in favourites,
                                    showLabel = true,
                                    onFocused = { focusedName = app.label },
                                    onClick = { onLaunch(app.packageName) },
                                    onMenu = { onToggleFavourite(app.packageName) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SectionTitle("More")
                    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(Glass.cardGap)) {
                        ActionCard("⚙", "Settings", onFocused = { focusedName = "Settings" }, onClick = onOpenSettings)
                        ActionCard("⌂", "Amazon Home", onFocused = { focusedName = "Amazon Home" }, onClick = onAmazonHome)
                    }
                }
            }
        }
    }

    LaunchedEffect(loaded) {
        if (loaded && apps.isNotEmpty()) {
            delay(60)
            runCatching { firstCard.requestFocus() }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        style = TextStyle(color = Glass.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, shadow = softShadow),
    )
}

@Composable
private fun AppCard(
    app: AppEntry,
    width: Dp,
    isFavourite: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Card(
        focused = focused,
        width = width,
        label = if (showLabel) app.label else null,
        modifier = modifier
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent {
                if (it.key == Key.Menu && it.type == KeyEventType.KeyUp) { onMenu(); true } else false
            },
        background = if (app.banner != null) Color.Black else Glass.cardPlain,
        onClick = onClick,
    ) {
        if (app.banner != null) {
            Image(app.banner, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                app.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(40.dp)) }
                Spacer(Modifier.width(8.dp))
                BasicText(
                    app.label, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = Glass.cardText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
        if (isFavourite) {
            BasicText(
                "★", style = TextStyle(color = Color.White, fontSize = 12.sp, shadow = softShadow),
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
    }
}

@Composable
private fun ActionCard(symbol: String, label: String, onFocused: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Card(
        focused = focused,
        width = Glass.gridCardWidth,
        label = null,
        modifier = Modifier.onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) onFocused()
        },
        background = Glass.cardPlain,
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            BasicText(symbol, style = TextStyle(color = Glass.cardText, fontSize = 26.sp))
            BasicText(label, style = TextStyle(color = Glass.cardText, fontSize = 13.sp, fontWeight = FontWeight.Medium))
        }
    }
}

/** A rounded 16:9 card that lifts, grows and gets a white ring and sheen when selected. */
@Composable
private fun Card(
    focused: Boolean,
    width: Dp,
    label: String?,
    modifier: Modifier,
    background: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Glass.cardCorner)
    val scale by animateFloatAsState(
        if (focused) Glass.focusScale else 1f,
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), label = "scale",
    )
    val sheen by animateFloatAsState(if (focused) 1f else 0f, tween(240), label = "sheen")
    val lift by animateDpAsState(if (focused) 20.dp else 3.dp, tween(240), label = "lift")

    Column(Modifier.width(width).zIndex(if (focused) 1f else 0f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(lift, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(background)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        ) {
            content()
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = sheen }
                    .border(3.dp, Glass.focusRing, shape)
                    .background(
                        Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.30f),
                            0.4f to Color.Transparent,
                            1f to Color.Transparent,
                        )
                    )
            )
        }
        if (label != null) {
            Spacer(Modifier.height(8.dp))
            BasicText(
                label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Glass.text, fontSize = 13.sp, shadow = softShadow),
            )
        }
    }
}

@Composable
private fun SetupCard(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(Glass.cardCorner)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Glass.cardPlain)
            .border(3.dp, if (focused) Glass.focusRing else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column {
            BasicText("Turn on auto-open", style = TextStyle(color = Glass.cardText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
            BasicText(
                "So Glass Home shows up by itself. Press select, then turn on Glass Home under Accessibility.",
                style = TextStyle(color = Glass.cardText.copy(alpha = 0.7f), fontSize = 13.sp),
            )
        }
    }
}
