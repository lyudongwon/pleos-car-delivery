# 카딜 (가칭)

**차량 위치와 이동 경로를 활용해 차 안에서 음식을 주문하고 배달받는 차량 특화 음식 배달 서비스.**
Pleos Connect(AAOS) 앱 · Kotlin · Jetpack Compose

<!-- TODO: 대표 이미지 또는 시연 GIF (디자인 적용 후). 예: 주행 중 주문 → 수령 지점이 내비 경유지로 들어간 장면 -->
> 🖼️ *대표 이미지 자리 (디자인 적용 후 추가)*

## 차에서 실제로 돌아가는 것

| 기능 | Pleos / AAOS | 상태 |
|---|---|---|
| 정차 중엔 화면, 주행 중엔 음성으로 주문 방식 자동 전환 | CarUxRestrictions | ✅ |
| 음성으로 주문하고 음성으로 안내 | Gleo AI SpeechToText · TextToSpeech | ✅ |
| 경로 근처 가게 추천 → 수령 지점 선택 → 내비 경유지 등록 → 수령 후 원래 경로로 | NaviHelper · FusedLocation | ✅ |
| 운전석 창문이 열렸다 닫히면 수령 감지 | Vehicle | ✅ |
| 주소 입력 없이 차 위치·차종으로 배달 요청 | FusedLocation · Car API | ✅ |
| Gleo AI에 "참치김밥 하나 주문해줘"라고 말하면 주문 시작 | Gleo AI 앱 연동 (schema.json) | ✅ |

## 두 가지 상황

- **주행 중**: 말로 주문하면, 앱이 가던 길 위의 수령 지점을 골라 내비에 넣어 줍니다. 그곳에 차를 세우고 창문으로 받습니다.
- **정차 중**: 차 화면으로 주문하면, 주소를 넣지 않아도 지금 차가 서 있는 자리로 배달됩니다.

## PoC: 확인한 것

| 확인하려던 것 | 결과 |
|---|---|
| 경로 위에서 받으면 정말 길을 안 벗어나나 | 수령 지점을 경유지로 넣기 전후 내비 남은 거리·시간이 같음(17km·23분). 가게에 들러 포장하면 +1km ([실측](docs/poc/F7_pickup_measure_1112.json)) |
| 차가 보내는 신호만으로 수령을 알 수 있나 | 수령 지점 도착(근접 알림) 뒤 운전석 창문 열림 → 닫힘을 받아 "받으셨나요?"를 묻고 음성으로 확정 |
| 차를 세울 수 없는 구간은 어디인가 | 고속도로를 타는 경로에는 수령 후보가 나오지 않음 → 일반 도로 구간만 후보로 둠 |
| 내비가 앞에 떠 있어도 앱이 계속 동작하나 | Android 14에서 백그라운드 앱이 멈추는 문제를 찾아 포그라운드 서비스로 해결, 무인 전체 실행으로 확인 |

전체 확인 기록: [docs/poc](docs/poc/README.md)

<details>
<summary>실행 방법</summary>

Pleos Connect 에뮬레이터(AAOS API 34)에서 실행합니다.

```bash
./gradlew :app:assembleDebug
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ai.cardeli.starter/.MainActivity
```

시나리오를 처음부터 끝까지 돌려 보려면:

```bash
bash scripts/smoke_s1.sh    # 주행 중 음성 주문 → 수령
bash scripts/smoke_s2.sh    # 정차 중 주문 → 배달
bash scripts/smoke_gleo.sh  # Gleo AI 주문
```

명령 목록은 [docs/demo.md](docs/demo.md)에 있습니다.
</details>

<details>
<summary>구조</summary>

```
app/src/main/java/ai/cardeli/starter/
├─ contract/   화면과 기능이 주고받는 상태·이벤트
├─ feature/    ViewModel, sdk/(Pleos SDK 연동), voice/(대화), route/(가게·수령 지점 계산)
└─ ui/         Compose 화면
```

SDK 동작 확인 기록: [docs/sdk_check.md](docs/sdk_check.md) · Gleo AI 연동: [docs/gleo_check.md](docs/gleo_check.md)
</details>
