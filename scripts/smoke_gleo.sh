#!/usr/bin/env bash
# F15 Gleo 연동 스모크: Gleo가 보내는 것과 같은 ORDER_FOOD 인텐트를 넣고, 앱 대화로 주문 완료까지 가는지 확인한다.
# (Gleo 음성 → 인텐트 부분은 docs/gleo_check.md에서 실제 Gleo로 확인함. 여기서는 인텐트 → 앱 흐름을 본다)
cd "$(dirname "$0")/.." || exit 1
source scripts/demo.sh
fail=0
wait_for() {
  local s i
  for i in $(seq "${3:-25}"); do
    s="$(demo_state)"
    if echo "$s" | grep -q "$2" && echo "$s" | grep -q '"voiceState":"Idle"'; then echo "  ok  $1"; return 0; fi
  done
  echo "  FAIL $1: '$2' 없음 → $s"; fail=1; return 1
}
"$ADB" shell am force-stop ai.pleos.playground.caas
demo_gear Drive >/dev/null
demo_geo 37.4127212 127.094681
demo_start >/dev/null; "$ADB" shell sleep 4
demo_reset
demo_route; "$ADB" shell sleep 15
"$ADB" shell "am start -a ai.cardeli.starter.ORDER_FOOD -p ai.cardeli.starter --es ACTION_PARAM '{\"menu\":\"참치 김밥\",\"quantity\":1}'" >/dev/null
wait_for "Gleo 메뉴로 바로 주문 확인" '"status":"Confirming".*"totalPrice":5500.*"assistantText":"길목김밥에서 참치김밥 하나, 5,500원이에요. 주문할까요?"'
demo_utter s1_3_yes
wait_for "주문 완료" '"status":"Ordered"'
wait_for "수령 지점 안내" '"pickup":{"name":"길목사거리 정차구역"' 30
demo_gear Park >/dev/null
[ $fail -eq 0 ] && echo "Gleo 스모크 통과" || echo "Gleo 스모크 실패"
exit $fail
