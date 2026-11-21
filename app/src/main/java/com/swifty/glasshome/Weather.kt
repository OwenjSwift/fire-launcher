package com.swifty.glasshome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class Weather(
    val tempC: Int,
    val highC: Int,
    val lowC: Int,
    val code: Int,
    val isDay: Boolean,
) {
    /** A symbol and a word for the current conditions (WMO weather codes). */
    val symbol: String get() = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1, 2 -> if (isDay) "🌤️" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "🌨️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }
    val words: String get() = when (code) {
        0 -> if (isDay) "Sunny" else "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Foggy"
        in 51..57 -> "Drizzle"
        in 61..67, in 80..82 -> "Rain"
        in 71..77, 85, 86 -> "Snow"
        in 95..99 -> "Storms"
        else -> ""
    }
}

/** Free weather from open-meteo.com (no account or key needed). */
object WeatherRepo {
    suspend fun fetch(): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${Glass.weatherLat}&longitude=${Glass.weatherLon}" +
                    "&current=temperature_2m,weather_code,is_day" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto&forecast_days=1"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            val body = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val json = JSONObject(body)
            val now = json.getJSONObject("current")
            val day = json.getJSONObject("daily")
            Weather(
                tempC = now.getDouble("temperature_2m").roundToInt(),
                highC = day.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                lowC = day.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                code = now.getInt("weather_code"),
                isDay = now.optInt("is_day", 1) == 1,
            )
        }.getOrNull()
    }
}
