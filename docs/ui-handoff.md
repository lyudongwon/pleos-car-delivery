# UI 담당 전달 사항

contract(`contract/OrderContract.kt`)가 바뀔 때마다 여기에 적는다. 위에 있을수록 최신이다.

## 2026-11-11 · 배달 정보(delivery) 추가

`OrderUiState.delivery: DeliveryInfo?` — 주문 뒤 채워진다. 기사에게 가는 정보다(기사 화면, 배달 화면).

| 필드 | 뜻 | 예 |
|---|---|---|
| `placeLabel` | 받는 곳 이름. S2는 "지금 차 위치", S1은 수령 지점 이름 | 지금 차 위치 / 길목사거리 정차구역 |
| `latitude`, `longitude` | 받는 곳 좌표 (주소 대신) | 37.3838, 127.1240 |
| `plate`, `color` | 처음 한 번 등록해 둔 차량 번호·색상 | 12가 3456, 흰색 |
| `carModel` | 차에서 읽은 차종 | Hyundai 42dot Connect |

DeliveryScreen이나 기사 화면(F11)에서 "기사에게 이렇게 전달됐다"를 보여줄 때 쓴다. `riderDistanceM`은 기사 좌표와 이 좌표 사이 거리다.

## 2026-11-11 · S1(주행 중 음성 주문) 값 추가

`OrderUiState`에 아래 값이 생겼다. 모두 기본값이 있어서 지금 화면 코드는 그대로 빌드된다. 값은 기능이 붙는 대로 채워진다(아래 "채워지는 때").

| 값 | 타입 | 뜻 | 보여줄 화면 | 채워지는 때 |
|---|---|---|---|---|
| `voiceState` | `VoiceState` (Idle·Listening·Thinking·Speaking) | 음성 대화 상태 | DrivingScreen: 듣는 중/말하는 중 표시 | F2·F5 |
| `transcript` | `String` | 운전자가 말하는 중인 문장(인식 중간 결과) | DrivingScreen: 자막처럼 | F2 |
| `assistantText` | `String` | 앱이 마지막으로 한 말 | DrivingScreen: 텍스트 1줄 | F5 |
| `suggestions` | `List<StoreSuggestion>` (storeName, aheadM) | 경로 근처 추천 가게, 최대 2개 | DrivingScreen: 가게 이름 두 줄 | F6 |
| `pickup` | `PickupPoint?` (name, detourMin, detourM, waitMin) | 정해진 수령 지점 | DrivingScreen: "○○에서 받아요 · +N분" | F7·F8 |
| `touchCount` | `Int` | 주행 중 화면 터치 횟수 | DrivingScreen 구석: "화면 터치 N회" | 지금 동작함 |

주행 화면(DrivingScreen) 규칙: Pleos 주행 제한(DO) 때문에 텍스트는 1줄, 터치 버튼은 두지 않는다. '주행 중' 배지, 음성 상태, `assistantText` 한 줄, `pickup` 한 줄, 터치 횟수 정도만 둔다.

`touchCount`는 화면 코드가 아니라 `MainActivity`가 창 전체에서 센다. 화면에서 따로 할 일은 없다.

Preview에서 보려면 `PreviewStates.kt`의 가짜 state에 값을 넣으면 된다. 예:

```kotlin
val driving = browsing.copy(
    isDriving = true,
    voiceState = VoiceState.Speaking,
    assistantText = "지나가는 길에 길목김밥, 모퉁이김밥이 있어요.",
    suggestions = listOf(StoreSuggestion("길목김밥", 1900), StoreSuggestion("모퉁이김밥", 3500)),
    pickup = PickupPoint("길목 공영주차장", detourMin = 2, detourM = 1400, waitMin = 5),
)
```

## 2026-11-11 · S2 메뉴

메뉴판(MenuScreen) 가게가 `서현김밥`이 됐다. 메뉴는 원조김밥 4,000원, 참치김밥 5,500원, 라볶이 6,500원, 어묵국 4,000원. 참치김밥 + 어묵국 = 9,500원이 시연 주문이다.
