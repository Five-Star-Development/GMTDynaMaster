package dev.fivestar.gmtdynamaster.presentation

import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class GmtWatchFaceService : WatchFaceService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val store = LocationStore(applicationContext)

        // refresh Location; while none is known retry
        scope.launch {
            while (isActive) {
                store.refresh()
                delay(if (store.location.value == null) 1.minutes else 3.hours)
            }
        }

        val renderer = GmtRenderer(surfaceHolder, currentUserStyleRepository, watchState, store)
        return WatchFace(WatchFaceType.ANALOG, renderer)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}