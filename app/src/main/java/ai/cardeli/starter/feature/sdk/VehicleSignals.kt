package ai.cardeli.starter.feature.sdk

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 차에서 받는 신호 (Pleos Vehicle SDK).
 * - 기어: P이면 지금 차 위치를 받는 곳으로 확정한다 (F10)
 * - 충전: 충전구 열림 + 배터리 잔량(SOC) 리스너 (F16). 권한 pleos.car.permission.CAR_ENERGY
 * 에뮬레이터에서는 Virtual Console로 기어·충전구·SOC를 바꾼다.
 */
class VehicleSignals(
    context: Context,
    private val onGear: (parked: Boolean) -> Unit,
    private val onCharge: (portOpen: Boolean, soc: Int?) -> Unit,
) {
    private val vehicle = Vehicle(context)
    private val handler = Handler(Looper.getMainLooper())
    private var portOpen = false
    private var soc: Int? = null

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            Log.i(TAG, "기어 $gear")
            onGear(gear == VehicleGear.GEAR_PARK)
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "기어 리스너 실패 ${e.message}")
        }
    }

    private val socListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(value: Int?) {
            if (value == null || value == soc) return
            soc = value
            Log.i(TAG, "배터리 잔량 $value%")
            onCharge(portOpen, value)
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "배터리 리스너 실패 ${e.message}")
        }
    }

    private val portListener = object : EvChargePortOpenListener {
        override fun onEvChargePortOpenUpdated(value: Boolean?) {
            val open = value == true
            if (open == portOpen) return
            portOpen = open
            Log.i(TAG, "충전구 ${if (open) "열림" else "닫힘"}")
            onCharge(open, soc)
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "충전구 리스너 실패 ${e.message}")
        }
    }

    init {
        try {
            vehicle.initialize()
            // 서비스 바인딩이 끝날 시간을 준다 (VehicleCheckActivity에서 확인한 방식)
            handler.postDelayed({
                try {
                    vehicle.getGear().registerGearSelection(gearListener)
                    vehicle.getGear().getCurrentGearSelectionState({ onGear(it == VehicleGear.GEAR_PARK) }, { e -> Log.w(TAG, "기어 조회 실패 ${e.message}") })
                } catch (e: Exception) {
                    Log.w(TAG, "기어 리스너 등록 실패", e)
                }
                try {
                    vehicle.getEvBattery().registerEvBatteryStateOfCharge(socListener)
                } catch (e: Exception) {
                    Log.w(TAG, "배터리 리스너 등록 실패", e)
                }
                try {
                    vehicle.getEvBattery().registerEvChargePortOpen(portListener)
                } catch (e: Exception) {
                    Log.w(TAG, "충전구 리스너 등록 실패", e)
                }
            }, 1_500)
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle 초기화 실패", e)
        }
    }

    fun release() {
        try { vehicle.getGear().unregisterGearSelection(gearListener) } catch (_: Exception) {}
        try { vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(socListener) } catch (_: Exception) {}
        try { vehicle.getEvBattery().unregisterEvChargePortOpen(portListener) } catch (_: Exception) {}
        try { vehicle.release() } catch (_: Exception) {}
    }

    private companion object {
        const val TAG = "VehicleSignals"
    }
}
