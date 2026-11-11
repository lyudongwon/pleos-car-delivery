# Gleo AI 앱 연동(App Schema) 확인 (2026-11-11)

환경: Pleos Connect 에뮬레이터 v2.1.0 (API 34, 에뮬레이터 36.4.10), 시험 앱 `ai.cardeli.starter`
기준 문서: document.pleos.ai `pleos-only/app-integration-guide` (core-concepts, app-capability, deliver-schema, action_callback, examples)
직접 확인한 것만 적었다.

## 1. Gleo AI 앱

| 항목 | 결과 | 근거 |
|---|---|---|
| Gleo AI 앱이 있는가 | 된다 | `ai.umos.ambientai` versionName=4.2.0.15, codePath=/system_ext/app/AmbientAiApp. Gleo 자신의 앱 설명에 `"appName":"Gleo AI"` |
| 기본 음성 비서인가 | 된다 | `settings get secure voice_interaction_service` → `ai.umos.ambientai/.service.AiAssistantService` |
| 관련 패키지 | 확인 | `ai.pleos.playground.caas` 4.2.10-coral-aosp(음성·LLM 서비스, STT `SPEECH_START`, LLM `LLM_START`), `ai.pleos.gleoai.model.provider` 4.1.21-coral-aosp(모델 제공, `MODEL_PROVIDER_START`). `ai.pleos.service.trustagent`는 차 잠금 신뢰 에이전트로 무관 |
| 화면에서 부르기: ASSIST 인텐트 | 안 된다 | `am start -a android.intent.action.ASSIST` → `[GAA] onCreate() … startAmbientAiService() … onDestroy()`, 화면 변화 없음 |
| 화면에서 부르기: 음성 버튼 키 | 안 된다 | `input keyevent KEYCODE_VOICE_ASSIST` → Gleo 반응 없음 |
| 음성 세션 열기 | 된다 | `adb shell cmd voiceinteraction show` → `[GAA] startPttFromDriverSeat()`, 화면 위쪽에 "운전석에서 듣는 중…" 표시 (실제 차의 운전대 음성 버튼과 같은 동작: `MICOM_PTT_PRESS:SHORT`) |
| 텍스트 입력창 | 없다 | Gleo 화면(HistoryActivity)은 리마인더 목록뿐. uiautomator 덤프에 입력창 없음 |
| 웨이크워드 "글레오" | 미확인 | 마이크로 직접 말해 보지 않았다 |

## 2. schema.json 읽기

`app/src/main/assets/schema.json`: packageName `ai.cardeli.starter`, appDescription "An app for ordering food delivered to the car.", invokeWord "배달", actionCalls는 `ORDER_FOOD` 하나(menu string, quantity number, component activity).

| 항목 | 결과 | 근거 |
|---|---|---|
| 넣기 전 | 확인 | `[GAA] ai.cardeli.starter has no schema.json` / `getSchemaList - Boot End 3` |
| 설치할 때 읽는가 | 된다 | `[Schema] InstalledPackages User App Packages : [ai.cardeli.starter]` → `[Schema] Loaded schema.json from ai.cardeli.starter:` → `[Schema] loadSchemaFromAssets add or remove schema - when application is installed or uninstalled` → `getSchemaList - Boot End 4` |
| 재부팅할 때 읽는가 | 된다 | 재부팅 후 `[Schema] processSaveSchema - Boot from Read PackageManager` → `[Schema] Loaded schema.json from ai.cardeli.starter:` → `Boot End 4` |
| 승인 절차 | 에뮬레이터에서는 없었다 | 문서에는 "App Schema를 전달받으면 … 검토합니다. 승인 후에는 통합이 자동으로 활성화됩니다"라고 되어 있으나, 에뮬레이터에서는 승인 없이 바로 호출까지 됐다. 실제 차·앱마켓에서의 승인 절차는 미확인 |

## 3. 실제 호출

