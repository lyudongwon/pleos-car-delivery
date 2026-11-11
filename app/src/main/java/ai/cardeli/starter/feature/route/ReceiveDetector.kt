package ai.cardeli.starter.feature.route

/**
 * 창문으로 수령 감지 (F13). 수령 지점에 도착한 뒤(armed) 운전석 창문이 열렸다가(0보다 큼) 다시 닫히면(0)
 * 음식을 받은 것으로 "후보"를 잡는다. 확정은 음성으로 묻는다(창문만으로는 환기일 수도 있어서).
 * 값은 Pleos Vehicle SDK 창문 위치 0(닫힘)~10(열림).
 */
class ReceiveDetector {

    private var armed = false
    private var opened = false
    private var fired = false

    /** 수령 지점에 도착하면 켠다 */
    fun arm() {
        armed = true
        opened = false
        fired = false
    }

    fun reset() {
        armed = false
        opened = false
        fired = false
    }

    /** 창문 위치가 바뀔 때마다 부른다. 열림 → 닫힘이 처음 완성되면 true */
    fun onWindow(position: Int): Boolean {
        if (!armed || fired) return false
        if (position > 0) opened = true
        if (position == 0 && opened) {
            fired = true
            return true
        }
        return false
    }
}
