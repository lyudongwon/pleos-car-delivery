#!/usr/bin/env bash
# 시나리오 리허설: 영상 콘티 순서대로 S1 → S2를 한 사이클 돌리며 화면 녹화·핵심 장면 캡처·타임라인을 남긴다.
# 스모크와 다른 점
#   - 상태를 묻지 않는다(앱을 건드리지 않음). 앱이 말을 마쳤는지는 logcat의 Speaker 로그로만 본다
#   - 운전자 대사는 앱 말이 끝나고 사람이 반응하는 간격(PAUSE초) 뒤에 넣는다
#   - 끝나고 확인은 한 번만(state 1회), 타임라인은 logcat에서 뽑는다(scripts/timeline.py)
#   bash scripts/rehearsal.sh
# 결과: docs/poc/rehearsal/<시각>/ 아래 s1.mp4·s2.mp4·*.png·*.log, 타임라인 md (녹화·캡처는 git에서 뺌)
cd "$(dirname "$0")/.." || exit 1
export MSYS_NO_PATHCONV=1   # Git Bash가 /sdcard 경로를 Windows 경로로 바꾸지 않게
source scripts/demo.sh
source scripts/lib_smoke.sh   # route_started
PAUSE="${PAUSE:-1.5}"
stamp="$(date +%m%d_%H%M)"
out="docs/poc/rehearsal/$stamp"; mkdir -p "$out"
REC_PID=""

shot() { "$ADB" exec-out screencap -p > "$out/$1.png"; echo "  📷 $1"; }

rec_start() {   # 기기에서 녹화(최대 180초) 시작
  "$ADB" shell rm -f "/sdcard/reh_$1.mp4"
  "$ADB" shell screenrecord --size 1280x720 --time-limit 180 "/sdcard/reh_$1.mp4" & REC_PID=$!
  "$ADB" shell sleep 1
}
rec_stop() {
  "$ADB" shell pkill -INT screenrecord >/dev/null 2>&1
  wait "$REC_PID" 2>/dev/null
  "$ADB" shell sleep 2
  "$ADB" pull "/sdcard/reh_$1.mp4" "$out/$1.mp4" >/dev/null && echo "  🎬 $1.mp4"
}

say_count() { "$ADB" logcat -d -s Speaker:I | grep -c "say '"; }