음성 입력: Gleo에 텍스트 입력이 없어서, Windows 한국어 음성(Microsoft Heami)으로 만든 "배달 앱으로 참치김밥 하나 주문해줘" WAV(16kHz/16bit/mono)를 에뮬레이터 gRPC `EmulatorController.injectAudio`로 가상 마이크에 실시간으로 넣었다(에뮬레이터를 `-grpc 8554 -grpc-use-token`으로 실행, 스크립트는 scratchpad의 `grpcclient/inject.js`). 세션은 `cmd voiceinteraction show`로 열었다.

| 항목 | 결과 | 근거 |
|---|---|---|
| Gleo 음성 인식 (서버 STT) | 된다 | `[GAA-STT]: FinalResult \| 배달 앱으로 참치 김밥 하나 주문해 줘. \| isWakeUpWord=false \| SERVER` |
| Gleo가 우리 앱 function을 고르는가 | 된다 | `[GAA-SDK]AppActionExecution: process : actionParameters: {"menu":"참치 김밥","quantity":1}, action: ORDER_FOOD, packageName: ai.cardeli.starter` |
| 인텐트 action 이름 | 문서와 다르다 | `[GAA] sendActionCall action: ai.cardeli.starter.ORDER_FOOD, component: activity`. 문서 예시는 action = function 이름(`ADD_REMINDER`)이지만 실제로는 **"패키지명.function 이름"**으로 보낸다. 처음에 `ORDER_FOOD`만 intent-filter에 넣었을 때 `No Activity found to handle Intent { act=ai.cardeli.starter.ORDER_FOOD … }` → Gleo가 "앱 연결에 문제가 있어서 말씀하신 것을 처리하지 못했어요."라고 말함. `ai.cardeli.starter.ORDER_FOOD`를 추가한 뒤 됨 |
| 우리 앱에 인텐트가 오는가 | 된다 | `GleoCheck: action=ai.cardeli.starter.ORDER_FOOD from=android-app://ai.umos.ambientai` (우리 앱 화면이 앞으로 뜸) |
| menu·quantity가 채워지는가 | 된다 | `GleoCheck: extra ACTION_PARAM = {"menu":"참치 김밥","quantity":1}` → `menu=참치 김밥 quantity=1`. 문서 예시처럼 따옴표로 한 번 더 감싸지 않은 JSON 문자열로 왔고, key별 개별 extra는 없었다 |
| 응답 브로드캐스트를 Gleo가 받는가 | 된다 | `[GAA] processReceiver(): APP_ACTION_RESPONSE` → `processAppActionResponse data: Bundle[{response={"functionName":"ORDER_FOOD","status":"success",…}, packageName=ai.cardeli.starter}]` |
| Gleo가 결과를 읽어 주는가 | 된다 | 스트리밍 TTS 문장 `배달 앱으로 참치 김밥 1개 주문할게요.` → `[GAAS][SDK]TtsCallback: onDone` |
| 메뉴 이름 정규화 | 참고 | 같은 문장을 세 번 넣었을 때 menu가 "참치 김밥", "참치김밥", "참치 김밥"으로 달랐다. 앱에서 띄어쓰기를 무시하고 메뉴를 찾아야 한다 |
| 주행 중(기어 D) 호출 | 미확인 | 정차(P)에서만 시험했다 |
| 실제 사람 목소리·마이크 | 미확인 | 합성 음성을 가상 마이크로 넣었다 |

## 4. 결론

**기획서에 Gleo AI 연동을 기능으로 써도 된다.** 에뮬레이터 v2.1.0에서 schema 등록(설치·재부팅 시 자동) → 음성 인식 → Gleo가 우리 앱의 ORDER_FOOD를 골라 menu·quantity를 채워 호출 → 앱 응답을 Gleo가 받아 "배달 앱으로 참치 김밥 1개 주문할게요."라고 읽는 것까지 직접 확인했다. 단, 실제 차·앱마켓 배포 때 schema 승인 절차가 있다고 문서에 적혀 있고, 인텐트 action은 문서와 달리 `패키지명.function 이름`으로 와서 둘 다 intent-filter에 넣어야 한다.
