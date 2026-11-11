# ui/ 작업 규칙 (UI 담당용)

브랜치·커밋·코드 컨벤션은 [CONTRIBUTING.md](../../../../../../../../CONTRIBUTING.md)를 따른다.

화면은 UI 담당, 기능(SDK·ViewModel·데이터)은 개발 담당이 맡는다. 서로 같은 파일을 건드리지 않도록 아래 규칙을 지킨다.

## 1. ui/ 밖은 수정하지 않는다

UI 담당이 고치는 곳은 `ui/` 아래뿐이다.

| 폴더 | 담당 | 내용 |
|---|---|---|
| `ui/theme/` | UI | 색상(Color.kt), 글꼴(Type.kt), 테마(Theme.kt). `TODO` 자리를 Pleos 디자인 규격 값으로 채운다 |
| `ui/components/` | UI | 여러 화면에서 같이 쓰는 컴포넌트 |
| `ui/screens/` | UI | 화면 4개와 Preview용 가짜 state(PreviewStates.kt) |
| `contract/` | 개발 | 화면과 기능이 주고받는 state·event 정의 |
| `feature/` | 개발 | ViewModel, SDK 연동(`sdk/`), 가상 데이터(`mock/`) |
| `MainActivity.kt` | 개발 | 상태에 따라 어떤 화면을 띄울지 고른다 |
| `check/` | 개발 | SDK 동작 확인용 시험 화면. 앱 본체와 상관없다 |

## 2. 화면에서 SDK·데이터를 직접 부르지 않는다

- 화면 함수는 모두 `(state: OrderUiState, onEvent: (OrderEvent) -> Unit)` 모양이다. 이 시그니처는 바꾸지 않는다.
- 보여줄 값은 `state`에서만 읽는다. `feature/`, `ai.pleos.*`, `android.car.*`를 import하지 않는다.
- 사용자 동작은 `onEvent(...)`로만 올린다. 화면 안에서 상태를 바꾸지 않는다.
  - 메뉴 추가·빼기: `OrderEvent.AddMenu(menu)`, `OrderEvent.RemoveMenu(menu)`
  - 주문 확인 화면으로: `OrderEvent.ReviewOrder` / 메뉴로 돌아가기: `OrderEvent.BackToMenu`
  - 주문하기: `OrderEvent.PlaceOrder`
  - 받았어요: `OrderEvent.ConfirmReceived`
- 펼침 여부, 스크롤 위치처럼 그 화면 안에서만 쓰는 상태는 `remember`로 둬도 된다.

## 3. 필요한 정보가 state에 없으면 개발 담당에게 요청한다

`contract/OrderContract.kt`를 직접 고치지 말고 "어떤 화면에 어떤 값이 필요하다"고 말해 준다. 개발 담당이 state에 넣는다. 개발 담당은 이 요청을 다른 작업보다 먼저 처리한다.

기다리는 동안 멈추지 않아도 된다. 그 값을 인자로 받는 컴포넌트를 먼저 만들고, Preview에서는 값을 직접 넣어 본다. state에 값이 들어오면 화면에서 `state.새값`을 넘기기만 하면 된다.

```kotlin
// state에 아직 없는 '대기 시간'을 보여줄 컴포넌트. 인자로 받아 두면 기다릴 필요가 없다
@Composable
fun WaitTimeBadge(waitMin: Int, modifier: Modifier = Modifier) { /* ... */ }

@Preview
@Composable
private fun WaitTimeBadgePreview() {
    CardeliTheme { WaitTimeBadge(waitMin = 5) }
}
```

## 화면별 역할

| 화면 | 언제 뜨나 | 메모 |
|---|---|---|
| `MenuScreen` | 정차 중, `Browsing` | 메뉴판. 받는 곳은 "지금 차 위치"로만 표시하고 주소 입력은 없다 |
| `ConfirmScreen` | 정차 중, `Confirming` | 주문 내용과 합계 확인 |
| `DeliveryScreen` | 정차 중, `Ordered`·`RiderComing`·`Arrived`·`Received` | 배달 진행. `riderDistanceM`으로 기사 거리 표시 |
| `DrivingScreen` | `isDriving == true`이면 단계와 상관없이 | 주행 중 화면. 터치 버튼을 두지 않는다. '주행 중' 배지, 텍스트 1줄, 음성 상태만 |

## Preview

- 화면마다 `@Preview`가 있고 가짜 state는 `PreviewStates.kt`에 있다. 에뮬레이터 없이 Android Studio에서 바로 본다.
- Preview 크기는 임시로 1280×720이다. 디자인 규격 해상도가 정해지면 맞춘다.
