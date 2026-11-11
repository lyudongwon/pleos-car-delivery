#!/usr/bin/env bash
# S1(주행 중 음성 주문) 스모크: 기어 D, 녹음 대사 4개로 주문 → 수령 지점 → 경유지 → 도착 → 창문 수령 → 수령 완료까지.
# 에뮬레이터가 켜져 있고 앱이 설치돼 있어야 한다. 실패하면 exit 1.
# PASSIVE=1 이면 앱을 건드리지 않고 기다렸다가 확인한다(실제 시연처럼). scripts/lib_smoke.sh 참고.
cd "$(dirname "$0")/.." || exit 1
source scripts/demo.sh
source scripts/lib_smoke.sh

"$ADB" shell am force-stop ai.pleos.playground.caas   # 남은 음성 세션 정리
demo_gear Drive >/dev/null
demo_geo 37.4127212 127.094681   # 차 위치를 S1 출발점(내비 고정 위치)으로
demo_start >/dev/null; "$ADB" shell sleep 4
demo_reset
demo_route; "$ADB" shell sleep 15   # 운전자가 서현역을 찍고 출발한 상태
[ "${NO_KEEPALIVE:-0}" = "1" ] && demo_keepalive false   # 동결 감지 검증용: 포그라운드 서비스를 끄고 돌린다

demo_utter s1_1_order
step "가게 두 곳 제안" '"suggestions":\["길목김밥","모퉁이김밥"\]' 18
demo_utter s1_2_pick
step "참치김밥+어묵국 9,500원 확인" '"status":"Confirming".*"totalPrice":9500' 18
demo_utter s1_3_yes
step "주문 완료·수령 지점 안내" '"status":"Ordered".*"pickup":{"name":"길목사거리 정차구역","detourMin":0' 20
step "내비 경유지 추가" '"assistantText":"경로에 넣었어요' 25
demo_geo 37.3995 127.1064   # 수령 지점(길목사거리 정차구역) 약 80m 앞으로 차를 옮긴다
step "수령 지점 도착 감지" '"assistantText":"길목사거리 정차구역에 도착했어요.*"atPickup":true' 15
demo_window 100; "$ADB" shell sleep 2; demo_window 0   # 창문을 열고 음식을 받은 뒤 닫는다
step "창문 열림→닫힘: 받았는지 물음" '"assistantText":"음식 받으셨나요?"' 12
demo_utter s1_4_received
step "수령 완료" '"status":"Received"' 15
step "화면 터치 0회" '"touchCount":0' 1 0
step "주행 중" '"isDriving":true' 1 0

demo_geo 37.4127212 127.094681; demo_gear Park >/dev/null
smoke_end S1
