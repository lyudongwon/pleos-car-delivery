# Pleos Connect SDK 확인 결과

확인일: 2026-11-10 · 방법: Maven에서 받은 AAR을 `javap`로 열어본 것(1~3)과, Pleos Connect 에뮬레이터 v2.1.0에서 시험 앱(`check/SdkCheckActivity`, `check/VehicleCheckActivity`)을 돌린 것(4). 추측은 적지 않았다. 확인하지 못한 것은 "미확인"으로 적었다.

## 1. 받은 SDK와 버전

| SDK | 버전 | 함께 받는 Pleos 패키지 |
|---|---|---|
| Vehicle | 2.3.2 | vehicle-aidl 1.1.1, vehicle-data 1.1.11 |
| NaviHelper | 3.0.5 | navi-data 3.0.4, navi-aidl 3.0.1 |
| SpeechToText | 3.3.1 | universal-access 0.6.0 |
| TextToSpeech | 3.3.1 | universal-access 0.6.0 |
| FusedLocation | 1.1.3 | fused-location-data 0.0.3, fused-location-aidl 0.0.5 |
| 에뮬레이터 | Pleos Connect v2.1.0 (API 34) | |

- 저장소 `https://nexus-playground.pleos.ai/repository/maven-releases/` 는 신청·로그인 없이 받아진다.
- **LLM이나 Gleo AI SDK는 없다.** Maven에 있는 공개 SDK는 위 다섯 개뿐이다. 에뮬레이터 안에 `ai.pleos.gleoai.model.provider` 패키지는 있지만 앱용 SDK는 없다. STT SDK 안에 `AmbientAiApi`(웨이크업 워드, 화자 인식, LM 부스트 설정)가 있으나 LLM 기능은 아니다.

## 2. NaviHelper

### 메서드 (`ai.pleos.playground.navi.helper.NaviHelper`)

모든 조회 메서드는 반환값이 없다(`void`). 결과는 `NaviHelperEventListener` 콜백으로 온다.

| 메서드 | 결과가 오는 콜백 | 필요 권한 |
|---|---|---|
| `initialize()` / `initialize(onSuccess, onFailed)` | | |
| `getCurrentLocationInfo()` | `onCurrentLocationInfo(CurrentLocationInfo)` | `pleos.car.permission.NAVI_ROUTE` |
| `getRouteStateInfo()` | `onRouteStateInfo(RouteStateInfo)` | NAVI_ROUTE |
| `getDestinationInfo()` | `onDestinationInfo(DestinationInfo)` | NAVI_ROUTE |
| `getWaypointInfo()` | `onWayPointInfo(List<WaypointInfo>)` | NAVI_ROUTE |
| `getBookmarkInfo()` | `onBookmarkInfo(List<BookmarkInfo>)` | NAVI_ROUTE |
| `getRecentDestinationInfo()` | `onRecentDestinationInfo(List<RecentDestinationInfo>)` | NAVI_ROUTE |
| `getTBTInfo()` | `onTBTInfo(List<TBTInfo>)` | NAVI_ROUTE |
| `getChargerOperatorInfo()` | `onChargerOperatorInfo(...)` | NAVI_ROUTE |
| `requestTrip(TripRequest)` | `onTripInfo(TripInfo)` | NAVI_ROUTE |
| `requestRoute(RouteInfo)` | `onRouteStarted`, `onNavigationStatus` | `pleos.car.permission.NAVI_ROUTE_SEARCH` |
| `requestRouteWithWaypoints(RouteWithWaypointsRequest)` | 같음 | NAVI_ROUTE_SEARCH |
| `addWaypoint(RequestWaypointInfo)` | `onWaypointChanged(WaypointChangedInfo)` | NAVI_ROUTE_SEARCH |
| `removeWaypoint(WaypointIndex)` | `onWaypointChanged` | NAVI_ROUTE_SEARCH |
| `requestReRoute()` | `onReRouteEnded` | NAVI_ROUTE_SEARCH |
| `cancelRoute()` | `onRouteCancelled` | NAVI_ROUTE_SEARCH |
| `changeRouteOption(RouteOption)` | `onRouteOptionChanged` | NAVI_ROUTE_SEARCH |
| `requestNavigationScreen(NavigationScreenRequest)` | | `pleos.car.permission.NAVI_CUSTOM_MAP` |

