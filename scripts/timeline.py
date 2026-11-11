"""리허설 로그(adb logcat -v time)에서 타임라인을 뽑고 음성 겹침을 검사한다.

    python -I scripts/timeline.py <logcat 파일> <출력 md> <시나리오 이름>

뽑는 것
- 운전자 대사(녹음 넣음), 인식 결과, 앱이 한 말(시작·끝), 주요 이벤트(가게 추천, 수령 지점, 경유지, 도착, 창문, 기사)
검사
- 앱 말끼리 겹침: 다음 말이 앞 말 끝 전에 시작
- 듣기·말하기 겹침: 대사를 넣은 시각이 앱이 말하는 중
"""
import re
import sys
from datetime import datetime

log_path, out_path, title = sys.argv[1], sys.argv[2], sys.argv[3]
LINE = re.compile(r"^(\d\d-\d\d \d\d:\d\d:\d\d\.\d\d\d) ([VDIWEF])/([^(]+?)\s*\(\s*\d+\): (.*)$")

events = []   # (t, kind, text)
for raw in open(log_path, encoding="utf-8", errors="replace"):
    m = LINE.match(raw.rstrip("\n"))
    if not m:
        continue
    ts, _lvl, tag, msg = m.groups()
    t = datetime.strptime("2026-" + ts, "%Y-%m-%d %H:%M:%S.%f")
    tag = tag.strip()
    if tag == "Speaker":
        if msg.startswith("say '"):
            events.append((t, "say", msg.split("'")[1]))
        elif msg.startswith("say 끝"):
            events.append((t, "say_end", ""))
    elif tag == "Listener" and msg.startswith("최종"):
        events.append((t, "heard", msg[3:].strip(" '")))
    elif tag == "Demo":
        if msg.startswith("명령 Utter"):
            events.append((t, "utter", re.sub(r".*name=([^)]+)\).*", r"\1", msg)))
        elif msg.startswith("명령 Event"):
            events.append((t, "touch", msg[len("명령 Event("):-1]))
        elif msg.startswith("명령 Rider"):
            events.append((t, "event", "기사 좌표 입력"))
        elif any(k in msg for k in ("가게 추천", "수령 지점 ", "경유지 추가", "경유지 뺌", "내비 안내 시작", "기사 ", "배달 정보", "Gleo 주문", "창문 열림")):
            events.append((t, "event", msg))
    elif tag == "LocationSource" and ((msg.startswith("근접 알림 ") and not msg.startswith(("근접 알림 등록", "근접 알림 오류"))) or "이미 반경" in msg):
        events.append((t, "event", "근접 알림: " + msg))
    elif tag == "WindowWatcher" and msg.startswith("운전석 창문"):
        events.append((t, "event", msg))

events.sort(key=lambda e: e[0])
t0 = events[0][0] if events else None

def rel(t):
    return f"{(t - t0).total_seconds():6.1f}s"

# 말하는 구간
speaking = []
start = None
for t, k, txt in events:
    if k == "say":
        start = (t, txt)
    elif k == "say_end" and start:
        speaking.append((start[0], t, start[1]))
        start = None

problems = []
for a, b in zip(speaking, speaking[1:]):
    if b[0] < a[1]:
        problems.append(f"앱 말 겹침: '{a[2]}' 끝나기 전에 '{b[2]}' 시작")
for t, k, txt in events:
    if k == "utter":
        for s, e, say in speaking:
            if s <= t < e:
                problems.append(f"대사 {txt}를 앱이 말하는 중에 넣음 ('{say}' 중)")

label = {"touch": "👆 화면 조작", "utter": "🎙 운전자", "heard": "📝 인식", "say": "🔊 앱", "event": "· 이벤트"}
rows = []
for t, k, txt in events:
    if k == "say_end":
        continue
    dur = ""
    if k == "say":
        for s, e, say in speaking:
            if s == t:
                dur = f" ({(e - s).total_seconds():.1f}초)"
    rows.append(f"| {rel(t)} | {label[k]} | {txt}{dur} |")

with open(out_path, "w", encoding="utf-8") as f:
    f.write(f"## {title}\n\n")
    if t0:
        f.write(f"시작 {t0:%H:%M:%S}, 총 {(events[-1][0] - t0).total_seconds():.0f}초, 앱 발화 {len(speaking)}회\n\n")
    f.write("| 시각 | 누구 | 내용 |\n|---|---|---|\n")
    f.write("\n".join(rows) + "\n\n")
    f.write("**검사**: " + ("문제 없음 (앱 말 겹침 0, 듣기·말하기 겹침 0)" if not problems else "") + "\n")
    for p in problems:
        f.write(f"- ⚠️ {p}\n")
print(f"{title}: 이벤트 {len(rows)}개, 문제 {len(problems)}개")
