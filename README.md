# Glass Home

A custom home screen for the Fire TV Stick 4K Max, in a frosted-glass style inspired by Apple TV.

- All your installed apps, with their TV artwork, on drifting colour glows behind frosted tiles
- Tiles lift, brighten and shine when you land on them
- A Favourites row up top: press the **☰ (menu)** button on any app to add or remove it
- Opens by itself when the TV starts and when you press Home (Amazon's screen may flash briefly first)

## Install or update on the Fire TV

1. Open **Downloader**, and in the address box type:
   `https://github.com/OwenjSwift/fire-launcher/releases/latest/download/GlassHome.apk`
2. Press **Go**, then **Install**. If Fire TV asks, allow Downloader to install unknown apps.
3. Open **Glass Home** from Your Apps.

The same link always gives you the newest version, so updating is the same three steps.

## Turn on auto-open (one time)

Glass Home shows a **Turn on auto-open** card until this is done.

Settings → Accessibility → **Glass Home** → turn it on.

That's the part that notices Amazon's home screen and swaps it for Glass Home. It only listens for
Amazon's launcher and reads nothing else.

If you ever want Amazon's own home, pick **Amazon Home** under *More*: auto-open pauses for 10 minutes.

## How it's built

Kotlin and Jetpack Compose. Every push to `main` is built by GitHub Actions and published as a release.
To restyle it, start with `app/src/main/java/com/swifty/glasshome/Theme.kt`.