기획서에 적은 메서드 10개(`getCurrentLocationInfo`, `getRouteStateInfo`, `getDestinationInfo`, `addWaypoint`, `removeWaypoint`, `requestReRoute`, `getWaypointInfo`, `getBookmarkInfo`, `getRecentDestinationInfo`, `requestTrip`)는 **모두 있다.** 리스너에 **`onWayPointArrived(WaypointArrivedInfo)`도 있다.**

### 데이터 클래스 필드

| 클래스 | 필드 |
|---|---|
| `RouteStateInfo` | termsAgreement, naviMode, isRouting, routeOption, remainDestinationChargingInt, remainWaypointChargingInt(List), trafficStatus |
| `CurrentLocationInfo` | latitude, longitude, heading, address, routeId, currentRouteIndex |
| `DestinationInfo` | latitude, longitude, displayLatitude, displayLongitude, name, address, remainDistance, remainTime, tollFare, poiId, poiSubId, poiProvider |
| `WaypointInfo` | name, address, latitude, longitude, displayLatitude, displayLongitude, remainDistance, remainTime, poiId, poiSubId, poiProvider |
| `RequestWaypointInfo` | latitude, longitude, poiName, waypointIndex, poiId, address, poiSubId, poiProvider |
| `WaypointArrivedInfo` | duration, distance, index, name, address |
| `TripRequest` | latitude, longitude, poiId, poiName, poiSubId |
| `TripInfo` | distance, eta, estimatedSocAtDest, energyConsumption, summary(TripSummary) |
| `TripSummary` | trafficConditions(List), routePoints(List\<RoutePointInfo\>) |
| `RoutePointInfo` | latitude, longitude, poiName, poiId, ratioOnRoute, poiType, pointType(ORIGIN·WAYPOINT·DESTINATION), settingType(AUTO·MANUAL) |
| `TBTInfo` | type(TurnType), distance, description |

### 확인 질문

| 질문 | 답 | 근거 |
|---|---|---|
| 경로 좌표 목록(폴리라인)을 주는가 | **아니오** | `RouteStateInfo`에 좌표 필드가 없다. `TripSummary.routePoints`는 출발지·경유지·목적지 지점(`RoutePointType`)과 경로 위 비율만 준다. 에뮬레이터에서 `requestTrip` 결과의 routePoints는 목적지 1개뿐이었다. |
| 경유지 최대 개수 | **2개** | `WaypointIndex` = FIRST, SECOND, ALL(ALL은 삭제용) |
| 지점까지 예상 거리·시간 | **된다** | `requestTrip` → `TripInfo.distance`(m), `eta`(초). 에뮬레이터: 2,814m / 449초 |

## 3. 음성·위치 SDK

| SDK | 클래스·메서드 | 필요 권한 |
|---|---|---|
| SpeechToText | `SpeechToText(context, Mode)`, `initialize(OnInitializeListener)`, `addListener(ResultListener)`, `request()`, `stop()`, `sendAudio(ByteArray)`, `setLocale`, `registerApp(clientId, clientSecret, listener)` | `pleos.car.permission.STT_SERVICE` |
| TextToSpeech | `TextToSpeech(context, Mode)`, `initialize(OnInitializeListener)`, `addEventListener(EventListener)`, `speak(text, Boolean)`, `stop()`, `setSpeakerType`, `registerApp(...)` | `pleos.car.permission.TTS_SERVICE` |
| FusedLocation | `FusedLocation(context)`, `initialize()`, `isSupported()`, `getLastKnownLocation(listener)`, `registerFusedLocationCallback(listener)`, `addProximityAlert(...)` | `pleos.car.permission.FUSED_LOCATION` |

- 기획서에 적은 `request`, `addListener`, `speak`, `registerFusedLocationCallback`, `isSupported`는 **모두 맞다.**
- STT·TTS 모드: `SERVER`, `ON_DEVICE`, `HYBRID`. **SERVER 모드(와 HYBRID의 서버 인식)는 Pleos Playground에서 My Project를 만들 때 받는 Client ID·Client Secret으로 `registerApp`을 해야 한다**(문서 App registration). ON_DEVICE만 쓰면 등록이 필요 없다.
- TTS는 `initialize`가 끝난 뒤 `addEventListener`를 불러야 한다. 먼저 부르면 `UninitializedPropertyAccessException`이 난다(에뮬레이터에서 확인).
- STT `ResultListener`: onUpdated(text, isFinal), onStartedRecognition, onEndedRecognition, onReady, onError, onUpdatedRms, onUpdatedEpdData.

