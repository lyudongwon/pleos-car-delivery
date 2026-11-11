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
demo_start >/dev/null; "$ADB" shell sleep 2
demo_reset
check "시작은 메뉴판" '"status":"Browsing"'
check "정차 중" '"isDriving":false'
demo_event AddMenu s2-2; demo_event AddMenu s2-4
check "참치김밥+어묵국 9,500원" '"totalPrice":9500'
demo_event ReviewOrder
check "주문 확인" '"status":"Confirming"'
demo_event PlaceOrder; "$ADB" shell sleep 1
demo_rider 37.3900 127.1300
check "기사 이동 중" '"status":"RiderComing"'
demo_rider 37.3840 127.1242
check "기사 도착 (30m 안)" '"status":"Arrived"'
demo_event ConfirmReceived
check "수령 완료" '"status":"Received"'
demo_reset
check "리셋" '"totalPrice":0'
[ $fail -eq 0 ] && echo "S2 스모크 통과" || echo "S2 스모크 실패"
exit $fail
