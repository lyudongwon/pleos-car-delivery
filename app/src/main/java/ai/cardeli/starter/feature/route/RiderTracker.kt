package ai.cardeli.starter.feature.route

/**
 * 배달기사 접근 판정 (F11). 기사 좌표와 받는 곳 좌표의 거리를 받아, 처음 [approachM] 안에 들어올 때와
 * 처음 [arrivedM] 안에 들어올 때 한 번씩 알린다. 거리가 다시 멀어졌다 가까워져도 같은 알림은 반복하지 않는다.
 */
class RiderTracker(private val approachM: Int = 200, private val arrivedM: Int = 30) {

    enum class Event { Approaching, Arrived }

    private var approached = false
    private var arrived = false

    fun reset() {
        approached = false
        arrived = false
    }

    /** 이번 거리로 새로 생긴 알림. 없으면 빈 목록 */
    fun update(distanceM: Int): List<Event> = buildList {
        if (!arrived && distanceM <= arrivedM) {
            arrived = true
            approached = true   // 바로 도착 거리로 들어오면 접근 알림은 건너뛴다
            add(Event.Arrived)
        } else if (!approached && distanceM <= approachM) {
            approached = true
            add(Event.Approaching)
        }
    }

    fun isArrived(distanceM: Int) = distanceM <= arrivedM
}