## 4. 에뮬레이터 시험 결과

| 항목 | 결과 |
|---|---|
| Virtual Console에서 기어 D → 주행 제한(UXR) | **켜진다.** `requiresDistractionOptimization=true`, restrictions=80. DO를 선언한 앱은 70km/h에서도 화면이 유지된다. |
| 창문 값·리스너 | `getCurrentWindowPosition`, `registerWindowPosition`(`WindowPositionListener.onWindowPositionUpdated`). 값 **0~10**. Virtual Console에서 운전석 창문을 100%로 열면 10, 닫으면 0이 이벤트로 온다. 권한 `pleos.car.permission.CAR_WINDOWS`. |
| INFO_MAKE / INFO_MODEL | "Hyundai 42dot" / "Connect" |
| 차량 등록번호 `getCRNInfo` | "invalid". 폐기 예정 API(빌드 경고: getVINInfo를 쓰라고 함). 번호판은 사용자가 등록해야 한다. |
| 기어 (Pleos Vehicle SDK `Gear`) | 동작하지만 폐기 예정. AAOS `GEAR_SELECTION`을 쓰라고 나온다. |
| NaviHelper 현재 위치 | 37.4127212, 127.094681 (성남 수정구 시흥동)에서 고정 |
| FusedLocation 위치 | Android GPS를 따른다(기본값은 미국 마운틴뷰, `adb emu geo fix`로 바꾸면 바뀜). **NaviHelper 위치와 다르다.** 에뮬레이터에서 두 위치를 섞어 쓰면 안 된다. |
| 목적지 + 경유지 2개 한 번에 (`requestRouteWithWaypoints`) | 된다. 경유지 2개가 잡힌다. |
| **경유지 좌표 (0,0)** | **재현됨.** `getWaypointInfo`가 경유지 이름과 남은 거리·시간은 주지만 위도·경도는 0.0이다. 앱이 넣은 좌표를 따로 저장해야 한다. |
| 안내 중 `addWaypoint` 한 번 | 된다. `onRouteCancelled` → 약 5.5초 뒤 `onWaypointChanged(FIRST, ADD)`, 목적지는 그대로 유지된다. |
| 경유지 추가 전후 목적지까지 | 5,700m·660초 → 6,900m·960초 (+1.2km, +5분). `getDestinationInfo`의 남은 거리·시간 차이로 경로 이탈을 계산할 수 있다. |
| `removeWaypoint` 직후 | 경로가 취소되고 다시 잡히지 않았다(목적지까지 사라짐). 경로 변경은 이전 변경의 재탐색(약 5초)이 끝난 뒤에 해야 한다. |
| 경로가 없을 때 `addWaypoint` | 경유지가 목적지처럼 잡힌다(그 지점까지 경로 생성). |
| `onWayPointArrived` 실제 발생 | **미확인.** 에뮬레이터의 내비 위치는 GPS 변경(geo fix)에도, Virtual Console 가속에도 움직이지 않았다(경유지까지 남은 거리 2,800m 그대로). 내비 앱의 모의 주행(`NaviMode.SIMULATION_GUIDING`)으로 시험할 수 있는지 확인이 필요하다. |
| TTS (ON_DEVICE, 등록 없이) | `initialize` 성공, `onReady`, `onStart`까지 온다. `onDone`은 확인하지 못했다. |
| STT (ON_DEVICE, 등록 없이) | `initialize` 성공, `request` 후 `onReady`까지 온다. 실제 음성 인식 결과는 마이크 입력을 넣지 못해 미확인. |

## 5. 기획서에 반영할 것

