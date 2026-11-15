package ai.cardeli.starter.feature.demo

import ai.cardeli.starter.feature.geo.LatLng
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 시연·테스트용 adb 명령 (F14). 화면을 건드리지 않고 앱을 조작한다.
 * 주행 장면에서 화면을 터치하면 터치 카운터가 올라가므로, 촬영 중 조작은 전부 이 명령으로 한다.
 * 사용법은 docs/demo.md
 */
sealed interface DemoCommand {
    /** 처음 상태로 되돌린다 (주행 여부는 차량 상태를 따른다) */
    data object Reset : DemoCommand

    /** 현재 state를 files/demo_state.json에 저장한다. 스모크 테스트가 읽는다 */
    data object Dump : DemoCommand

    /** 녹음해 둔 대사 파일(assets/voice/<name>.pcm)을 음성 인식에 넣는다 (F2) */
    data class Utter(val name: String) : DemoCommand

    /** S1 목적지(기흥역)로 내비 안내를 시작한다. 운전자가 목적지를 찍고 출발한 상태를 만든다 (F8) */
    data object Route : DemoCommand

    /** 앱이 내비로 수령 후보(와 가게 들러 포장)의 추가 거리·시간을 직접 잰다. 1~2분 걸린다 (F7 실시간 계산) */
    data object Measure : DemoCommand

    /** 포그라운드 서비스를 켜고 끈다. 스모크의 동결 감지가 실제로 잡는지 확인할 때만 쓴다 */
    data class KeepAlive(val on: Boolean) : DemoCommand

    /** 문장을 음성으로 말한다 (F4 확인용) */
    data class Say(val text: String) : DemoCommand

    /** 배달기사 위치 (F11) */
    data class Rider(val location: LatLng) : DemoCommand

    /** 주문 진행 단계를 넘긴다 (예: Cooking). 기사 단계(Delivering·RiderComing·Arrived)는 기사 좌표로 정해진다 */
    data class Progress(val status: String) : DemoCommand

    /** 충전구·배터리 잔량을 직접 넣는다. Virtual Console로 못 바꿀 때 쓴다 (F16) */
    data class Charge(val portOpen: Boolean, val soc: Int?) : DemoCommand

    /** 화면 대신 OrderEvent를 보낸다. name: AddMenu·RemoveMenu(id 필요), ReviewOrder, BackToMenu, PlaceOrder, ConfirmReceived */
    data class Event(val name: String, val id: String?) : DemoCommand

    companion object {
        /** intent extra를 읽어 명령으로 바꾼다. 모르는 명령이나 값이 빠지면 null */
        fun parse(extra: (String) -> String?): DemoCommand? = when (extra("cmd")?.lowercase()) {
            "reset" -> Reset
            "dump" -> Dump
            "route" -> Route
            "measure" -> Measure
            "keepalive" -> KeepAlive(extra("on") != "false")
            "utter" -> extra("name")?.let { Utter(it) }
            "say" -> extra("text")?.takeIf { it.isNotBlank() }?.let { Say(it) }
            "rider" -> {
                val lat = extra("lat")?.toDoubleOrNull()
                val lng = extra("lng")?.toDoubleOrNull()
                if (lat != null && lng != null) Rider(LatLng(lat, lng)) else null
            }
            "event" -> extra("name")?.let { Event(it, extra("id")) }
            "progress" -> extra("status")?.let { Progress(it) }
            "charge" -> Charge(extra("open") != "false", extra("soc")?.toIntOrNull())
            else -> null
        }
    }
}

/** 수신기 → ViewModel 전달 통로. 앱 프로세스 하나에 하나 */
object DemoBus {
    private val _commands = MutableSharedFlow<DemoCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<DemoCommand> = _commands.asSharedFlow()

    fun send(command: DemoCommand) {
        _commands.tryEmit(command)
    }
}
