package ai.cardeli.starter.feature

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log

/**
 * 앱이 뒤로 가도 얼지 않게 붙잡아 두는 포그라운드 서비스.
 *
 * 주문 중에는 내비가 앞으로 나와 이 앱이 백그라운드가 된다. 그러면 Android 14 프로세스 동결(freezer)로
 * 앱이 얼어서 위치(도착 감지)·창문(수령 감지)·기사 알림·음성 응답이 제때 처리되지 않았다(11/11 확인, isFrozen=true).
 * 포그라운드 서비스가 떠 있으면 동결되지 않는다.
 */
class KeepAliveService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "주문 진행", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("카딜")
            .setContentText("주문을 이어서 처리하고 있어요")
            .setOngoing(true)
            .build()
        startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        Log.i(TAG, "포그라운드 서비스 시작")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "KeepAlive"
        private const val CHANNEL = "order"
        private const val ID = 1

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, KeepAliveService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "포그라운드 서비스 시작 실패", e)
            }
        }
    }
}