# 앱이 새로 말을 시작해서(직전 개수보다 많아짐) 다 끝나고 2초 조용할 때까지 기다린다. 최대 <초>
wait_reply() {
  local before=$1 secs=$2 end=$(( SECONDS + $2 )) last
  while [ "$SECONDS" -lt "$end" ]; do
    "$ADB" shell sleep 1
    last="$("$ADB" logcat -d -s Speaker:I | grep -E "say '|say 끝" | tail -1)"
    if [ "$(say_count)" -gt "$before" ] && echo "$last" | grep -q "say 끝"; then
      "$ADB" shell sleep 2
      [ "$("$ADB" logcat -d -s Speaker:I | grep -E "say '|say 끝" | tail -1)" = "$last" ] && return 0
    fi
  done
  echo "  !! ${secs}초 안에 앱 말이 끝나지 않음"; return 1
}

# 앱이 <문구>를 말하고 끝낼 때까지 기다린다. 최대 <초>
wait_phrase() {
  local phrase=$1 end=$(( SECONDS + $2 ))
  while [ "$SECONDS" -lt "$end" ]; do
    "$ADB" shell sleep 1
    "$ADB" logcat -d -s Speaker:I | grep -E "say '|say 끝" | grep -A1 "$phrase" | grep -q "say 끝" && return 0
  done
  echo "  !! '$phrase' 안내를 못 들음"; return 1
}

# 운전자가 대사를 말한다: 반응 간격 뒤 녹음을 넣고, 앱 대답이 끝날 때까지 기다린다
driver() {
  local name=$1 secs=${2:-25} n
  n=$(say_count)
  "$ADB" shell sleep "$PAUSE"
  echo "  🎙 $name"
  demo_utter "$name"
  wait_reply "$n" "$secs"
}

dump_log() {   # <이름>: 이번 시나리오 로그 저장 + 타임라인
  "$ADB" logcat -d -v time -s Demo:I Speaker:I Listener:I LocationSource:I WindowWatcher:I NaviClient:I KeepAlive:I > "$out/$1.log"
  python -I scripts/timeline.py "$out/$1.log" "$out/$1_timeline.md" "$2"
}

"$ADB" logcat -G 16M
"$ADB" shell am force-stop ai.pleos.playground.caas

echo "== S1 주행 중 음성 주문"
demo_gear Drive >/dev/null
demo_geo 37.4127212 127.094681
demo_start >/dev/null; "$ADB" shell sleep 4
demo_reset
"$ADB" logcat -c; demo_route
route_started || echo "  !! 내비 안내 시작 못 함"
"$ADB" shell am start -n "$PKG/.MainActivity" >/dev/null; "$ADB" shell sleep 2   # 안내가 잡힌 뒤 밥먹GO를 다시 앞으로 (상태는 그대로)
"$ADB" logcat -c
rec_start s1
shot s1_0_driving
driver s1_1_order;    shot s1_1_suggest
driver s1_2_pick;     shot s1_2_confirm
driver s1_3_yes 40;   shot s1_3_ordered
wait_phrase "경로에 추가했습니다" 30; shot s1_4_waypoint_navi
"$ADB" shell sleep 5   # 수령 지점까지 가는 구간(영상에서는 편집으로 줄임)
n=$(say_count); demo_geo 37.295221 127.106226   # 수령 지점 80m 앞
wait_reply "$n" 20;   shot s1_5_arrived
"$ADB" shell sleep 2
driver s1_4_received 30; shot s1_7_received
"$ADB" shell sleep 3; shot s1_8_route_back
rec_stop s1
s1_state="$(demo_state)"
dump_log s1 "S1 주행 중 음성 주문"
demo_geo 37.4127212 127.094681; demo_gear Park >/dev/null

echo "== S2 정차 중 주문"
demo_geo 37.3838 127.1240
demo_start >/dev/null; "$ADB" shell sleep 2
demo_reset
"$ADB" logcat -c
rec_start s2
shot s2_0_menu
"$ADB" shell sleep 2; demo_event AddMenu s2-2
"$ADB" shell sleep 2; demo_event AddMenu s2-4
"$ADB" shell sleep 2; shot s2_1_cart
demo_event ReviewOrder; "$ADB" shell sleep 3; shot s2_2_confirm
demo_event PlaceOrder;  "$ADB" shell sleep 3; shot s2_3_delivery
demo_rider 37.3900 127.1300; "$ADB" shell sleep 4; shot s2_4_rider_far
n=$(say_count); demo_rider 37.3850 127.1255; wait_reply "$n" 15; shot s2_5_rider_near
n=$(say_count); demo_rider 37.3840 127.1242; wait_reply "$n" 15; shot s2_6_rider_arrived
"$ADB" shell sleep 2; demo_event ConfirmReceived; "$ADB" shell sleep 3; shot s2_7_received
rec_stop s2
s2_state="$(demo_state)"
dump_log s2 "S2 정차 중 주문"

{
  echo "# 리허설 $stamp"; echo
  echo "녹화·캡처: \`$out/\` (git 밖)"; echo
  cat "$out/s1_timeline.md"; echo
  echo "끝 state: \`$(echo "$s1_state" | grep -o '"status":"[A-Za-z]*"\|"touchCount":[0-9]*\|"isDriving":[a-z]*' | tr '\n' ' ')\`"; echo
  cat "$out/s2_timeline.md"; echo
  echo "끝 state: \`$(echo "$s2_state" | grep -o '"status":"[A-Za-z]*"\|"isDriving":[a-z]*' | tr '\n' ' ')\`"
} > "$out/timeline.md"
echo "결과: $out/timeline.md"
