package com.swifty.glasshome

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.graphics.FilterQuality
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    var focusedName by remember { mutableStateOf("") }
    val firstCard = remember { FocusRequester() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    val shelfApps = remember(apps, favourites) { favourites.mapNotNull { byPkg[it] }.ifEmpty { apps.take(6) } }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF2A2035))) {
        val pageHeight = maxHeight

        // The photo (already blurred once when it loaded). Drawn as one plain image.
        wallpaper?.let {
            Image(
                it, contentDescription = null, contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High, modifier = Modifier.fillMaxSize(),
            )
        }
        // A very light shade at the bottom so the shelf sits nicely
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.25f))
            )
        )

        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // ---------- Page 1: clock and the shelf, laid out top to bottom ----------
            Column(Modifier.fillMaxWidth().height(pageHeight)) {
                Row(
                    Modifier.fillMaxWidth().height(40.dp).padding(top = 18.dp, end = Glass.sidePadding),
                    horizontalArrangement = Arrangement.End,
                ) {
                    weather?.let { WeatherPill(it) }
                }

                GlassClock(now, Modifier.fillMaxWidth())

                Spacer(Modifier.weight(1f))

                AnimatedContent(
                    targetState = focusedName,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "title",
                    modifier = Modifier.padding(start = Glass.sidePadding + 4.dp, bottom = 8.dp),
                ) { name ->
                    BasicText(
                        name.ifEmpty { " " },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Glass.text, fontSize = 22.sp, fontWeight = FontWeight.Medium, shadow = softShadow),
                    )
                }
                // The glass shelf
                val shelfShape = RoundedCornerShape(Glass.shelfCorner)
                Box(
                    Modifier
                        .padding(start = Glass.sidePadding - 14.dp, end = Glass.sidePadding - 14.dp, bottom = 26.dp)
                        .fillMaxWidth()
                        .clip(shelfShape)
                        .background(Glass.glass)
                        .border(1.dp, Glass.glassEdge, shelfShape)
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(Glass.cardGap),
                    ) {
                        itemsIndexed(shelfApps, key = { _, a -> a.packageName }) { i, app ->
                            AppCard(
                                app = app,
                                width = Glass.shelfCardWidth,
                                modifier = if (i == 0) Modifier.focusRequester(firstCard) else Modifier,
                                onFocused = {
                                    focusedName = app.label
                                    if (scroll.value != 0) scope.launch { scroll.animateScrollTo(0) }
                                },
                                onClick = { onLaunch(app.packageName) },
                                onMenu = { onToggleFavourite(app.packageName) },
                            )
                        }
                    }
                }
            }

            // ---------- Page 2: every app ----------
            val panelShape = RoundedCornerShape(Glass.shelfCorner)
            Column(
                Modifier
                    .padding(horizontal = Glass.sidePadding - 16.dp)
                    .padding(bottom = 40.dp)
                    .fillMaxWidth()
                    .clip(panelShape)
                    .background(Glass.glass)
                    .border(1.dp, Glass.glassEdge, panelShape)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                if (!autoOpenOn) {
                    SetupCard(onSetUpAutoOpen)
                    Spacer(Modifier.height(18.dp))
                }
                SectionTitle("All apps")
                apps.chunked(Glass.appsPerRow).forEach { row ->
                    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(Glass.cardGap)) {
                        row.forEach { app ->
                            AppCard(
                                app = app,
                                width = Glass.gridCardWidth,
                                onFocused = { focusedName = app.label },
                                onClick = { onLaunch(app.packageName) },
                                onMenu = { onToggleFavourite(app.packageName) },
                            )
                        }
                    }
                }
                BasicText(
                    "Press ☰ on any app to add it to, or remove it from, the shelf.",
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
                    style = TextStyle(color = Glass.textSoft, fontSize = 13.sp),
                )
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(Glass.cardGap)) {
                    ActionCard("Settings", onFocused = { focusedName = "Settings" }, onClick = onOpenSettings)
                    ActionCard("Amazon Home", onFocused = { focusedName = "Amazon Home" }, onClick = onAmazonHome)
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
        style = TextStyle(color = Glass.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, shadow = softShadow),
    )
}

@Composable
private fun AppCard(
    app: AppEntry,
    width: Dp,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Card(
        focused = focused,
        width = width,
        modifier = modifier
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
            // No TV artwork: a card in the app's own colour with its icon and name, Apple TV style.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent))
                )
            )
            Column(
                Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                app.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(32.dp)) }
                Spacer(Modifier.height(6.dp))
                BasicText(
                    app.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun ActionCard(label: String, onFocused: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Card(
        focused = focused,
        width = Glass.gridCardWidth,
        modifier = Modifier.onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) onFocused()
        },
        background = Color.White.copy(alpha = 0.22f),
        onClick = onClick,
    ) {
        BasicText(
            label,
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/**
 * A rounded card. When selected it grows a little and lifts on a soft shadow, the way
 * Apple TV does it. Nothing else moves, and unselected cards have no shadow at all,
 * which keeps things smooth on the Fire Stick.
 */
@Composable
private fun Card(
    focused: Boolean,
    width: Dp,
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
            .width(width)
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
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = if (focused) 0.35f else 0.2f))
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column {
            BasicText("Turn on auto-open", style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
            BasicText(
                "So Glass Home shows up by itself. Press select, then turn on Glass Home under Accessibility.",
                style = TextStyle(color = Glass.textSoft, fontSize = 13.sp),
            )
        }
    }
}
