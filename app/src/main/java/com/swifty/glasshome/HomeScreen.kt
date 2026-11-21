package com.swifty.glasshome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.foundation.text.BasicText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    favourites: List<String>,
    loaded: Boolean,
    autoOpenOn: Boolean,
    onLaunch: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSetUpAutoOpen: () -> Unit,
    onAmazonHome: () -> Unit,
) {
    var focusedName by remember { mutableStateOf("") }
    val firstTile = remember { FocusRequester() }
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    val favApps = favourites.mapNotNull { byPkg[it] }

    Box(Modifier.fillMaxSize()) {
        GlassWallpaper()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 36.dp, bottom = 48.dp)
        ) {
            Header(Modifier.padding(horizontal = Glass.sidePadding))

            // The big name of whatever you're on, like the Apple TV top shelf.
            AnimatedContent(
                targetState = focusedName,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "title",
                modifier = Modifier.padding(horizontal = Glass.sidePadding, vertical = 18.dp),
            ) { name ->
                BasicText(
                    name.ifEmpty { " " },
                    style = TextStyle(color = Glass.text, fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!autoOpenOn) {
                SetupCard(onSetUpAutoOpen, Modifier.padding(horizontal = Glass.sidePadding))
                Spacer(Modifier.height(20.dp))
            }

            var firstAssigned = false
            fun firstModifier(): Modifier =
                if (!firstAssigned) { firstAssigned = true; Modifier.focusRequester(firstTile) } else Modifier

            if (favApps.isNotEmpty()) {
                SectionTitle("Favourites")
                val favFirst = firstModifier()
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Glass.sidePadding, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(Glass.tileGap),
                ) {
                    items(favApps, key = { it.packageName }) { app ->
                        AppTile(
                            app = app,
                            onFocused = { focusedName = app.label },
                            onClick = { onLaunch(app.packageName) },
                            onMenu = { onToggleFavourite(app.packageName) },
                            modifier = if (app == favApps.first()) favFirst else Modifier,
                        )
                    }
                }
            }

            SectionTitle(if (favApps.isEmpty()) "Your apps  ·  press ☰ on one to add it to Favourites" else "All apps")
            apps.chunked(Glass.appsPerRow).forEachIndexed { rowIndex, row ->
                val rowFirst = if (rowIndex == 0) firstModifier() else Modifier
                Row(
                    Modifier.padding(horizontal = Glass.sidePadding, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(Glass.tileGap),
                ) {
                    row.forEachIndexed { i, app ->
                        AppTile(
                            app = app,
                            isFavourite = app.packageName in favourites,
                            onFocused = { focusedName = app.label },
                            onClick = { onLaunch(app.packageName) },
                            onMenu = { onToggleFavourite(app.packageName) },
                            modifier = if (i == 0) rowFirst else Modifier,
                        )
                    }
                }
            }

            SectionTitle("More")
            Row(
                Modifier.padding(horizontal = Glass.sidePadding, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(Glass.tileGap),
            ) {
                ActionTile("⚙", "Settings", onFocused = { focusedName = "Settings" }, onClick = onOpenSettings)
                ActionTile("⌂", "Amazon Home", onFocused = { focusedName = "Amazon Home (10 min)" }, onClick = onAmazonHome)
            }
        }
    }

    // Put the remote's focus on the first app once everything has loaded.
    LaunchedEffect(loaded) {
        if (loaded && apps.isNotEmpty()) {
            delay(50)
            runCatching { firstTile.requestFocus() }
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            val msToNextMinute = 60_000 - (System.currentTimeMillis() % 60_000)
            delay(msToNextMinute + 50)
        }
    }
    val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            BasicText(greeting, style = TextStyle(color = Glass.textSoft, fontSize = 18.sp))
            BasicText(
                SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now),
                style = TextStyle(color = Glass.text, fontSize = 22.sp, fontWeight = FontWeight.Medium),
            )
        }
        BasicText(
            SimpleDateFormat("h:mm", Locale.getDefault()).format(now),
            style = TextStyle(color = Glass.text, fontSize = 48.sp, fontWeight = FontWeight.Light),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text,
        modifier = Modifier.padding(start = Glass.sidePadding, top = 14.dp),
        style = TextStyle(color = Glass.textSoft, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    )
}

