# 협업 규칙

두 명이 같은 레포에서 작업한다. 기능(SDK·ViewModel)은 개발 담당, 화면(Compose UI)은 UI 담당이 맡는다.
폴더별 담당과 화면 작업 규칙은 [ui/README.md](app/src/main/java/ai/cardeli/starter/ui/README.md)에 있다.

## 1. 브랜치

- `main`은 항상 빌드되는 상태로 둔다.
- 기능·화면 작업은 브랜치를 따서 하고 PR로 합친다.
- 문서, 설정, 오타처럼 한두 줄짜리는 `main`에 바로 push해도 된다. 빌드만 깨지 않으면 된다.

| 접두어 | 용도 | 예 |
|---|---|---|
| `feat/` | 기능 추가 | `feat/f7-pickup-point` |
| `ui/` | 화면 작업 | `ui/menu-screen` |
| `fix/` | 버그 수정 | `fix/rider-distance` |
| `chore/` | 설정, 빌드, 문서 | `chore/gradle-compose` |

이름은 소문자와 `-`만 쓴다. 기능명세 ID가 있으면 넣는다 (`f7`).

## 2. 커밋 메시지

```
<타입>: <요약>

<본문 (필요할 때만)>
```

| 타입 | 언제 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `ui` | 화면·디자인 변경 |
| `refactor` | 동작은 같고 코드 구조만 바꿈 |
| `docs` | 문서 |
| `chore` | 빌드, 설정, 의존성 |
| `test` | 테스트 |

- 요약은 한국어로, 50자 안쪽, 끝에 마침표를 찍지 않는다.
- 기능명세 ID가 있으면 괄호로 붙인다.
- 한 커밋에는 한 가지 일만 담는다.

```
feat: 경로 위 수령 지점 계산 (F7)
ui: 메뉴판 화면 레이아웃
fix: 기사 거리가 음수로 나오는 문제
chore: Compose 의존성 추가
```

## 3. PR

- 제목은 커밋 메시지 형식과 같다.
- 본문은 PR 템플릿을 채운다. 짧게 써도 된다.
- **리뷰를 기다리지 않는다.** 빌드가 되는 걸 확인했으면 본인이 바로 합친다. 합칠 때는 **Squash and merge**를 쓴다.
- 상대가 맡은 폴더나 `contract/`를 건드렸으면 합친 뒤 상대에게 알린다.
- 합친 브랜치는 지운다.

## 4. 코드 컨벤션

기본은 [Kotlin 공식 코딩 컨벤션](https://kotlinlang.org/docs/coding-conventions.html)이다. `.editorconfig`에 들여쓰기·줄 길이 설정이 있어서 Android Studio에서 **Reformat Code**(Ctrl+Alt+L)만 하면 대부분 맞는다.

### 형식

- 들여쓰기 4칸, 한 줄 120자 안쪽
- `*` import를 쓰지 않는다
- 여러 줄로 쓴 인자·리스트의 마지막에 쉼표를 붙여도 된다

### 이름

| 대상 | 규칙 | 예 |
|---|---|---|
| 클래스, Composable 함수 | PascalCase | `OrderViewModel`, `MenuScreen` |
| 함수, 변수 | camelCase | `placeOrder`, `riderDistanceM` |
| 상수 | UPPER_SNAKE_CASE | `RIDER_STEP_MS` |
| 패키지 | 소문자 | `feature.sdk` |
| 파일 | 안의 주요 클래스·화면 이름과 같게 | `MenuScreen.kt` |

- 단위가 있는 값은 이름에 단위를 붙인다 (`distanceM`, `delayMs`, `price`는 원 단위).

### Compose

- 화면: `XxxScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit)`. 이 모양을 바꾸지 않는다.
- 컴포넌트: 필요한 값과 콜백만 받고, `modifier: Modifier = Modifier`를 첫 번째 선택 인자로 둔다.
- 화면·컴포넌트 안에서 SDK, ViewModel, `feature/`를 부르지 않는다.
- Preview 함수는 `private fun XxxPreview()`로 두고, 가짜 state는 `PreviewStates.kt`에서 가져온다.
- 색과 글꼴은 `MaterialTheme.colorScheme`과 `MaterialTheme.typography`로 쓰고 화면에 값을 직접 쓰지 않는다.
- 화면 문구는 시연 기간에는 코드에 바로 써도 된다.

### 기능 코드

- 화면 상태는 ViewModel의 `StateFlow<OrderUiState>` 하나로 내보낸다. 화면이 바꿀 수 있는 상태를 따로 두지 않는다.
- SDK 호출은 `feature/sdk/`에, 가상 데이터는 `feature/mock/`에 둔다.
- `check/`는 SDK 확인용 시험 코드라 위 규칙에서 뺀다. 앱 코드에서 `check/`를 참조하지 않는다.

### 주석

- 한국어로 쓴다. 코드가 "무엇을" 하는지보다 "왜" 그렇게 했는지를 적는다.
- 남은 일은 `TODO(담당 또는 기능 ID): 내용` 형식으로 적는다.

```kotlin
// TODO(UI): 메뉴 카드 디자인
// TODO(F11): adb broadcast로 받은 기사 좌표로 바꾼다
```
