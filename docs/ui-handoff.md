# UI 담당 전달 사항

contract(`contract/OrderContract.kt`)가 바뀔 때마다 여기에 적는다. 위에 있을수록 최신이다.

## 2026-11-15 · 시연 영상 흐름에 맞춘 값 추가 (contract/OrderContract.kt)

| 값 / 이벤트 | 내용 | 영상 화면 |
|---|---|---|
| `stores: List<StoreCard>` | 정차 중 가게 목록. 이름·카테고리·거리(m)·예상 시간(분)·배달비·`beforeChargeEnds` | 주문하기(가게 목록) |
| `OrderEvent.SelectStore(id)` | 가게를 고르면 `menus`·`deliveryFee`가 그 가게로 바뀐다 | 가게 → 메뉴 |
| `deliveryFee`, `payTotal` | 배달비, 결제 금액(메뉴 + 배달비) | 결제 금액 |
| `options: OrderOptions` | 배달 방식·수령 창문·위치 알림·추가 전달 사항·가게 요청사항·결제 방식 | 주문서 |
| `SelectMethod` · `SelectWindow` · `SelectLocateBy` · `SetRiderNote` · `SetStoreNote` · `SelectPayment` | 위 값을 바꾸는 이벤트. enum마다 화면 문구용 `label`이 있다 | 주문서 |
| `OrderStatus.Cooking`, `Delivering` | 진행 단계 추가: 접수(Ordered) → 조리 중 → 배달 시작 → 곧 도착(RiderComing) → 도착(Arrived) → 완료(Received) | 주문 내역 진행 표시 |
| `etaMin` | 배달 완료까지 남은 시간(분). 조리 + 기사 이동으로 계산 | "약 N분 후" |
| `OrderEvent.NotReceived` | "음식을 수령하셨나요?"에서 "아니요, 못 받았어요" | 수령 확인 |
| `isParked` | 기어 P. 이때 주문하면 차 위치를 받는 곳으로 확정(`delivery.fixedByPark`) | |
| `charging: ChargingInfo?` | 충전 중이면 잔량(%)과 목표까지 남은 시간(분). 이때 `stores`는 충전 끝나기 전에 받을 수 있는 가게가 앞 | |
| `delivery.window`, `locateBy`, `riderNote` | 기사에게 같이 가는 값 | 라이더 곧 도착 화면 |

- `Arrived`가 되면 "음식을 수령하셨나요?"(네 = `ConfirmReceived`, 아니요 = `NotReceived`)를 띄우면 된다.
- 위치 알림이 비상등이면, 기사가 200m 안에 들어올 때 앱이 "비상등을 켜 주세요"라고 말한다(차의 비상등을 앱이 켜지는 못한다).
- `suggestions`는 이제 한 개(지난번 주문한 가게)만 온다.

## 2026-11-11 · 수령 지점 도착(atPickup) 추가

`OrderUiState.atPickup: Boolean` — 차가 수령 지점 반경 100m 안에 들어오면 true. DrivingScreen에서 "도착" 표시에 쓴다. 이때 `assistantText`는 "○○에 도착했어요. 기사님이 오고 있어요."다.

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
    suggestions = listOf(StoreSuggestion("길목김밥", 14900), StoreSuggestion("모퉁이김밥", 16700)),
    pickup = PickupPoint("길목 공영주차장", detourMin = 2, detourM = 1400, waitMin = 5),
)
```

## 2026-11-11 · S2 메뉴

메뉴판(MenuScreen) 가게가 `서현김밥`이 됐다. 메뉴는 원조김밥 4,000원, 참치김밥 5,500원, 라볶이 6,500원, 어묵국 4,000원. 참치김밥 + 어묵국 = 9,500원이 시연 주문이다.
