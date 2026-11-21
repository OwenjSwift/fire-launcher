package com.swifty.glasshome

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private var apps by mutableStateOf<List<AppEntry>>(emptyList())
    private var favourites by mutableStateOf<List<String>>(emptyList())
    private var autoOpenOn by mutableStateOf(true)
    private var loaded by mutableStateOf(false)
    private var wallpaper by mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        // This is the home screen, so Back has nowhere to go.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        lifecycleScope.launch {
            wallpaper = withContext(Dispatchers.Default) { WallpaperRepo.load(this@MainActivity) }
        }

        setContent {
            HomeScreen(
                apps = apps,
                favourites = favourites,
                loaded = loaded,
                autoOpenOn = autoOpenOn,
                wallpaper = wallpaper,
                onLaunch = { AppRepository.launch(this, it) },
                onToggleFavourite = { favourites = prefs.toggleFavourite(favourites, it) },
                onOpenSettings = { openSettings(Settings.ACTION_SETTINGS) },
                onSetUpAutoOpen = { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
                onAmazonHome = { openAmazonHome() },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        prefs.clearPause()
        autoOpenOn = Prefs.isAutoOpenEnabled(this)
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) { AppRepository.loadApps(this@MainActivity) }
            apps = list
            favourites = prefs.favourites(list)
            loaded = true
            // Check GitHub for a new custom wallpaper photo
            val changed = withContext(Dispatchers.IO) { WallpaperRepo.refresh(this@MainActivity) }
            if (changed) {
                wallpaper = withContext(Dispatchers.Default) { WallpaperRepo.load(this@MainActivity) }
            }
        }
    }

    private fun openSettings(action: String) {
        val ok = runCatching {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!ok && action != Settings.ACTION_SETTINGS) openSettings(Settings.ACTION_SETTINGS)
    }

    private fun openAmazonHome() {
        prefs.pauseAutoOpen(minutes = 10)
        val home = packageManager.getLaunchIntentForPackage(HomeWatcherService.AMAZON_HOME)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        runCatching { startActivity(home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
