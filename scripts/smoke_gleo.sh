#!/usr/bin/env bash
# F15 Gleo 연동 스모크: Gleo가 보내는 것과 같은 ORDER_FOOD 인텐트를 넣고, 앱 대화로 주문 완료까지 가는지 확인한다.
# (Gleo 음성 → 인텐트 부분은 docs/gleo_check.md에서 실제 Gleo로 확인함. 여기서는 인텐트 → 앱 흐름을 본다)
# PASSIVE=1 이면 앱을 건드리지 않고 기다렸다가 확인한다(실제 시연처럼). scripts/lib_smoke.sh 참고.
cd "$(dirname "$0")/.." || exit 1
source scripts/demo.sh
source scripts/lib_smoke.sh

"$ADB" shell am force-stop ai.pleos.playground.caas
demo_gear Drive >/dev/null
demo_geo 37.4127212 127.094681
demo_start >/dev/null; "$ADB" shell sleep 4
demo_reset
"$ADB" logcat -c; demo_route
route_started || { echo "  FAIL 내비 안내 시작"; SMOKE_FAIL=1; }
"$ADB" shell "am start -a ai.cardeli.starter.ORDER_FOOD -p ai.cardeli.starter --es ACTION_PARAM '{\"menu\":\"참치 김밥\",\"quantity\":1}'" >/dev/null
step "Gleo 메뉴로 바로 주문 확인" '"status":"Confirming".*"totalPrice":5500.*"assistantText":"길목김밥에서 참치김밥 하나, 5,500원입니다. 주문할까요?"' 12
demo_utter s1_3_yes
step "주문 완료·수령 지점 안내" '"status":"Ordered".*"pickup":{"name":"길목사거리 정차구역"' 20
demo_gear Park >/dev/null
smoke_end Gleo
