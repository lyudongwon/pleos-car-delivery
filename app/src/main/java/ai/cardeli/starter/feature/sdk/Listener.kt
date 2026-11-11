package ai.cardeli.starter.feature.sdk

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * 음성 인식 (F2). Pleos SpeechToText ON_DEVICE 모드(등록 불필요).
 *
 * 에뮬레이터에는 마이크 입력을 넣을 수 없어서, 녹음해 둔 대사(assets/voice/<이름>.pcm, 16kHz·16bit·mono)를
 * 실제 말하는 속도로 sendAudio에 넣는다. sendAudio는 ON_DEVICE에서만 된다.
 * 마이크 인식(request)은 쓰지 않는다: 강제 종료되면 음성 서비스에 세션이 남아 TTS까지 막는다(docs/sdk_check.md F4).
 */
class Listener(private val context: Context) {

    private val stt = SpeechToText(context, Mode.ON_DEVICE)
    private val ready = CompletableDeferred<Boolean>()
    private val busy = Mutex()

    private var onPartial: ((String) -> Unit)? = null
    private var result: CompletableDeferred<String?>? = null

    init {
        try {
            stt.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    stt.setLocale(Locale.KOREA)
                    stt.addListener(listener)
                    ready.complete(true)
                }
                override fun onFailure(e: ErrorType) {
                    Log.w(TAG, "STT 초기화 실패 $e")
                    ready.complete(false)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "STT 초기화 예외", e)
            ready.complete(false)
        }
    }

    private val listener = object : ResultListener {
        override fun onUpdated(text: String, isFinal: Boolean) {
            Log.i(TAG, "onUpdated '$text' final=$isFinal")
            if (isFinal) result?.complete(text) else onPartial?.invoke(text)
        }
        override fun onError(e: ErrorType) {
            Log.w(TAG, "onError $e")
            result?.complete(null)
        }
        override fun onUpdatedEpdData(on: Long, off: Long) {}
        override fun onStartedRecognition() {}
        override fun onEndedRecognition() {}
        override fun onReady() {}
        override fun onUpdatedRms(rms: Double) {}
    }

    /** assets/voice/<name>.pcm 을 인식해 최종 문장을 돌려준다. 실패하거나 시간이 지나면 null */
    suspend fun recognizeAsset(name: String, partial: (String) -> Unit = {}): String? {
        val pcm = try {
            withContext(Dispatchers.IO) { context.assets.open("voice/$name.pcm").use { it.readBytes() } }
        } catch (e: Exception) {
            Log.w(TAG, "대사 파일 없음: voice/$name.pcm")
            return null
        }
        return recognize(pcm, partial)
    }

    suspend fun recognize(pcm: ByteArray, partial: (String) -> Unit = {}): String? = busy.withLock {
        val ok = withTimeoutOrNull(INIT_TIMEOUT_MS) { ready.await() } ?: false
        if (!ok) return@withLock null
        val r = CompletableDeferred<String?>()
        result = r
        onPartial = partial
        try {
            withContext(Dispatchers.IO) {
                // 0.1초(3,200바이트)씩 실제 속도로 넣는다
                for (start in pcm.indices step CHUNK) {
                    stt.sendAudio(pcm.copyOfRange(start, minOf(start + CHUNK, pcm.size)))
                    delay(CHUNK_MS)
                }
                stt.completeAudioSend()
            }
            withTimeoutOrNull(FINAL_TIMEOUT_MS) { r.await() }.also { Log.i(TAG, "최종 '$it'") }
        } finally {
            result = null
            onPartial = null
        }
    }

    fun release() {
        try { stt.release() } catch (_: Exception) {}
    }

    private companion object {
        const val TAG = "Listener"
        const val CHUNK = 3_200
        const val CHUNK_MS = 100L
        const val INIT_TIMEOUT_MS = 5_000L
        const val FINAL_TIMEOUT_MS = 8_000L
    }
}
