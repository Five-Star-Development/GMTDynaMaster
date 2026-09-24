package dev.fivestar.gmtdynamaster.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import org.shredzone.commons.suncalc.SunTimes
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import androidx.core.content.edit

data class LatLng(val lat: Double, val lng: Double)

data class DaySpan(
    val riseMin: Float,
    val setMin: Float,
    val alwaysUp: Boolean,
    val alwaysDown: Boolean,
) {
    companion object {
        /** Fallback without Location: classic 6–18 watch. */
        val DEFAULT = DaySpan(360f, 1080f, alwaysUp = false, alwaysDown = false)
    }
}

object SunCalc {
    fun compute(date: LocalDate, zone: ZoneId, loc: LatLng): DaySpan {
        val t = SunTimes.compute()
            .timezone(zone)
            .on(date.year, date.monthValue, date.dayOfMonth)
            .at(loc.lat, loc.lng)
            .oneDay()
            .execute()

        fun ZonedDateTime.minutes() = toLocalTime().toSecondOfDay() / 60f

        return DaySpan(
            riseMin = t.rise?.minutes() ?: 0f,
            setMin = t.set?.minutes() ?: 1440f,
            alwaysUp = t.isAlwaysUp,
            alwaysDown = t.isAlwaysDown,
        )
    }
}

/** Takes the last know positon, saves it in SharedPreferences. */
class LocationStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("gmt", Context.MODE_PRIVATE)
    private val _location = MutableStateFlow(load())
    val location: StateFlow<LatLng?> = _location

    private fun load(): LatLng? =
        if (prefs.contains("lat")) {
            LatLng(prefs.getFloat("lat", 0f).toDouble(), prefs.getFloat("lng", 0f).toDouble())
        } else null

    @SuppressLint("MissingPermission")
    suspend fun refresh() {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val client = LocationServices.getFusedLocationProviderClient(context)
        val loc = runCatching {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: client.lastLocation.await()
        }.getOrNull() ?: return

        prefs.edit {
            putFloat("lat", loc.latitude.toFloat())
                .putFloat("lng", loc.longitude.toFloat())
        }
        _location.value = LatLng(loc.latitude, loc.longitude)
    }
}