package ai.cardeli.starter.check

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import android.app.Activity
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView

/**
 * Pleos Connect 에뮬레이터에서 기획서의 "확인 필요" 항목을 직접 불러보는 시험 앱.
 * - Pleos Vehicle SDK: CarInfo(getCRNInfo, getVINInfo), Gear(getCurrentGearSelectionState + 리스너)
 * - AAOS Car API: PERF_VEHICLE_SPEED, GEAR_SELECTION, INFO_MAKE, INFO_MODEL, CarUxRestrictions
 */
class VehicleCheckActivity : Activity() {

    private val tag = "PleosStarter"
    private val main = Handler(Looper.getMainLooper())
    private val results = linkedMapOf<String, String>()
    private lateinit var text: TextView

    private lateinit var vehicle: Vehicle
    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var uxManager: CarUxRestrictionsManager? = null

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) = put("[Pleos] 기어 변경 이벤트", gear?.name ?: "null")
        override fun onFailed(e: Exception) = put("[Pleos] 기어 리스너 실패", e.describe())
    }

    // 운전석 창문: 열렸다 닫히면 "수령 완료"로 보는 흐름을 시험
    private val driverWindow = WindowArea(WindowValue.ROW_1_LEFT)
    private val windowEvents = mutableListOf<Int>()
    private val windowListener = object : WindowPositionListener {
        override fun onWindowPositionUpdated(pos: Int?) {
            pos?.let { windowEvents += it }
            put("[Pleos] 운전석 창문 이벤트", windowEvents.joinToString(" → "))
            // 0보다 큰 값(열림)이 한 번 나온 뒤 0(닫힘)으로 돌아오면 수령 완료로 판단
            if (windowEvents.size >= 2 && windowEvents.last() == 0 && windowEvents.any { it > 0 }) {
                put("[판단] 수령 완료", "창문 열림 → 닫힘 감지")
            }
        }
        override fun onFailed(e: Exception) = put("[Pleos] 창문 리스너 실패", e.describe())
    }

    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED ->
                    put("[AAOS] 속도 (m/s)", value.value.toString())
                VehiclePropertyIds.GEAR_SELECTION ->
                    put("[AAOS] 기어", aaosGearName(value.value as Int))
            }
        }
        override fun onErrorEvent(propertyId: Int, zone: Int) =
            put("[AAOS] 오류 property=$propertyId", "zone=$zone")
    }

    private val uxListener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { r ->
        put("[AAOS] 주행 제한(DO 필요)", describe(r))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        text = TextView(this).apply {
            textSize = 22f
            typeface = Typeface.MONOSPACE
            setPadding(48, 48, 48, 48)
        }
        setContentView(ScrollView(this).apply { addView(text) })
        put("상태", "시작")

        startPleosVehicle()
        startAaosCar()
    }

    // ---------- Pleos Vehicle SDK ----------
    private fun startPleosVehicle() {
        vehicle = Vehicle(this)
        try {
            vehicle.initialize()
        } catch (e: Exception) {
            put("[Pleos] initialize 실패", e.describe())
            return
        }
        // 서비스 바인딩이 끝날 시간을 조금 준 뒤 호출
        main.postDelayed({ readPleos() }, 1500)
    }

    private fun readPleos() {
        val carInfo = vehicle.getCarInfo()
        carInfo.getCRNInfo(
            { crn -> put("[Pleos] 차량 등록번호(CRN)", crn ?: "null") },
            { e -> put("[Pleos] 차량 등록번호(CRN)", "실패: ${e.describe()}") }
        )
        carInfo.getVINInfo(
            { vin -> put("[Pleos] 차대번호(VIN)", vin ?: "null") },
            { e -> put("[Pleos] 차대번호(VIN)", "실패: ${e.describe()}") }
        )
        val gear = vehicle.getGear()
        gear.getCurrentGearSelectionState(
            { g -> put("[Pleos] 현재 기어", g?.name ?: "null") },
            { e -> put("[Pleos] 현재 기어", "실패: ${e.describe()}") }
        )
        try {
            gear.registerGearSelection(gearListener)
        } catch (e: Exception) {
            put("[Pleos] 기어 리스너 등록", "실패: ${e.describe()}")
        }
        readWindow()
    }

    private fun readWindow() {
        val window = vehicle.getWindow()
        window.checkWindowPositionCapabilities(
            { bounds -> put("[Pleos] 창문 지원 범위", bounds.entries.joinToString { "${it.key.values.joinToString()}=${it.value}" }) },
            { e -> put("[Pleos] 창문 지원 범위", "실패: ${e.describe()}") }
        )
        window.getCurrentWindowPosition(driverWindow,
            { p -> put("[Pleos] 운전석 창문 위치", p?.toString() ?: "null") },
            { e -> put("[Pleos] 운전석 창문 위치", "실패: ${e.describe()}") }
        )
        try {
            window.registerWindowPosition(driverWindow, windowListener)
        } catch (e: Exception) {
            put("[Pleos] 창문 리스너 등록", "실패: ${e.describe()}")
        }
        // 시험: 3초 뒤 창문을 열고(최대 10), 3초 더 뒤 닫아서(0) 리스너가 잡는지 본다
        main.postDelayed({ moveWindow(10) }, 3000)
        main.postDelayed({ moveWindow(0) }, 6000)
    }

    private fun moveWindow(pos: Int) {
        vehicle.getWindow().setWindowPosition(driverWindow, pos,
            { _, p ->
                put("[시험] 창문 이동 요청", "$p 완료")
                // 요청 직후와 1초 뒤 실제 값을 다시 읽어본다
                for (delay in listOf(200L, 1000L)) main.postDelayed({
                    vehicle.getWindow().getCurrentWindowPosition(driverWindow,
                        { now -> put("[시험] 요청 $pos 후 ${delay}ms 실제 위치", now?.toString() ?: "null") },
                        { e -> put("[시험] 요청 $pos 후 재조회", "실패: ${e.describe()}") })
                }, delay)
            },
            { e -> put("[시험] 창문 이동 요청", "$pos 실패: ${e.describe()}") }
        )
    }

    // ---------- AAOS 표준 Car API ----------
    private fun startAaosCar() {
        try {
            val c = Car.createCar(this)
            car = c
            val pm = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            propertyManager = pm

            readString(pm, VehiclePropertyIds.INFO_MAKE, "[AAOS] 제조사(INFO_MAKE)")
            readString(pm, VehiclePropertyIds.INFO_MODEL, "[AAOS] 차종(INFO_MODEL)")

            pm.registerCallback(propertyCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_ONCHANGE)
            pm.registerCallback(propertyCallback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)

            val ux = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            uxManager = ux
            put("[AAOS] 주행 제한(DO 필요)", describe(ux.currentCarUxRestrictions))
            ux.registerListener(uxListener)
        } catch (e: Exception) {
            put("[AAOS] Car 연결 실패", e.describe())
        }
    }

    private fun readString(pm: CarPropertyManager, id: Int, label: String) {
        try {
            put(label, pm.getProperty(String::class.java, id, 0)?.value ?: "null")
        } catch (e: Exception) {
            put(label, "실패: ${e.describe()}")
        }
    }

    // ---------- 표시 ----------
    private fun put(key: String, value: String) {
        Log.i(tag, "$key = $value")
        main.post {
            results[key] = value
            text.text = results.entries.joinToString("\n\n") { "${it.key}\n  ${it.value}" }
        }
    }

    private fun describe(r: CarUxRestrictions?): String =
        if (r == null) "null"
        else "requiresDO=${r.isRequiresDistractionOptimization}, restrictions=${r.activeRestrictions}"

    private fun aaosGearName(v: Int) = when (v) {
        1 -> "NEUTRAL"; 2 -> "REVERSE"; 4 -> "PARK"; 8 -> "DRIVE"; else -> "기타($v)"
    }

    private fun Exception.describe() = "${javaClass.simpleName}: $message"

    override fun onDestroy() {
        try { vehicle.getGear().unregisterGearSelection(gearListener) } catch (_: Exception) {}
        try { vehicle.getWindow().unregisterWindowPosition(driverWindow, windowListener) } catch (_: Exception) {}
        try { vehicle.release() } catch (_: Exception) {}
        propertyManager?.unregisterCallback(propertyCallback)
        uxManager?.unregisterListener()
        car?.disconnect()
        super.onDestroy()
    }
}
