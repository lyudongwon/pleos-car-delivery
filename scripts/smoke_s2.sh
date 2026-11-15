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
step "정차 중·기어 P" '"isDriving":false,"isParked":true' 3 0
step "가게 목록: 차 위치로 거리·예상 시간 계산" '"stores":\[{"id":"s2-a","eta":11' 2 0
demo_event SelectStore s2-a
demo_event AddMenu s2-2; demo_event AddMenu s2-4
step "참치김밥+어묵국 9,500원" '"totalPrice":9500' 2 0
step "결제 금액(배달비 2,300원 포함)" '"deliveryFee":2300,"payTotal":11800' 1 0
demo_event ReviewOrder
step "주문서" '"status":"Confirming"' 2 0
demo_event SelectWindow Passenger; demo_event SelectLocateBy Hazard; demo_event SelectPayment HyundaiPay
step "수령 창문·위치 알림·결제 방식 선택" '"options":{"method":"ToCar","window":"Passenger","locateBy":"Hazard","payment":"HyundaiPay"}' 2 0
demo_event PlaceOrder
step "주문 접수: 예상 11분" '"etaMin":11,"status":"Ordered"' 3 0
step "배달 정보: 지금 차 좌표" '"delivery":{"place":"지금 차 위치","lat":37.383' 1 0
step "배달 정보: 번호판·차종·창문·비상등·기어 P 확정" '"plate":"12가 3456","color":"흰색","carModel":"Hyundai 42dot Connect","window":"Passenger","locateBy":"Hazard","fixedByPark":true' 1 0
demo_progress Cooking
step "조리 중" '"status":"Cooking"' 2 0
demo_rider 37.3900 127.1300
step "배달 시작 (기사 약 870m)" '"status":"Delivering"' 3 0
demo_rider 37.3850 127.1255
step "200m 안: 곧 도착 + 비상등 안내" '"status":"RiderComing".*"assistantText":"배달기사가 곧 도착해요. 비상등을 켜 주세요."' 8
demo_rider 37.3840 127.1242
step "기사 도착 (30m 안): 받았는지 물음" '"status":"Arrived".*"assistantText":"배달기사가 도착했어요. 음식을 받으셨나요?"' 8
demo_event ConfirmReceived
step "수령 완료" '"etaMin":0,"status":"Received"' 4 0
demo_reset
step "리셋" '"totalPrice":0' 2 0
smoke_end S2
