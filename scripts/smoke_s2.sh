#!/usr/bin/env bash
# S2(정차 중 주문) 스모크: 데모 명령만으로 처음부터 끝까지 돌리고 단계마다 state를 확인한다.
# 에뮬레이터가 켜져 있고 앱이 설치돼 있어야 한다. 실패하면 exit 1.
# PASSIVE=1 이면 앱을 건드리지 않고 기다렸다가 확인한다(실제 시연처럼). scripts/lib_smoke.sh 참고.
cd "$(dirname "$0")/.." || exit 1
source scripts/demo.sh
source scripts/lib_smoke.sh

demo_gear Park >/dev/null
demo_geo 37.3838 127.1240   # 차를 서현 학원가 주차장에
demo_start >/dev/null; "$ADB" shell sleep 2
demo_reset
step "시작은 메뉴판" '"status":"Browsing"' 2 0
step "정차 중" '"isDriving":false' 1 0
demo_event AddMenu s2-2; demo_event AddMenu s2-4
step "참치김밥+어묵국 9,500원" '"totalPrice":9500' 2 0
demo_event ReviewOrder
step "주문 확인" '"status":"Confirming"' 2 0
demo_event PlaceOrder
step "배달 정보: 지금 차 좌표" '"delivery":{"place":"지금 차 위치","lat":37.383' 3 0
step "배달 정보: 등록 번호판·차에서 읽은 차종" '"plate":"12가 3456","color":"흰색","carModel":"Hyundai 42dot Connect"' 1 0
demo_rider 37.3900 127.1300
step "기사 이동 중 (약 870m)" '"status":"RiderComing"' 3 0
demo_rider 37.3850 127.1255
step "200m 안: 곧 도착 안내" '"assistantText":"배달기사가 곧 도착해요."' 6
demo_rider 37.3840 127.1242
step "기사 도착 (30m 안) 안내" '"status":"Arrived".*"assistantText":"배달기사가 도착했어요."' 6
demo_event ConfirmReceived
step "수령 완료" '"status":"Received"' 2 0
demo_reset
step "리셋" '"totalPrice":0' 2 0
smoke_end S2
