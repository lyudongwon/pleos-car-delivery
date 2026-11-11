package ai.cardeli.starter.feature.sdk

import ai.cardeli.starter.feature.geo.LatLng
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 차 현재 위치 (F6 가게 추천, F10 차 위치 배달). Pleos FusedLocation.
 * 에뮬레이터에서는 `adb emu geo fix <경도> <위도>`를 따라간다(내비 위치와는 따로 움직인다).
 * 아직 위치를 못 받았으면 null.
 */
class LocationSource(context: Context) {

    private val fused = FusedLocation(context)
    private val _location = MutableStateFlow<LatLng?>(null)
    val location: StateFlow<LatLng?> = _location.asStateFlow()

    private val listener = object : FusedLocationListener {
        override fun onChanged(data: LocationData?) {
            if (data != null) _location.value = LatLng(data.latitude, data.longitude)
        }
        override fun onError(e: Exception) {
            Log.w(TAG, "위치 오류 ${e.message}")
        }
    }

    init {
        try {
            fused.initialize()
            // 서비스 연결이 끝날 시간을 조금 준다 (SdkCheckActivity에서 확인한 방식)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    fused.getLastKnownLocation(listener)
                    fused.registerFusedLocationCallback(listener)
                } catch (e: Exception) {
                    Log.w(TAG, "위치 등록 실패", e)
                }
            }, 1_500)
        } catch (e: Exception) {
            Log.w(TAG, "FusedLocation 초기화 실패", e)
        }
    }

    fun release() {
        try { fused.release() } catch (_: Exception) {}
    }

    private companion object {
        const val TAG = "LocationSource"
    }
}
