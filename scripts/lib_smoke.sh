#!/usr/bin/env bash
# 스모크 공통 함수. scripts/demo.sh 다음에 source 한다.
#
# 두 가지 모드:
#   기본       상태를 계속 물으며(demo_state) 조건이 맞을 때까지 기다린다. 빠르지만, 묻는 명령이 앱을 깨워서
#              앱이 백그라운드에서 얼어도(Android 14 동결) 통과할 수 있다.
#   PASSIVE=1  정해진 시간 동안 앱을 건드리지 않고 기다린 뒤 한 번만 확인한다. 실제 시연처럼 앱을 가만히 둔다.
#              기다리는 동안 10초마다 앱이 얼었는지(isFrozen) 보고, 한 번이라도 얼면 스모크 실패.
#              isFrozen 확인은 dumpsys라 앱을 깨우지 않는다.

SMOKE_FAIL=0
FROZEN_SEEN=0
PASSIVE="${PASSIVE:-0}"

# 우리 앱 프로세스가 얼어 있으면 true
demo_frozen() {
  "$ADB" shell dumpsys activity processes | grep -A12 "$PKG/u" | grep -o "isFrozen=[a-z]*" | head -1 | cut -d= -f2
}

# 앱을 건드리지 않고 <초>만큼 기다리며 10초마다 동결 확인
passive_wait() {
  local left=$1 d
  while [ "$left" -gt 0 ]; do
    d=$(( left < 10 ? left : 10 ))
    "$ADB" shell sleep "$d"
    left=$(( left - d ))
    if [ "$(demo_frozen)" = "true" ]; then
      FROZEN_SEEN=1
      echo "  !!  앱이 얼어 있음 (isFrozen=true)"
    fi
  done
}

# step <설명> <state에 들어 있어야 할 정규식> <초> [idle 필요: 1(기본)·0]
#   idle 필요 1이면 앱이 말하는 중이 아닐 때(voiceState Idle)만 통과로 본다
step() {
  local desc=$1 pattern=$2 secs=$3 need_idle=${4:-1} s
  if [ "$PASSIVE" = "1" ]; then
    # 무폴링에서는 "그 단계를 지났는지"만 본다. 확인 순간에 앱이 다음 안내를 말하고 있을 수 있어 Idle은 보지 않는다
    passive_wait "$secs"
    s="$(demo_state)"
    if echo "$s" | grep -q "$pattern"; then
      echo "  ok  $desc"; return 0
    fi
  else
    local end=$(( SECONDS + secs ))
    while :; do
      s="$(demo_state)"
      if echo "$s" | grep -q "$pattern" && { [ "$need_idle" = "0" ] || echo "$s" | grep -q '"voiceState":"Idle"'; }; then
        echo "  ok  $desc"; return 0
      fi
      [ "$SECONDS" -ge "$end" ] && break
    done
  fi
  echo "  FAIL $desc: '$pattern' 없음 → $s"
  SMOKE_FAIL=1
  return 1
}

# smoke_end <이름>: 결과를 출력하고 종료 코드를 정한다
smoke_end() {
  local name=$1 mode="폴링"
  [ "$PASSIVE" = "1" ] && mode="무폴링"
  if [ "$FROZEN_SEEN" = "1" ]; then
    echo "  FAIL 기다리는 동안 앱이 얼었음"
    SMOKE_FAIL=1
  fi
  if [ "$SMOKE_FAIL" = "0" ]; then echo "$name 스모크 통과 ($mode)"; else echo "$name 스모크 실패 ($mode)"; fi
  exit "$SMOKE_FAIL"
}
