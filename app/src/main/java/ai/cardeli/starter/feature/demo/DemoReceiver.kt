package ai.cardeli.starter.feature.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * adb shell am broadcast -n ai.cardeli.starter/.feature.demo.DemoReceiver --es cmd <명령> [...]
 * 값은 모두 문자열(--es)로 보낸다. 좌표를 --ef로 보내면 float라 정밀도가 깨진다.
 */
class DemoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val command = DemoCommand.parse { intent.getStringExtra(it) }
        if (command == null) {
            Log.w(TAG, "알 수 없는 명령: ${intent.extras?.keySet()?.associateWith { intent.getStringExtra(it) }}")
            return
        }
        Log.i(TAG, "명령 $command")
        DemoBus.send(command)
    }

    companion object {
        const val TAG = "Demo"
    }
}
