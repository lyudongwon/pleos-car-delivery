#!/usr/bin/env bash
# S1(주행 중 음성 주문) 스모크: 기어 D로 두고 녹음 대사 3개를 차례로 넣어, 터치 없이 주문 완료까지 가는지 확인한다.
# 에뮬레이터가 켜져 있고 앱이 설치돼 있어야 한다. 실패하면 exit 1.
cd "$(dirname "$0")/.." || exit 1
source scripts/demo.sh
fail=0
# wait_for <설명> <state에 들어 있어야 할 문자열> [최대 초]
wait_for() {
  local s i
  for i in $(seq "${3:-25}"); do
    s="$(demo_state)"
    if echo "$s" | grep -q "$2" && echo "$s" | grep -q '"voiceState":"Idle"'; then echo "  ok  $1"; return 0; fi
  done
  echo "  FAIL $1: '$2' 없음 → $s"; fail=1; return 1
}
"$ADB" shell am force-stop ai.pleos.playground.caas   # 남은 음성 세션 정리
demo_gear Drive >/dev/null
demo_geo 37.4127212 127.094681   # 차 위치를 S1 출발점(내비 고정 위치)으로
demo_start >/dev/null; "$ADB" shell sleep 4
demo_reset
demo_route; "$ADB" shell sleep 15   # 운전자가 서현역을 찍고 출발한 상태
demo_utter s1_1_order
wait_for "가게 두 곳 제안" '"suggestions":\["길목김밥","모퉁이김밥"\]'
demo_utter s1_2_pick
wait_for "참치김밥+어묵국 9,500원 확인" '"status":"Confirming".*"totalPrice":9500'
demo_utter s1_3_yes
wait_for "주문 완료" '"status":"Ordered"'
wait_for "수령 지점 안내" '"pickup":{"name":"길목사거리 정차구역","detourMin":0' 30
wait_for "내비 경유지 추가" '"assistantText":"경로에 넣었어요' 40
demo_geo 37.3995 127.1064   # 수령 지점(길목사거리 정차구역) 약 80m 앞으로 차를 옮긴다
wait_for "수령 지점 도착 감지" '"assistantText":"길목사거리 정차구역에 도착했어요.*"atPickup":true' 20
demo_window 100; "$ADB" shell sleep 2; demo_window 0   # 창문을 열고 음식을 받은 뒤 닫는다
wait_for "창문 열림→닫힘: 받았는지 물음" '"assistantText":"음식 받으셨나요?"' 20
demo_utter s1_4_received
wait_for "수령 완료" '"status":"Received"' 25
s="$(demo_state)"
echo "$s" | grep -q '"touchCount":0' && echo "  ok  화면 터치 0회" || { echo "  FAIL 터치 횟수 → $s"; fail=1; }
echo "$s" | grep -q '"isDriving":true' && echo "  ok  주행 중" || { echo "  FAIL 주행 상태 → $s"; fail=1; }
demo_geo 37.4127212 127.094681; demo_gear Park >/dev/null
[ $fail -eq 0 ] && echo "S1 스모크 통과" || echo "S1 스모크 실패"
exit $fail
