package com.zebra.ai.barcodefinder.application.data.services

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.application.domain.services.feedback.FeedbackService
import com.zebra.ai.barcodefinder.sdkcoordinator.model.FeedbackType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Concrete implementation of FeedbackService.
 * Handles Android Vibrator and SoundPool.
 */
class SystemFeedbackService(context: Context) : FeedbackService {

    private val TAG = "FeedbackEngine"

    private data class FeedbackRequest(val type: FeedbackType)

    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val soundPool: SoundPool
    private val soundId: Int
    private val feedbackQueue = ConcurrentLinkedQueue<FeedbackRequest>()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled exception in FeedbackManager scope", throwable)
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + coroutineExceptionHandler)
    private var processingJob: Job? = null
    private val jobMutex = Mutex()
    private val isPaused = AtomicBoolean(true)

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(MAX_SOUND_STREAMS).setAudioAttributes(audioAttributes).build()
        soundId = soundPool.load(context, R.raw.beep_short, 1)
        Log.d(TAG, "FeedbackEngine initialized.")
    }

    override fun pause() {
        Log.d(TAG, "Pausing feedback.")
        isPaused.set(true)
        feedbackQueue.clear()

        scope.launch {
            jobMutex.withLock {
                try {
                    processingJob?.cancelAndJoin()
                } catch (e: CancellationException) {
                    Log.d(TAG, "Job cancelled.")
                } finally {
                    processingJob = null
                }
            }
        }
        try {
            soundPool.autoPause()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "SoundPool error on pause.", e)
        }
    }

    override fun resume() {
        Log.d(TAG, "Resuming feedback.")
        isPaused.set(false)
    }

    override fun triggerFeedback(type: FeedbackType) {
        if (isPaused.get() || !scope.isActive) return

        if (type.audio || type.haptics) {
            feedbackQueue.add(FeedbackRequest(type))
            scope.launch {
                processFeedbackQueue()
            }
        }
    }

    override fun release() {
        if (!scope.isActive) return
        Log.d(TAG, "Releasing resources.")
        scope.cancel("FeedbackManager released.")
        feedbackQueue.clear()
        try {
            soundPool.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing SoundPool.", e)
        }
    }

    private suspend fun processFeedbackQueue() {
        jobMutex.withLock {
            if (processingJob?.isActive == true) return@withLock
            processingJob = scope.launch {
                while (isActive && !isPaused.get()) {
                    val request = feedbackQueue.poll() ?: break
                    executeSingleFeedback(request)
                    delay(PULSE_INTERVAL_MS)
                }
                processingJob = null
            }
        }
    }

    private fun executeSingleFeedback(request: FeedbackRequest) {
        if (!scope.isActive) return
        if (request.type.haptics) {
            handleSinglePulseVibration()
        }
        if (request.type.audio) {
            handleSingleBeepSound()
        }
    }

    private fun handleSingleBeepSound() {
        if (!scope.isActive || soundId == 0) return
        try {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound", e)
        }
    }

    private fun handleSinglePulseVibration() {
        if (!scope.isActive || vibrator == null || !vibrator.hasVibrator()) return
        try {
            vibrator.vibrate(PULSE_VIBRATION_EFFECT)
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating", e)
        }
    }

    private companion object {
        const val MAX_SOUND_STREAMS = 5
        const val PULSE_VIBRATION_MS = 40L
        const val PULSE_INTERVAL_MS = 75L
        val PULSE_VIBRATION_EFFECT: VibrationEffect = VibrationEffect.createOneShot(PULSE_VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
    }
}
