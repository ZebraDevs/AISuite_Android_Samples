package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.AdvancedFilterOption
import com.zebra.aidatacapturedemo.data.CharacterMatchFilterOption
import com.zebra.aidatacapturedemo.data.DetectionLevel
import com.zebra.aidatacapturedemo.data.OcrRegularFilterOption
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

/**
 * FeedbackUtils is a utility class that provides feedback mechanisms such as vibration, sound,
 * and speech recognition for the AI Data Capture Demo.
 * It initializes the necessary components for these feedback mechanisms and handles the speech
 * recognition results to update the OCR filter data in the ViewModel.
 */
class FeedbackUtils(val viewModel: AIDataCaptureDemoViewModel, context: Context) {
    init {
        vibrator = context.getSystemService(Vibrator::class.java)
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100) // STREAM_MUSIC for general media, 100 for max volume
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // Fetch the existing OcrFilterData
                    val defaultOcrFilterData = uiState.ocrFilterData

                    // For WORD Level filters:
                    // Remove all hypen "-" from the resultant text if, anything exists.
                    val exactMatchStringList =
                        if (defaultOcrFilterData.selectedCharacterMatchFilterData.detectionLevel == DetectionLevel.WORD) {
                            matches[0]?.let { it ->
                                it.replace("-", "")
                                    .replace(" ", ",")  // Note: During WORD_LEVEL selection, the user can input multiple word, hence separate them using commas.
                                    .split(",").map { it.trim() }
                            } ?: run {
                                listOf()
                            }
                        } else { // line level
                            matches[0]?.let {
                                listOf(it) // Note: During LINE_LEVEL selection, the user cannot input multiple lines.
                            } ?: run {
                                listOf()
                            }
                        }

                    // Now, assign SpeechRecognizer words for Exact Match
                    defaultOcrFilterData.selectedCharacterMatchFilterData.type = CharacterMatchFilterOption.EXACT_MATCH
                    defaultOcrFilterData.selectedCharacterMatchFilterData.exactMatchStringList = exactMatchStringList
                    defaultOcrFilterData.selectedRegularFilterOption = OcrRegularFilterOption.ADVANCED
                    if (!defaultOcrFilterData.selectedAdvancedFilterOptionList.contains(
                            AdvancedFilterOption.CHARACTER_MATCH)) {
                        defaultOcrFilterData.selectedAdvancedFilterOptionList.add(AdvancedFilterOption.CHARACTER_MATCH)
                    }
                    viewModel.updateOcrFilterData(ocrFilterData = defaultOcrFilterData)

                    micStatePressed = false
                    Log.d("SpeechRecognizer", "onResults: $exactMatchStringList" );
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognizer", "onReadyForSpeech");
            }
            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "onBeginningOfSpeech");
            }
            override fun onBufferReceived(p0: ByteArray?) {
                Log.d("SpeechRecognizer", "onBufferReceived");
            }
            override fun onEndOfSpeech() {
                Log.d("SpeechRecognizer", "onEndOfSpeech");
                micStatePressed = false
            }
            override fun onRmsChanged(p0: Float) {

            }
            override fun onError(error: Int) {
                Log.d("SpeechRecognizer", "onError : ${error}");
                micStatePressed = false
            }
            override fun onEvent(p0: Int, p1: Bundle?) {
                Log.d("SpeechRecognizer", "onEvent : ${p0}");
            }

            override fun onPartialResults(p0: Bundle?) {
                Log.d("SpeechRecognizer", "onPartialResults : ${p0}");
            }
            // ... other RecognitionListener methods
        })
    }
    companion object {
        private lateinit var uiState: AIDataCaptureDemoUiState
        var micStatePressed: Boolean = false
        private lateinit var vibrator: Vibrator
        private lateinit var toneGenerator : ToneGenerator
        private lateinit var speechRecognizer: SpeechRecognizer
        private val speechRecognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        fun vibrate() {
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        fun beep() {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150) // TONE_CDMA_PIP for a short "pip" sound, 150ms duration
        }

        fun startListening(uiState: AIDataCaptureDemoUiState) {
            Companion.uiState = uiState
            speechRecognizer.startListening(speechRecognitionIntent)
        }

        fun stopListening() {
            speechRecognizer.cancel()
        }

        fun deinitialize() {
            toneGenerator.release()
            speechRecognizer.destroy()
        }

    }
}
