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
import com.zebra.aidatacapturedemo.data.OCRFilterData
import com.zebra.aidatacapturedemo.data.OCRFilterType
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

class FeedbackUtils(val viewModel: AIDataCaptureDemoViewModel, context: Context) {
    init {
        vibrator = context.getSystemService(Vibrator::class.java)
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100) // STREAM_MUSIC for general media, 100 for max volume
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {

                    // 1. Remove all hypen "-" from the text
                    //2. After that, split the text with comma "," as separator and create a final list of Strings
                    val exactMatchStringList = matches[0].replace("-", "").split(",").map { it.trim() }

                    // Display recognizedText in a TextView or EditText
                    viewModel.updateOcrFilterData(OCRFilterData(ocrFilterType = OCRFilterType.EXACT_MATCH, exactMatchStringList = exactMatchStringList))
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

        fun startListening(){
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
