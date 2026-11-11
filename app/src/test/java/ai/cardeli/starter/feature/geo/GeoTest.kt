package ai.cardeli.starter.feature.geo

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoTest {

    @Test
    fun `위도 0_01도는 약 1113m다`() {
        assertEquals(1113.2, Geo.distanceM(LatLng(37.0, 127.0), LatLng(37.01, 127.0)), 1.0)
    }

    @Test
    fun `경로 옆 점은 수직 거리와 경로상 위치가 나온다`() {
        // 동쪽으로 곧게 뻗은 경로 위 중간 지점에서 북쪽으로 100m 떨어진 점
        val route = listOf(LatLng(37.0, 127.0), LatLng(37.0, 127.01), LatLng(37.0, 127.02))
        val total = Geo.cumulativeM(route).last()
        val p = Geo.project(LatLng(37.0 + 100 / 111_320.0, 127.01), route)
        assertEquals(100.0, p.offsetM, 1.0)
        assertEquals(total / 2, p.alongM, 1.0)
    }

    @Test
    fun `경로 시작점 뒤쪽 점은 경로상 위치가 0이다`() {
        val route = listOf(LatLng(37.0, 127.0), LatLng(37.0, 127.01))
        val p = Geo.project(LatLng(37.0, 126.99), route)
        assertEquals(0.0, p.alongM, 0.001)
    }
}