/** The frosted-glass tile. It lifts, brightens and gets a soft shine when you land on it. */
@Composable
private fun AppTile(
    app: AppEntry,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    isFavourite: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    GlassTile(
        focused = focused,
        label = app.label,
        modifier = modifier
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent {
                if (it.key == Key.Menu && it.type == KeyEventType.KeyUp) { onMenu(); true } else false
            },
        onClick = onClick,
    ) {
        if (app.banner != null) {
            Image(app.banner, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                app.icon?.let { Image(it, contentDescription = app.label, modifier = Modifier.size(44.dp)) }
                Spacer(Modifier.height(6.dp))
                BasicText(
                    app.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = Glass.text, fontSize = 12.sp),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        if (isFavourite) {
            BasicText("★", style = TextStyle(color = Color.White, fontSize = 12.sp), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
    }
}

@Composable
private fun ActionTile(symbol: String, label: String, onFocused: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    GlassTile(
        focused = focused,
        label = label,
        modifier = Modifier.onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) onFocused()
        },
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            BasicText(symbol, style = TextStyle(color = Glass.text, fontSize = 30.sp))
            BasicText(label, style = TextStyle(color = Glass.textSoft, fontSize = 12.sp))
        }
    }
}

@Composable
private fun GlassTile(
    focused: Boolean,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Glass.corner)
    val springy = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
    val scale by animateFloatAsState(if (focused) Glass.focusScale else 1f, springy, label = "scale")
    val shine by animateFloatAsState(if (focused) 1f else 0f, tween(260), label = "shine")
    val lift by animateDpAsState(if (focused) 22.dp else 2.dp, tween(260), label = "lift")

    Column(
        Modifier.width(Glass.tileWidth).zIndex(if (focused) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(lift, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(if (focused) Glass.tileFillFocused else Glass.tileFill)
                .border(1.5.dp, if (focused) Glass.tileEdgeFocused else Glass.tileEdge, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        ) {
            content()
            // Soft diagonal shine across the glass when selected.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shine }
                    .background(
                        Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.28f),
                            0.45f to Color.Transparent,
                            1f to Color.Transparent,
                        )
                    )
            )
        }
        Spacer(Modifier.height(10.dp))
        BasicText(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Glass.text, fontSize = 13.sp),
            modifier = Modifier.graphicsLayer { alpha = shine },
        )
    }
}

@Composable
private fun SetupCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(Glass.corner)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) Glass.tileFillFocused else Glass.tileFill)
            .border(1.5.dp, if (focused) Glass.tileEdgeFocused else Glass.tileEdge, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
        Column {
            BasicText("Turn on auto-open", style = TextStyle(color = Glass.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold))
            BasicText(
                "So Glass Home shows up by itself. Press select, then turn on Glass Home under Accessibility.",
                style = TextStyle(color = Glass.textSoft, fontSize = 14.sp),
            )
        }
    }
}

/** The wallpaper: soft colour glows drifting slowly behind frosted glass. */
@Composable
private fun GlassWallpaper() {
    val drift = rememberInfiniteTransition(label = "wallpaper")
    val t by drift.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t",
    )
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Glass.sky)
        val w = size.width
        val h = size.height
        glow(Glass.glowA, Offset(w * (0.15f + 0.20f * t), h * (0.25f + 0.10f * t)), w * 0.55f)
        glow(Glass.glowB, Offset(w * (0.85f - 0.25f * t), h * (0.20f + 0.25f * t)), w * 0.50f)
        glow(Glass.glowC, Offset(w * (0.55f + 0.15f * t), h * (0.95f - 0.20f * t)), w * 0.60f)
        glow(Glass.glowD, Offset(w * (0.05f + 0.10f * t), h * (0.95f - 0.10f * t)), w * 0.35f)
        // Darken the bottom a little so the tiles and text stay easy to read.
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.55f))))
    }
}

private fun DrawScope.glow(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), center, radius),
        radius = radius,
        center = center,
    )
}
