package ai.cardeli.starter.check

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import ai.pleos.playground.llm.LLM
import ai.pleos.playground.llm.constant.Mode
import ai.pleos.playground.llm.listener.ResultListener

/**
 * LLM SDK 시험: 에뮬레이터에서 LLM SDK가 실제로 응답하는지 본다.
 * 실행: adb shell am start -n ai.cardeli.starter/.check.LlmCheckActivity [--es mode SERVER]
 * 로그: adb logcat -s LlmCheck
 */
class LlmCheckActivity : Activity() {
    private val tag = "LlmCheck"
    private val main = Handler(Looper.getMainLooper())
    private lateinit var out: TextView
    private var llm: LLM? = null

    private val prompt = "다음 주문 문장에서 메뉴와 수량만 JSON으로 뽑아줘. 문장: \"응, 참치김밥이랑 어묵국 하나\""

    private fun log(msg: String) {
        Log.i(tag, msg)
        main.post { out.append(msg + "\n") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        out = TextView(this).apply { textSize = 22f; setPadding(40, 40, 40, 40) }
        setContentView(out)

        val mode = if (intent.getStringExtra("mode") == "SERVER") Mode.SERVER else Mode.ON_DEVICE
        log("모드: $mode")
        try {
            val l = LLM(this, mode)
            llm = l
            l.initialize()
            log("initialize 호출함")
        } catch (e: Throwable) {
            log("initialize 예외: ${e.javaClass.simpleName}: ${e.message}")
            return
        }
        // 서비스 연결을 기다렸다가 호출 (initialize는 결과 콜백이 없음)
        main.postDelayed({ ask() }, 4000)
    }

    private fun ask() {
        val l = llm ?: return
        try { log("getPromptsContents: ${l.getPromptsContents()}") } catch (e: Throwable) { log("getPromptsContents 예외: ${e.message}") }
        log("질문: $prompt")
        val started = System.currentTimeMillis()
        try {
            l.generateContent(prompt, object : ResultListener {
                override fun onResponse(response: String, isFinal: Boolean) {
                    log("응답(${System.currentTimeMillis() - started}ms, final=$isFinal): $response")
                }
                override fun onError(error: String) {
                    log("오류(${System.currentTimeMillis() - started}ms): $error")
                }
            })
        } catch (e: Throwable) {
            log("generateContent 예외: ${e.javaClass.simpleName}: ${e.message}")
        }
        main.postDelayed({ log("-- 30초 경과 --") }, 30000)
    }

    override fun onDestroy() {
        try { llm?.release() } catch (_: Throwable) {}
        super.onDestroy()
    }
}
