#!/usr/bin/env bash
# 시연·테스트용 adb 명령 모음 (F14). 사용법: docs/demo.md
#   source scripts/demo.sh   후   demo_reset, demo_event AddMenu s2-2, demo_rider 37.385 127.1256, demo_state, demo_gear Drive
ADB="${ADB:-adb}"
PKG=ai.cardeli.starter
RECV="$PKG/.feature.demo.DemoReceiver"

demo_cmd()   { "$ADB" shell am broadcast -n "$RECV" "$@" >/dev/null; }
demo_reset() { demo_cmd --es cmd reset; }
demo_measure() { demo_cmd --es cmd measure; }   # 앱이 내비로 수령 후보 추가 시간을 직접 잰다(1~2분)
demo_measure_log() { "$ADB" shell run-as "$PKG" --user "$DEMO_USER" cat files/pickup_measure_live.json; echo; }
demo_keepalive() { demo_cmd --es cmd keepalive --es on "$1"; }   # true/false. 동결 감지 검증용
demo_route() { demo_cmd --es cmd route; }   # S1 목적지(서현역)로 내비 안내 시작
demo_event() { if [ -n "$2" ]; then demo_cmd --es cmd event --es name "$1" --es id "$2"; else demo_cmd --es cmd event --es name "$1"; fi; }
demo_say()   { demo_cmd --es cmd say --es text "'$1'"; }   # 공백 있는 문장은 adb shell에서 한 덩어리로 넘어가게 작은따옴표로 감싼다
demo_utter() { demo_cmd --es cmd utter --es name "$1"; }
demo_rider() { demo_cmd --es cmd rider --es lat "$1" --es lng "$2"; }
# state를 저장하고 바로 출력
# AAOS는 앱이 사용자 10으로 돈다
DEMO_USER="${DEMO_USER:-$("$ADB" shell am get-current-user | tr -dc '0-9')}"
demo_state() { demo_cmd --es cmd dump; "$ADB" shell sleep 1; "$ADB" shell run-as "$PKG" --user "$DEMO_USER" cat files/demo_state.json; echo; }
# F7 수령 지점 계산 로그 출력
demo_pickup_log() { "$ADB" shell run-as "$PKG" --user "$DEMO_USER" cat files/pickup_log.json; echo; }
# 차량 위치 (FusedLocation): 인자는 위도 경도, adb에는 경도 위도 순서로 넘긴다
demo_geo()   { "$ADB" emu geo fix "$2" "$1" >/dev/null; }
# Virtual Console: 기어 바꾸기 (heartbeat 후 요청)
demo_gear()  {
  "$ADB" forward tcp:8088 tcp:8088 >/dev/null
  curl -s -X POST "http://localhost:8088/browser/session?client=demo" >/dev/null
  curl -s -X PUT "http://localhost:8088/vehicle/gear?gear=$1" | grep -o '"value":"[A-Za-z]*"'
}
# Virtual Console: 운전석 창문 0(닫힘)~100(열림)
demo_window() {
  "$ADB" forward tcp:8088 tcp:8088 >/dev/null
  curl -s -X POST "http://localhost:8088/browser/session?client=demo" >/dev/null
  curl -s -X PUT "http://localhost:8088/vehicle/body?window_fl=$1" >/dev/null
}
demo_start() { "$ADB" shell am force-stop "$PKG"; "$ADB" shell am start -W -n "$PKG/.MainActivity" | grep -E "Status"; }