| 항목 | 결과 | 기획서 |
|---|---|---|
| 경로 좌표 | 없음 | 5번: 현재 위치·목적지·남은 거리는 NaviHelper, 만남 지점 후보 계산에 필요한 경로선은 외부 경로 API로. 후보까지 거리·시간은 `requestTrip`로 비교 가능 |
| 경유지 개수 | 최대 2개 | 5번: 경유지 2개가 이미 차 있으면 만남 지점을 앱 화면에만 표시 |
| 경유지 좌표 (0,0) | 재현됨 | 기획서 변경 없음. 구현에서 앱이 넣은 좌표를 저장 |
| 메서드 이름 | 모두 맞음 | 변경 없음. 조회는 콜백으로 받는다는 점만 구현에서 반영 |
| 창문 값 | 0~10 위치값, 리스너 동작 | 5번 그대로 |
| LLM/Gleo SDK | 없음 | 5번: 주문 해석은 서버의 외부 LLM 그대로 |
| STT 서버 모드 | Client ID·Secret 필요 | 5번 또는 준비: Pleos Playground My Project 생성 필요. ON_DEVICE로 시작 가능 |
| 차량 번호 | 차에서 못 읽음 | 4번 시나리오 2의 "차가 알고 있는 차량 번호" → "처음 한 번 등록한 차량 번호" |

## 6. 추가 확인 (2026-11-11)

| 항목 | 결과 |
|---|---|
| STT 실제 인식 (ON_DEVICE, 등록 없이) | 된다. 한국어 합성 음성 PCM(16kHz·16bit·mono, `app/src/main/assets/order.pcm`)을 `sendAudio`로 넣으니 "가는 길을 받을 수 있는 국밥 시켜줘"(원문 "가는 길에…")가 최종 결과로 나왔다. 중간 결과가 단어 단위로 온다. `SdkCheckActivity --es test S` |
| FusedLocation 근접 알림 | 된다. 반경 100m 등록 후 `adb emu geo fix`로 이동하니 ENTER, EXIT이 왔다. `SdkCheckActivity --es test P` |

## 7. LLM SDK (2026-11-11)

결론: 받을 수는 있지만 에뮬레이터에서 응답을 받지 못함. 공식적으로 제공 종료.

| 확인 항목 | 결과 |
|---|---|
| Maven 저장소 | `ai.pleos.playground:LLM` 2.1.2 / 2.1.3.1 / 2.1.3.2 남아 있음. 마지막 업로드 2025-03-24 |
| 공식 공지 | 에뮬레이터 v2.1.0(2026-10-19) 릴리즈 노트 Removed Features에 "LLM SDK 제공 종료" |
| 문서 | LLM API 문서 페이지 주소는 남아 있으나 내용이 비어 있음(제목만) |
| 에뮬레이터 서비스 | `ai.pleos.playground.caas`(AmbientAiService)의 `LLMService`가 `LLM_START`를 받음. 연결 성공 |
| 권한 | `pleos.car.permission.LLM_SERVICE` 필요(protection normal). 없으면 initialize에서 SecurityException |
| ON_DEVICE 모드 | 서비스 로그 "on-device LLM is disabled". 응답·오류 콜백 둘 다 없음 |
| SERVER 모드 | 요청은 만들지만 인증 토큰이 없어(expired, uaToken 비어 있음) 서버로 안 보냄. 오류 콜백도 없음. registerApp(Client ID, Client Secret) 필요 |

시험 코드: `LlmCheckActivity` (`adb shell am start -n ai.cardeli.starter/.check.LlmCheckActivity [--es mode SERVER]`, 로그 `adb logcat -s LlmCheck`)

## 추가 확인 (2026-11-11 밤): Virtual Console HTTP API, 앱 화면 전환

| 항목 | 결과 |
|---|---|
| Virtual Console 위치 | 에뮬레이터 안 `ai.pleos.connectwebconsole`이 기기 8088 포트로 웹 콘솔을 띄운다. `adb forward tcp:8088 tcp:8088` 후 http://localhost:8088 |
| 명령으로 기어 바꾸기 | **된다.** `PUT /vehicle/gear?gear=Drive` (Park·Reverse·Neutral·Drive). 3초 안에 `POST /browser/session?client=<이름>` heartbeat가 없으면 `{"error":"emulator_inactive"}`로 거절한다. 바꿀 수 있는 항목 목록은 `GET /vehicle/catalog` (controls, body, driving-mode 등) |
| VHAL 직접 주입 | 안 된다. `cmd car_service inject-vhal-event`는 user 빌드라 SecurityException |
| 앱 화면 전환 (F1) | **된다.** 기어 Park → MenuScreen(`isDriving=false`), Drive → DrivingScreen, 다시 Park → MenuScreen. `DrivingStateSource`(CarUxRestrictionsManager)로 받음 |
| Compose Preview 기기 렌더링 | **된다.** `androidx.compose.ui.tooling.PreviewActivity`로 화면 4개의 Preview가 모두 그려짐 |

