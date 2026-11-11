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
demo_start >/dev/null; "$ADB" shell sleep 4
demo_reset
demo_utter s1_1_order
wait_for "가게 두 곳 제안" '"suggestions":\["길목김밥","모퉁이김밥"\]'
demo_utter s1_2_pick
wait_for "참치김밥+어묵국 9,500원 확인" '"status":"Confirming".*"totalPrice":9500'
demo_utter s1_3_yes
wait_for "주문 완료" '"status":"Ordered"'
s="$(demo_state)"
echo "$s" | grep -q '"touchCount":0' && echo "  ok  화면 터치 0회" || { echo "  FAIL 터치 횟수 → $s"; fail=1; }
echo "$s" | grep -q '"isDriving":true' && echo "  ok  주행 중" || { echo "  FAIL 주행 상태 → $s"; fail=1; }
demo_gear Park >/dev/null
[ $fail -eq 0 ] && echo "S1 스모크 통과" || echo "S1 스모크 실패"
exit $fail
