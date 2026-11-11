package ai.cardeli.starter.feature.sdk

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 주행 중인지(주행 제한 UXR이 켜졌는지)를 흘려 보낸다. (F1)
 * Car 서비스가 없는 기기(일반 폰 AVD 등)에서는 false 하나만 보낸다.
 */
class DrivingStateSource(private val context: Context) {

    fun isDriving(): Flow<Boolean> = callbackFlow {
        val car = try {
            Car.createCar(context)
        } catch (e: Exception) {
            Log.w(TAG, "Car 연결 실패, 정차로 간주", e)
            null
        }
        val ux = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager

        if (ux == null) {
            trySend(false)
        } else {
            trySend(ux.currentCarUxRestrictions?.isRequiresDistractionOptimization ?: false)
            ux.registerListener { r -> trySend(r.isRequiresDistractionOptimization) }
        }

        awaitClose {
            try { ux?.unregisterListener() } catch (_: Exception) {}
            car?.disconnect()
        }
    }.distinctUntilChanged()

    private companion object {
        const val TAG = "DrivingState"
    }
}