## 0단계 PoC 확인 (2026-11-11 밤)

현재 위치는 에뮬레이터 내비 고정값(성남 수정구 시흥동 37.41272, 127.09468), 시험 목적지는 북동쪽 약 4km(37.44272, 127.12468). `SdkCheckActivity --es test T [--es mode fresh]`, 로그 `adb logcat -s PleosCheck` 의 `[T]` 줄.

| 항목 | 결과 |
|---|---|
| `requestTrip` 후보별 호출 | **된다.** 후보 5곳에 차례로 부르면 5번 모두 `onTripInfo`가 온다. 다만 **현재 위치 → 후보**까지의 거리·시간만 준다(예: 1,799m·367초). 후보를 거쳐 목적지까지 늘어나는 양은 이것만으로 알 수 없다 |
| 경유지를 바꿔 끼우며 측정 (`addWaypoint FIRST` 반복) | **첫 번째만 된다.** 첫 후보는 5,700m·780초 → 7,000m·1,020초로 잡혔지만, 두 번째부터는 경로가 다시 잡히지 않거나 값이 그대로였다 |
| 후보마다 경로 새로 요청 (`requestRouteWithWaypoints(목적지, [후보])`) | **된다.** 후보마다 약 12초. 요청 직후 `onDestinationInfo`가 한 번 0으로 오고, 재탐색이 끝나면 실제 값이 온다 |
| TTS `onDone` | **오지 않는다.** 두 번 돌려 `onReady`·`onStart`까지 왔지만 100초를 기다려도 `onDone`이 없었다. 응답이 끝나는 시점은 글자 수로 잡는다 |
| 외부 경로 API (OSRM 공개 서버, 키 없음) | **된다.** `route` 서비스가 도로를 따라가는 좌표 234개(6.6km)를, `table` 서비스가 지점 간 거리·시간 행렬을 준다. 시연 경로 모양을 뽑는 데 쓴다 |

후보별 실측 (기준: 목적지만 5,700m·780초, 내비 값은 100m·60초 단위로 반올림)

| 후보 | 좌표 | 경로 새로 요청 후 | 추가 거리 | 추가 시간 |
|---|---|---|---|---|
| 0 | 37.41872, 127.10068 | 7,000m · 1,020초 | +1.3km | +4분 |
| 1 | 37.42872, 127.10268 | 7,400m · 1,200초 | +1.7km | +7분 |
| 2 | 37.42372, 127.11368 | 7,100m · 900초 | +1.4km | **+2분** |
| 3 | 37.43072, 127.11268 | 6,900m · 1,080초 | +1.2km | +5분 |
| 4 | 37.44272, 127.11268 | 9,700m · 1,380초 | +4.0km | +10분 |

결론: 후보별 추가 시간은 **내비에 후보마다 경로를 새로 요청해서 실측**할 수 있다. 5곳이면 1분쯤 걸려 시연 중 실시간으로 하기엔 느리므로, 시연 후보는 미리 이 방식으로 재 두고(F7) 앱은 고른 지점 하나만 실제로 경유지에 넣어 "+N분"을 다시 잰다(F8).

## F4 음성 응답 확인 (2026-11-11 밤)

`feature/sdk/Speaker.kt`, 데모 명령 `demo_say "<문장>"`, 로그 `adb logcat -s Speaker` 와 음성 서비스 로그 `[GAAS]OnDeviceStreamPlayerImpl`.

| 항목 | 결과 |
|---|---|
| 앱 화면에서 TTS (ON_DEVICE, Application context) | **된다.** `speak` 후 약 0.2~0.6초 뒤 `writeAudioTrack(ai.cardeli.starter:ondevice)`로 오디오가 나간다. `onStart`는 오고 `onDone`은 오지 않는다 |
| 두 문장 연속 | 앱이 차례로 말하게 하면 겹치지 않는다. 끝나는 시점은 글자 수로 추정: 0.9초 + 글자당 0.22초. 24자 문장에서 추정 끝(22.05초)과 마지막 오디오 쓰기(22.31초)가 0.3초 차이 |
| 남은 음성 인식 세션 | 시험 화면(`SdkCheckActivity`)이 `stt.request()` 후 강제 종료되면 음성 서비스(`ai.pleos.playground.caas`)에 인식 세션이 남아, 0.1초마다 `ai.cardeli.starter not found check register` 오류가 쌓이고 TTS 콜백(`onStart`)이 오지 않으며 첫 문장이 재생되지 않았다. `adb shell am force-stop ai.pleos.playground.caas` 후 정상 |

