# 데모 명령 (F14)

시연 촬영과 테스트에서 화면을 건드리지 않고 앱을 조작한다. 주행 장면에서 화면을 터치하면 터치 카운터가 올라가므로 촬영 중 조작은 전부 명령으로 한다.

## 준비

```bash
export ADB=adb          # adb가 PATH에 없으면 전체 경로
source scripts/demo.sh
```

## 명령

| 함수 | 하는 일 |
|---|---|
| `demo_start` | 앱을 새로 띄운다 |
| `demo_route` | 내비에 S1 목적지(서현역) 안내를 시작한다. 운전자가 목적지를 찍고 출발한 상태 |
| `demo_pickup_log` | F7 수령 지점 계산 로그(JSON)를 출력한다 |
| `demo_reset` | 주문 상태를 처음으로 되돌린다 (주행 여부는 차량 상태를 따름) |
| `demo_state` | 현재 state를 JSON으로 저장하고 출력한다 |
| `demo_event <이름> [메뉴 id]` | 화면 대신 이벤트를 보낸다: `AddMenu`·`RemoveMenu`(id 필요), `ReviewOrder`, `BackToMenu`, `PlaceOrder`, `ConfirmReceived` |
| `demo_rider <위도> <경도>` | 배달기사 위치를 보낸다 (F11) |
| `demo_utter <이름>` | 녹음 대사 `assets/voice/<이름>.pcm`을 음성 인식에 넣는다. S1 대사: `s1_1_order`, `s1_2_pick`, `s1_3_yes` |
| `demo_say "<문장>"` | 앱이 문장을 음성으로 읽는다 |
| `demo_geo <위도> <경도>` | 차 위치를 옮긴다 (FusedLocation, `adb emu geo fix`) |
| `demo_gear <Park·Drive·Reverse·Neutral>` | Virtual Console로 기어를 바꾼다 |

메뉴 id: S2 서현김밥은 `s2-1` 원조김밥, `s2-2` 참치김밥, `s2-3` 라볶이, `s2-4` 어묵국.

직접 보내려면:

```bash
adb shell am broadcast -n ai.cardeli.starter/.feature.demo.DemoReceiver --es cmd event --es name AddMenu --es id s2-2
```

값은 모두 문자열(`--es`)로 보낸다. 좌표를 `--ef`로 보내면 float라 정밀도가 깨진다.

## 스모크 테스트

```bash
bash scripts/smoke_s1.sh     # S1 주행 중 음성 주문: 기어 D, 대사 3개로 주문 완료까지, 터치 0회
bash scripts/smoke_s2.sh     # S2 정차 주문 흐름 전체
bash scripts/smoke_gleo.sh   # Gleo가 보내는 ORDER_FOOD 인텐트 → 앱 주문 흐름
```

## 주의

- AAOS 에뮬레이터는 앱이 사용자 10으로 돈다. state 파일은 `/data/user/10/ai.cardeli.starter/files/demo_state.json`이고 `run-as ... --user 10`으로 읽는다(`demo_state`가 알아서 함).
- 수신기는 시연용이라 외부에 열려 있다(`exported=true`). 실제 배포 앱에서는 뺀다.
