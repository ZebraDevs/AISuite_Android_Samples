package com.zebra.ai.ppodguided.ai

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.application
import com.zebra.ai.ppodguided.repositories.OnPreferenceChangedListener
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_BLUR_BARCODES
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_BLUR_PEOPLE
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_BLUR_PETS
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_BLUR_RATIO
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_BLUR_TEXT
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_COMPLIANCE_BLUR
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_COMPLIANCE_CONTAINS_PEOPLE
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_COMPLIANCE_PACKAGE
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_COMPLIANCE_SURROUNDINGS
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_THRESHOLD_CONTAINS_PEOPLE
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_THRESHOLD_IMAGE_QUALITY
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_THRESHOLD_PACKAGE
import com.zebra.ai.ppodguided.repositories.PreferenceKeys.PREF_KEY_THRESHOLD_SURROUNDINGS
import com.zebra.ai.ppodguided.viewmodels.AppViewModel
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDK
import com.zebra.ai.vision.detector.AIVisionSDKLicenseException
import com.zebra.ai.vision.detector.Detector
import com.zebra.ai.vision.detector.ImageAttributeMetricValue
import com.zebra.ai.vision.detector.ImageAttributeMetricValue.ConditionType.GT_THAN
import com.zebra.ai.vision.detector.ImageAttributeMetricValue.ConditionType.LS_THAN
import com.zebra.ai.vision.detector.ImageAttributeMetricValue.ImageAttributeMetric.ImageQualityClear
import com.zebra.ai.vision.detector.ImageAttributeMetricValue.ImageAttributeMetric.ImageTagPackageVisible
import com.zebra.ai.vision.detector.ImageAttributeMetricValue.ImageAttributeMetric.ImageTagPeopleVisible
import com.zebra.ai.vision.detector.ImageAttributeMetricValue.ImageAttributeMetric.ImageTagSurroundVisible
import com.zebra.ai.vision.detector.ImageAttributeResult
import com.zebra.ai.vision.detector.ImageAttributesDetector
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.ImageTransformDetector
import com.zebra.ai.vision.detector.ImageTransformDetector.BlurRadius
import com.zebra.ai.vision.detector.ImageTransformDetector.TransformActionDescriptor
import com.zebra.ai.vision.detector.ImageTransformDetector.TransformationAction
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.entity.Entity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**************************************************************************************************/
data class ImageAttributes(
    var quality: Boolean = true,
    var qualityConfidence: Float = -1f,
    var packageVisible: Boolean = true,
    var packageVisibleConfidence: Float = -1f,
    var peopleNotVisible: Boolean = true,
    var peopleVisibleConfidence: Float = -1f,
    var surroundingsVisible: Boolean = true,
    var surroundingsVisibleConfidence: Float = -1f,
    var resultingBitmap: Bitmap? = null
)
/**************************************************************************************************/
class ImageProcessor(private val viewModel: AppViewModel) {
    private val TAG = "ImageProcessor"
    private val _executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val _callExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var _imageAttributesDetector: ImageAttributesDetector? = null
    private var _imageTransformDetector: ImageTransformDetector? = null
    private val  preferences = viewModel.preferences
    private var _totalDetectors = 0
    private var _totalTransformers = 0

    /**************************************************************************************************/
    private val mPreferenceListener = OnPreferenceChangedListener {
        _callExecutor.execute {
            viewModel.setImageProcessorLoaded(false)
            viewModel.setImageProcessorLoaded(initialiseDetector())
        }
    }
    /**************************************************************************************************/