## F2 음성 인식 확인 (2026-11-11 밤)

`feature/sdk/Listener.kt`, 데모 명령 `demo_utter <이름>`, 대사 파일 `app/src/main/assets/voice/*.pcm` (Windows Microsoft Heami 한국어 합성 음성, 16kHz·16bit·mono, 앞뒤 0.5초 무음).

| 대사 | 인식 결과 |
|---|---|
| `s1_1_order` "가는 길에 받을 수 있는 김밥 시켜줘" | "가는 길에 받을 수 있는 김밥 시켜줘" (정확) |
| `s1_2_pick` "첫 번째, 참치김밥 하나랑 어묵국 하나" | "첫 번째 참치김밥 하나랑 어묵국 하나" (정확, 2회 모두) |
| `s1_3_yes` "응. 주문해." | "응 주문해" (정확) |

- 인식 중간 결과가 단어 단위로 와서 `transcript`(자막)에 실시간으로 들어간다. 1초 시점에 "한", "응"처럼 앞부분이 보인다.
- **문장 사이 쉼에서 인식이 끝난다.** 처음에 "첫 번째. 참치김밥 하나랑 어묵국 하나."로 만들었더니 마침표 뒤 쉼에서 끝나 "첫 번째"만 남았다. 대사는 마침표 없이 한 호흡으로 만든다(쉼표는 괜찮다).
- 마이크 인식(`request`)을 쓰지 않아 음성 서비스에 세션이 남지 않는다. 세 대사를 돌린 뒤에도 `check register` 오류 0건.

## F12·F13 확인 (2026-11-11 밤)

| 항목 | 결과 |
|---|---|
| 근접 알림 등록 타이밍 | ENTER는 반경 밖에서 안으로 들어올 때만 온다. 등록 때 이미 안에 있으면 오지 않아서, 등록 직후 현재 위치로 한 번 더 확인한다 |
| Virtual Console 창문 | `PUT /vehicle/body?window_fl=0~100` (heartbeat 필요). Vehicle SDK 리스너에는 0~10으로 온다 |
| `removeWaypoint` | 경로 재탐색이 끝난 뒤 부르면 경유지가 빠지고 원래 목적지로 다시 잡힌다(`onWaypointChanged FIRST REMOVE`, 5,100m). 0단계에서 본 "다시 안 잡힘"은 재탐색 중에 불렀기 때문 |
| STT 쉼표 | "응, 받았어"는 쉼표 쉼에서 끊겨 "응"만 인식됐다. 짧은 대사는 쉼표도 빼고 만든다 |

## F7 앱 안 실시간 계산 · 앱 동결 (2026-11-11 밤)

| 항목 | 결과 |
|---|---|
| 앱이 직접 후보별 실측 (`demo_measure`) | **된다.** 앱 본체가 `requestRouteWithWaypoints`로 지점마다 경로를 새로 요청하고 `requestTrip`으로 현재→지점 시간을 받아 6곳을 56초에 쟀다(곳당 약 7초). 거리는 시험 화면으로 미리 잰 값과 모두 같고, 시간은 에뮬레이터 교통 상황에 따라 ±1분 달랐다. 원본 `docs/poc/F7_pickup_measure_live_1111.json` |
| **앱 동결** | **내비가 앞에 나오면 우리 앱이 백그라운드가 되어 Android 14 프로세스 동결로 얼었다**(`dumpsys activity processes` → `isFrozen=true`). 이때 타이머·위치·창문·음성 처리가 멈추고, adb 명령이 올 때만 잠깐 풀렸다. 스모크가 통과했던 건 상태를 1초마다 묻는 명령이 앱을 깨웠기 때문이다. **포그라운드 서비스(`KeepAliveService`, specialUse)를 띄워 해결** → 내비가 앞에 있어도 `isFrozen=false` |
| 로그 버퍼 | 에뮬레이터 내비(GpsLocationSource) 경고가 2MB 로그 버퍼를 빠르게 밀어낸다. 긴 확인 전에는 `adb logcat -G 16M` |
