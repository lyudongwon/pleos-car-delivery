#!/usr/bin/env bash
# 콜드 부팅 전체 실행: 에뮬레이터 재부팅 → 빌드·설치 → 준비 → S1·S2·Gleo 무폴링 스모크 → 결과 요약.
# 녹화 전이나 큰 변경 뒤 한 번 돌린다. 실패가 하나라도 있으면 exit 1.
#   bash scripts/run_all.sh            # 재부팅부터
#   NO_REBOOT=1 bash scripts/run_all.sh   # 재부팅 없이
# 결과: docs/poc/e2e/run_<시각>.md (스모크 출력 전체 포함)
cd "$(dirname "$0")/.." || exit 1
export ADB="${ADB:-adb}"
stamp="$(date +%m%d_%H%M)"
out_dir="docs/poc/e2e"; mkdir -p "$out_dir"
report="$out_dir/run_$stamp.md"
log="$out_dir/run_$stamp.log"
t0=$SECONDS
note() { echo "$*"; echo "$*" >> "$log"; }
: > "$log"

note "== 1. 에뮬레이터 준비 ($(date '+%F %T'))"
if [ "${NO_REBOOT:-0}" != "1" ]; then
  "$ADB" reboot
  timeout 300 "$ADB" wait-for-device
  timeout 300 "$ADB" shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
  "$ADB" shell sleep 20   # 부팅 직후 차량·내비 서비스가 뜰 시간
  note "재부팅 완료 ($(( SECONDS - t0 ))초)"
fi
"$ADB" logcat -G 16M
"$ADB" shell am force-stop ai.pleos.playground.caas

note "== 2. 빌드·설치"
if ! ./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain -q >> "$log" 2>&1; then
  note "빌드 또는 단위 테스트 실패"; exit 1
fi
tests=$(grep -h -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml | awk -F'"' '{t+=$2; f+=$6} END {print t" 중 실패 "f}')
note "단위 테스트 $tests"
"$ADB" install -r -g app/build/outputs/apk/debug/app-debug.apk >> "$log" 2>&1 || { note "설치 실패"; exit 1; }

declare -A result dur
for t in s1 s2 gleo; do
  note "== 스모크 $t (무폴링)"
  st=$SECONDS
  PASSIVE=1 bash "scripts/smoke_$t.sh" 2>&1 | grep -v "^Warning: Activity not started" | tee -a "$log"
  rc=${PIPESTATUS[0]}
  dur[$t]=$(( SECONDS - st ))
  result[$t]=$([ "$rc" = "0" ] && echo "통과" || echo "**실패**")
done

fail=0
for t in s1 s2 gleo; do [ "${result[$t]}" = "통과" ] || fail=1; done
total=$(( SECONDS - t0 ))
{
  echo "# 전체 실행 $(date '+%F %T')"
  echo
  echo "- 재부팅: $([ "${NO_REBOOT:-0}" = "1" ] && echo "안 함" || echo "함")"
  echo "- 단위 테스트: $tests"
  echo "- 커밋: $(git rev-parse --short HEAD)$(git diff --quiet || echo " (커밋 안 된 변경 있음)")"
  echo "- 총 소요: ${total}초"
  echo
  echo "| 스모크 (무폴링·동결 감시) | 결과 | 소요 |"
  echo "|---|---|---|"
  echo "| S1 주행 중 음성 주문 → 수령 완료 | ${result[s1]} | ${dur[s1]}초 |"
  echo "| S2 정차 중 주문 → 기사 도착 → 수령 | ${result[s2]} | ${dur[s2]}초 |"
  echo "| Gleo 주문 → 수령 지점 안내 | ${result[gleo]} | ${dur[gleo]}초 |"
  echo
  echo "## 출력"
  echo
  echo '```'
  cat "$log"
  echo '```'
} > "$report"
rm -f "$log"
echo
echo "결과: $report"
[ $fail -eq 0 ] && echo "전체 실행 통과" || echo "전체 실행 실패"
exit $fail
