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
import com.zebra.aidatacapturedemo.data.CharacterMatchData
import com.zebra.aidatacapturedemo.data.CharacterMatchFilterOption
import com.zebra.aidatacapturedemo.data.DetectionLevel
import com.zebra.aidatacapturedemo.data.FilterField
import com.zebra.aidatacapturedemo.model.FilterUtils
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
        toneGenerator = ToneGenerator(
            AudioManager.STREAM_MUSIC,
            100
        ) // STREAM_MUSIC for general media, 100 for max volume
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = (matches[0] ?: "").trim()
                    if (spokenText.isEmpty()) {
                        Log.d("SpeechRecognizer", "onResults: empty transcript, ignoring")
                        micStatePressed = false
                        return
                    }
                    // Read live ViewModel state at the moment we act, not the snapshot
                    // captured when the mic button was pressed — speech recognition can
                    // take a couple of seconds, during which the filter state could change.
                    val liveUiState = viewModel.uiState.value
                    val selectedIds = liveUiState.ocrFilterData.selectedFieldIds.toSet()

                    if (selectedIds.isEmpty()) {
                        // No field has been created/selected yet (e.g. voice search used
                        // standalone, without visiting OCR Field Configuration first).
                        // Auto-create a field for the spoken filter, the same way the manual
                        // "New Field" dialog does, so voice search works on its own. The field
                        // keeps a fixed, stable name rather than the spoken text itself, since
                        // repeating voice search replaces this same field's filter value and a
                        // name copied from the first utterance would go stale after that.
                        val fieldId = FilterUtils.generateFieldId(VOICE_SEARCH_FIELD_TITLE)
                        viewModel.upsertOcrField(
                            FilterField(
                                id = fieldId,
                                title = VOICE_SEARCH_FIELD_TITLE,
                                isDefault = false
                            )
                        )
                        val matchStringList =
                            buildMatchTokens(spokenText, DetectionLevel.WORD)
                        val newConfig = FilterUtils.getOrCreateOcrFieldConfig(
                            viewModel.uiState.value,
                            fieldId,
                            VOICE_SEARCH_FIELD_TITLE
                        )
                        // Brand-new field, no prior user-configured match type to respect —
                        // default to CONTAINS since the spoken word can appear anywhere in
                        // the detected OCR text.
                        val existingCm = newConfig.characterMatchData ?: CharacterMatchData()
                        val config = newConfig.copy(
                            characterMatchEnabled = true,
                            characterMatchData = existingCm.copy(
                                type = CharacterMatchFilterOption.CONTAINS,
                                containsStringList = matchStringList
                            )
                        )
                        viewModel.saveOcrFieldConfig(config)
                        viewModel.updateSelectedOcrFieldIds(listOf(fieldId))
                        Log.d("SpeechRecognizer", "onResults: created field '$spokenText' with $matchStringList")
                    } else {
                        // Use detection level from the first selected field's existing config
                        val firstConfig = liveUiState.ocrFilterData.fieldFilterConfigs
                            .firstOrNull { it.fieldId in selectedIds }
                        val detectionLevel =
                            firstConfig?.characterMatchData?.detectionLevel
                                ?: DetectionLevel.WORD

                        val matchStringList =
                            buildMatchTokens(spokenText, detectionLevel)

                        // Apply spoken result to ALL selected fields, creating a config for
                        // any selected field that doesn't have one yet. Each field's existing
                        // match type (STARTS_WITH / CONTAINS / EXACT_MATCH) is preserved rather
                        // than overridden — a user who deliberately chose EXACT_MATCH for
                        // precision shouldn't have voice search silently loosen it to CONTAINS.
                        // Each field is saved individually via saveOcrFieldConfig (the same
                        // per-field upsert the manual filter screens use) instead of replacing
                        // the whole ocrFilterData object, so this can't clobber unrelated
                        // ViewModel state. The config for each field is (re)fetched from live
                        // state right before saving, so a config edited elsewhere moments
                        // earlier isn't reverted.
                        val fieldTitlesById =
                            liveUiState.ocrFilterData.fieldList.associate { it.id to it.title }
                        selectedIds.forEach { id ->
                            val cfg = FilterUtils.getOrCreateOcrFieldConfig(
                                viewModel.uiState.value,
                                id,
                                fieldTitlesById[id] ?: id
                            )
                            val existingCm = cfg.characterMatchData ?: CharacterMatchData()
                            viewModel.saveOcrFieldConfig(
                                cfg.copy(
                                    characterMatchEnabled = true,
                                    characterMatchData = applySpokenTokens(existingCm, matchStringList)
                                )
                            )
                        }
                        Log.d("SpeechRecognizer", "onResults: $matchStringList")
                    }

                    micStatePressed = false
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
        private const val VOICE_SEARCH_FIELD_TITLE = "Voice Filter"
        var micStatePressed: Boolean = false
        private lateinit var vibrator: Vibrator
        private lateinit var toneGenerator: ToneGenerator
        private lateinit var speechRecognizer: SpeechRecognizer
        private val speechRecognitionIntent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            }

        fun vibrate() {
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        150,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
        }

        fun beep() {
            toneGenerator.startTone(
                ToneGenerator.TONE_CDMA_PIP,
                150
            ) // TONE_CDMA_PIP for a short "pip" sound, 150ms duration
        }

        fun startListening() {
            speechRecognizer.startListening(speechRecognitionIntent)
        }

        fun stopListening() {
            speechRecognizer.cancel()
        }

        fun deinitialize() {
            if (::toneGenerator.isInitialized) {
                toneGenerator.release()
            }
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.destroy()
            }
        }

        private fun buildMatchTokens(
            spokenText: String,
            detectionLevel: DetectionLevel
        ): List<String> {
            return if (detectionLevel == DetectionLevel.WORD) {
                spokenText.replace("-", "").replace(" ", ",")
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                listOf(spokenText)
            }
        }

        /**
         * Writes the spoken tokens into whichever match-type list the field is already
         * configured to use, leaving that type unchanged.
         */
        private fun applySpokenTokens(
            existingCm: CharacterMatchData,
            matchTokens: List<String>
        ): CharacterMatchData {
            return when (existingCm.type) {
                CharacterMatchFilterOption.STARTS_WITH ->
                    existingCm.copy(startsWithStringList = matchTokens)

                CharacterMatchFilterOption.CONTAINS ->
                    existingCm.copy(containsStringList = matchTokens)

                CharacterMatchFilterOption.EXACT_MATCH ->
                    existingCm.copy(exactMatchStringList = matchTokens)
            }
        }
    }
}
