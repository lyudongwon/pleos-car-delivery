package ai.cardeli.starter.feature.sdk

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log

/**
 * 차종 읽기 (F10). AAOS Car API INFO_MAKE·INFO_MODEL (에뮬레이터: "Hyundai 42dot" / "Connect").
 * 차량 번호는 Pleos getCRNInfo가 "invalid"를 돌려줘 못 쓰므로, 사용자가 등록한 값(데모 데이터)을 쓴다.
 */
object VehicleInfo {

    /** "제조사 차종". 읽지 못하면 null */
    fun carModel(context: Context): String? {
        var car: Car? = null
        return try {
            car = Car.createCar(context)
            val pm = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            val make = pm.getProperty(String::class.java, VehiclePropertyIds.INFO_MAKE, 0)?.value
            val model = pm.getProperty(String::class.java, VehiclePropertyIds.INFO_MODEL, 0)?.value
            listOfNotNull(make, model).joinToString(" ").ifBlank { null }
        } catch (e: Exception) {
            Log.w("VehicleInfo", "차종 읽기 실패", e)
            null
        } finally {
            car?.disconnect()
        }
    }
}