    init{
        AIVisionSDK.getInstance(viewModel.application).init()
        preferences.addPreferenceListener(
            listOf(
                PREF_KEY_COMPLIANCE_BLUR,
                PREF_KEY_THRESHOLD_IMAGE_QUALITY,
                PREF_KEY_COMPLIANCE_PACKAGE,
                PREF_KEY_THRESHOLD_PACKAGE,
                PREF_KEY_COMPLIANCE_CONTAINS_PEOPLE,
                PREF_KEY_THRESHOLD_CONTAINS_PEOPLE,
                PREF_KEY_COMPLIANCE_SURROUNDINGS,
                PREF_KEY_THRESHOLD_SURROUNDINGS,
                PREF_KEY_BLUR_RATIO,
                PREF_KEY_BLUR_TEXT,
                PREF_KEY_BLUR_PETS,
                PREF_KEY_BLUR_PEOPLE,
                PREF_KEY_BLUR_BARCODES
            ),
            mPreferenceListener
        )

        // Init with initial preferences
        _callExecutor.execute {
            viewModel.setImageProcessorLoaded(initialiseDetector())
        }
    }
    /**************************************************************************************************/
    fun release() {
        preferences.removePreferenceListener(mPreferenceListener)
        _executor.shutdown()
        _callExecutor.shutdown()
        _imageAttributesDetector?.dispose()
        _imageTransformDetector?.dispose()
        _imageTransformDetector = null
        _imageAttributesDetector = null
    }
    /**************************************************************************************************/
    private fun initialiseDetector(): Boolean {
        return initImageAttributesDetector() && initImageTransformDetector()
    }
    /**************************************************************************************************/
    private fun initImageAttributesDetector(): Boolean {
        _imageAttributesDetector?.dispose()
        _imageAttributesDetector = null

        _totalDetectors = 0
        val qualityMetric: ImageAttributeMetricValue = initTag(ImageQualityClear,
            GT_THAN,
            PREF_KEY_COMPLIANCE_BLUR,
            PREF_KEY_THRESHOLD_IMAGE_QUALITY
        )
        val packageDetector: ImageAttributeMetricValue = initTag( ImageTagPackageVisible,
            GT_THAN,
            PREF_KEY_COMPLIANCE_PACKAGE,
            PREF_KEY_THRESHOLD_PACKAGE
        )
        val personDetector: ImageAttributeMetricValue = initTag( ImageTagPeopleVisible,
            LS_THAN,
            PREF_KEY_COMPLIANCE_CONTAINS_PEOPLE,
            PREF_KEY_THRESHOLD_CONTAINS_PEOPLE
        )
        val surroundingsDetector: ImageAttributeMetricValue = initTag(ImageTagSurroundVisible,
            GT_THAN,
            PREF_KEY_COMPLIANCE_SURROUNDINGS,
            PREF_KEY_THRESHOLD_SURROUNDINGS
        )

        // If we are not detecting anything, Then break out
        if (_totalDetectors == 0) return true

        // Add Detector Metrics.
        val metrics: MutableList<ImageAttributeMetricValue> = ArrayList()
        metrics.add(qualityMetric)
        metrics.add(packageDetector)
        metrics.add(personDetector)
        metrics.add(surroundingsDetector)

        val settings = ImageAttributesDetector.Settings()
        settings.configureImageAttributeMetrics(metrics)
        Log.i(TAG, "Initialising Detector : $settings")
        try {
            _imageAttributesDetector = ImageAttributesDetector.getImageAttributesDetector(settings, _executor).get()
        }catch (e : Exception) {
            if (hasCause(e,AIVisionSDKLicenseException::class.java)) viewModel.setImageProcessLicenseValid(false)
            Log.e(TAG, "Error Initialising Detector : $e")
        }
        return _imageAttributesDetector != null
    }
    /**************************************************************************************************/
    private fun initImageTransformDetector(): Boolean {
        _imageTransformDetector?.dispose()
        _imageTransformDetector = null

        val actions: MutableList<TransformationAction> = ArrayList()
        _totalTransformers = 0
        if (preferences[PREF_KEY_BLUR_BARCODES] as Boolean) {
            actions.add(TransformationAction.LocalizeAndBlurBarcode)
            _totalTransformers++
        }
        if (preferences[PREF_KEY_BLUR_TEXT] as Boolean) {
            actions.add(TransformationAction.LocalizeAndBlurText)
            _totalTransformers++
        }
        if (preferences[PREF_KEY_BLUR_PEOPLE] as Boolean) {
            actions.add(TransformationAction.LocalizeAndBlurPeople)
            _totalTransformers++
        }
        if (preferences[PREF_KEY_BLUR_PETS] as Boolean) {
            actions.add(TransformationAction.LocalizeAndBlurPets)
            _totalTransformers++
        }

        // Break out if we aren't transforming anything
        if (_totalTransformers == 0) return true

        val blurRatio = when (preferences[PREF_KEY_BLUR_RATIO] as String) {
            "1" -> BlurRadius.High
            "2" -> BlurRadius.Medium
            else -> BlurRadius.Low
        }

        val descriptor = TransformActionDescriptor.Builder()
            .setActions(actions)
            .setBlurRadius(blurRatio)
            .build()

        val settings = ImageTransformDetector.Settings()
        val options = InferencerOptions()
        options.runtimeProcessorOrder = arrayOf(InferencerOptions.DSP)
        settings.configureInferencerOptions(options)
        settings.configureTransformationActions(descriptor)

        try {
            _imageTransformDetector = ImageTransformDetector.getImageTransformDetector(settings, _executor).get()
        }catch (e : Exception) {
            if (hasCause(e,AIVisionSDKLicenseException::class.java)) viewModel.setImageProcessLicenseValid(false)
            Log.e(TAG, "Error Initialising Transformer : $e")
        }
        return _imageTransformDetector != null
    }
    /**************************************************************************************************/
    private fun initTag(metric: ImageAttributeMetricValue.ImageAttributeMetric,
                        condition: ImageAttributeMetricValue.ConditionType,
                        enabledPreference: String,
                        valuePreference: String ): ImageAttributeMetricValue {
        val metricObj = ImageAttributeMetricValue.Builder(metric)
            .setEnable(true)
            .build()

        // If no value set, use the default from the model
        val value = if (preferences[valuePreference] == null) metricObj.value else preferences[valuePreference] as Float
        val enabled = preferences[enabledPreference] as Boolean
        preferences[valuePreference] = value
        if (enabled) _totalDetectors++

        Log.d(TAG, "Metric: " + metricObj + " -> " + metricObj.value)
        return ImageAttributeMetricValue.Builder(metric)
            .setValue(value)
            .setEnable(enabled)
            .setCondition(condition)
            .build()
    }
    /**************************************************************************************************/
    fun initEntityTrackerAnalyzer(): EntityTrackerAnalyzer? {
        val detectors: MutableList<Detector<out List<Entity>>> = mutableListOf()
        if (_imageAttributesDetector == null) return null
        detectors.add( _imageAttributesDetector as Detector<out List<Entity>>)

        return EntityTrackerAnalyzer(
            detectors,
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            _executor,
            ::handleEntities)
    }
    /**************************************************************************************************/
    fun handleEntities(result:EntityTrackerAnalyzer.Result) {
        val attributeEntities = result.getValue(_imageAttributesDetector as Detector<out List<Entity>>)
        var isCompliant = true
        attributeEntities?.forEach { imageAttribute ->
            if (imageAttribute is ImageAttributeResult) {
                isCompliant = (isCompliant && imageAttribute.isCompliant)
            }
        }
        viewModel.setAnalysisResult(isCompliant)
    }
    /**************************************************************************************************/
    fun processImage(inputBitmap: Bitmap?, resultCallback: (ImageAttributes?) -> Unit) {
        if (inputBitmap == null) {
            resultCallback(null)
            return
        }

        _callExecutor.execute {
            try {
                val imageData = ImageData.fromBitmap(inputBitmap,0)
                val imageAttributes = ImageAttributes(resultingBitmap = inputBitmap)

                // Sanity Check
                if ((_totalDetectors != 0 && _imageAttributesDetector == null) ||
                    (_totalTransformers != 0 && _imageTransformDetector == null)) {
                    Log.e(TAG, "Detector not initialised")
                    resultCallback(null)
                    return@execute
                }

                // Process the Image Attributes
                _imageAttributesDetector?.let { imageAttributesDetector ->
                    val attributeEntities = imageAttributesDetector.process(imageData, _executor).get()
                    attributeEntities?.forEach { imageAttribute ->
                        val metricName = imageAttribute.metric.toString()
                        val isCompliant = imageAttribute.isCompliant
                        val value = imageAttribute.value
                        Log.d(TAG, "Metric: $metricName | Compliant: $isCompliant | Value: $value")

                        when (imageAttribute.metric) {
                            ImageQualityClear -> {
                                imageAttributes.quality = isCompliant
                                imageAttributes.qualityConfidence = value as Float
                            }
                            ImageTagPackageVisible -> {
                                imageAttributes.packageVisible = isCompliant
                                imageAttributes.packageVisibleConfidence = value as Float
                            }
                            ImageTagPeopleVisible -> {
                                imageAttributes.peopleNotVisible = isCompliant
                                imageAttributes.peopleVisibleConfidence = value as Float
                            }
                            ImageTagSurroundVisible -> {
                                imageAttributes.surroundingsVisible = isCompliant
                                imageAttributes.surroundingsVisibleConfidence = value as Float
                            }
                        }
                    }
                }

                // Image Transformation
                _imageTransformDetector?.let { imageTransformDetector ->
                    val transformationResult = imageTransformDetector.process(imageData).get()
                    if (transformationResult.bitmapImage != null) {
                        imageAttributes.resultingBitmap = transformationResult.bitmapImage
                    }
                }
                resultCallback(imageAttributes)
            } catch (e: Exception) {
                Log.e(TAG, "Image processing failed", e)
                resultCallback(null)
            }
        }
    }

    /**************************************************************************************************/
    fun hasCause(throwable: Throwable?, causeClass: Class<out Throwable>): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (causeClass.isInstance(current)) {
                return true
            }
            current = current.cause
        }
        return false
    }

}