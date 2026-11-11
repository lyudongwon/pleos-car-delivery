package ai.cardeli.starter.feature.geo

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double)

/** 경로 위로 내린 점: 경로에서 떨어진 거리와, 경로 시작점부터 그 점까지 경로를 따라간 거리 */
data class RouteProjection(val offsetM: Double, val alongM: Double)

/**
 * 시연 범위(수 km)용 평면 근사 계산. 위도 1도 ≈ 111,320m, 경도는 cos(위도)를 곱한다.
 * 수 km 안에서 오차는 1% 안쪽이라 가게 거르기·후보 고르기에 충분하다.
 */
object Geo {

    private const val M_PER_DEG = 111_320.0

    fun distanceM(a: LatLng, b: LatLng): Double {
        val kx = M_PER_DEG * cos(Math.toRadians((a.lat + b.lat) / 2))
        return hypot((b.lng - a.lng) * kx, (b.lat - a.lat) * M_PER_DEG)
    }

    /** 경로 좌표를 따라간 누적 거리. [i]는 시작점부터 points[i]까지의 거리 */
    fun cumulativeM(points: List<LatLng>): DoubleArray {
        val cum = DoubleArray(points.size)
        for (i in 1 until points.size) cum[i] = cum[i - 1] + distanceM(points[i - 1], points[i])
        return cum
    }

    /** 점 p를 경로에 수직으로 내려, 가장 가까운 지점의 거리와 경로상 위치를 구한다 */
    fun project(p: LatLng, points: List<LatLng>, cum: DoubleArray = cumulativeM(points)): RouteProjection {
        require(points.size >= 2) { "경로 좌표는 2개 이상이어야 한다" }
        var best = RouteProjection(Double.MAX_VALUE, 0.0)
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val kx = M_PER_DEG * cos(Math.toRadians(a.lat))
            val bx = (b.lng - a.lng) * kx
            val by = (b.lat - a.lat) * M_PER_DEG
            val px = (p.lng - a.lng) * kx
            val py = (p.lat - a.lat) * M_PER_DEG
            val len2 = bx * bx + by * by
            val t = if (len2 == 0.0) 0.0 else ((px * bx + py * by) / len2).coerceIn(0.0, 1.0)
            val offset = hypot(px - bx * t, py - by * t)
            if (offset < best.offsetM) best = RouteProjection(offset, cum[i] + sqrt(len2) * t)
        }
        return best
    }
}
