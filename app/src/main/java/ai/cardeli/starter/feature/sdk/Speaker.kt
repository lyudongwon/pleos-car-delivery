package ai.cardeli.starter.feature.sdk

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 음성 응답 (F4). Pleos TextToSpeech ON_DEVICE 모드(등록 불필요).
 *
 * 에뮬레이터에서 재생 완료 콜백 onDone이 오지 않으므로(docs/sdk_check.md 0단계), 끝나는 시점은
 * 글자 수로 추정한다. onDone이 먼저 오면 그때 끝낸다. 응답끼리, 응답과 음성 인식이 겹치지 않게 차례로 말한다.
 */
class Speaker(context: Context) {

    private val tts = TextToSpeech(context, Mode.ON_DEVICE)
    private val ready = CompletableDeferred<Boolean>()
    private val queue = Mutex()
    private var done: CompletableDeferred<Unit>? = null

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    init {
        try {
            tts.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    tts.addEventListener(events)
                    ready.complete(true)
                }
                override fun onFailure(e: ErrorType) {
                    Log.w(TAG, "TTS 초기화 실패 $e")
                    ready.complete(false)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "TTS 초기화 예외", e)
            ready.complete(false)
        }
    }

    private val events = object : EventListener {
        override fun onReady() {}
        override fun onStart() {
            Log.i(TAG, "onStart")
        }
        override fun onDone() {
            Log.i(TAG, "onDone")
            done?.complete(Unit)
        }
        override fun onStop() {
            done?.complete(Unit)
        }
        override fun onError(e: ErrorType) {
            Log.w(TAG, "onError $e")
            done?.complete(Unit)
        }
        override fun onUpdatedRms(rms: Double) {}
    }

    /** 말하고, 끝날 때까지(또는 추정 시간이 지날 때까지) 기다린다. 초기화에 실패하면 기다리기만 한다 */
    suspend fun say(text: String) = queue.withLock {
        val ok = withTimeoutOrNull(INIT_TIMEOUT_MS) { ready.await() } ?: false
        val estimate = SpeechTiming.estimateMs(text)
        Log.i(TAG, "say '$text' (추정 ${estimate}ms, tts=${if (ok) "on" else "off"})")
        _speaking.value = true
        try {
            if (ok) {
                val d = CompletableDeferred<Unit>()
                done = d
                tts.speak(text, false)
                withTimeoutOrNull(estimate) { d.await() }
            } else {
                delay(estimate)
            }
        } finally {
            done = null
            _speaking.value = false
            Log.i(TAG, "say 끝")
        }
    }

    fun release() {
        try { tts.release() } catch (_: Exception) {}
    }

    private companion object {
        const val TAG = "Speaker"
        const val INIT_TIMEOUT_MS = 3_000L
    }
}

/**
 * 한국어 합성 음성 재생 시간 추정. 공백·문장부호를 뺀 글자당 0.22초 + 합성·시작 지연 0.9초.
 * 에뮬레이터 실측(11/11): 19자·24자 두 문장이 이어서 약 11초 재생됐다.
 */
object SpeechTiming {
    private const val START_MS = 900L
    private const val PER_CHAR_MS = 220L

    fun estimateMs(text: String): Long {
        val chars = text.count { it.isLetterOrDigit() }
        return START_MS + PER_CHAR_MS * chars
    }
}
