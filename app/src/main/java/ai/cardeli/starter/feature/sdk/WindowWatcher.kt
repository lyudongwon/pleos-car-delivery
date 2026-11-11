package ai.cardeli.starter.feature.sdk

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 운전석 창문 위치 (F13). Pleos Vehicle SDK, 값 0(닫힘)~10(열림). 권한 CAR_WINDOWS.
 * 에뮬레이터에서는 Virtual Console(`demo_window 100` / `demo_window 0`)로 열고 닫는다.
 */
class WindowWatcher(context: Context, private val onPosition: (Int) -> Unit) {

    private val vehicle = Vehicle(context)
    private val driverWindow = WindowArea(WindowValue.ROW_1_LEFT)

    private val listener = object : WindowPositionListener {
        override fun onWindowPositionUpdated(position: Int?) {
            Log.i(TAG, "운전석 창문 $position")
            position?.let(onPosition)
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "창문 리스너 실패 ${e.message}")
        }
    }

    init {
        try {
            vehicle.initialize()
            // 서비스 바인딩이 끝날 시간을 준다 (VehicleCheckActivity에서 확인한 방식)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    vehicle.getWindow().registerWindowPosition(driverWindow, listener)
                } catch (e: Exception) {
                    Log.w(TAG, "창문 리스너 등록 실패", e)
                }
            }, 1_500)
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle 초기화 실패", e)
        }
    }

    fun release() {
        try { vehicle.getWindow().unregisterWindowPosition(driverWindow, listener) } catch (_: Exception) {}
        try { vehicle.release() } catch (_: Exception) {}
    }

    private companion object {
        const val TAG = "WindowWatcher"
    }
}
