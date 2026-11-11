#!/usr/bin/env bash
# S2(정차 중 주문) 스모크: 데모 명령만으로 처음부터 끝까지 돌리고 단계마다 state를 확인한다.
# 에뮬레이터가 켜져 있고 앱이 설치돼 있어야 한다. 실패하면 exit 1.
cd "$(dirname "$0")/.." || exit 1
source scripts/demo.sh
fail=0
check() { # check <설명> <state에 들어 있어야 할 문자열>
  local s; s="$(demo_state)"
  if echo "$s" | grep -q "$2"; then echo "  ok  $1"; else echo "  FAIL $1: '$2' 없음 → $s"; fail=1; fi
}
demo_gear Park >/dev/null
demo_geo 37.3838 127.1240   # 차를 서현 학원가 주차장에
demo_start >/dev/null; "$ADB" shell sleep 2
demo_reset
check "시작은 메뉴판" '"status":"Browsing"'
check "정차 중" '"isDriving":false'
demo_event AddMenu s2-2; demo_event AddMenu s2-4
check "참치김밥+어묵국 9,500원" '"totalPrice":9500'
demo_event ReviewOrder
check "주문 확인" '"status":"Confirming"'
demo_event PlaceOrder; "$ADB" shell sleep 1
check "배달 정보: 지금 차 좌표" '"delivery":{"place":"지금 차 위치","lat":37.383'
check "배달 정보: 등록 번호판·차에서 읽은 차종" '"plate":"12가 3456","color":"흰색","carModel":"Hyundai 42dot Connect"'
demo_rider 37.3900 127.1300
check "기사 이동 중 (약 870m)" '"status":"RiderComing"'
demo_rider 37.3850 127.1255; "$ADB" shell sleep 1
check "200m 안: 곧 도착 안내" '"assistantText":"배달기사가 곧 도착해요."'
demo_rider 37.3840 127.1242; "$ADB" shell sleep 1
check "기사 도착 (30m 안) 안내" '"status":"Arrived".*"assistantText":"배달기사가 도착했어요."'
demo_event ConfirmReceived
check "수령 완료" '"status":"Received"'
demo_reset
check "리셋" '"totalPrice":0'
[ $fail -eq 0 ] && echo "S2 스모크 통과" || echo "S2 스모크 실패"
exit $fail
