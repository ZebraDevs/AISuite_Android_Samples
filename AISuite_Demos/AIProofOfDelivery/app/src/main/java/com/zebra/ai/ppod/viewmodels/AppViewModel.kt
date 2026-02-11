package com.zebra.ai.ppod.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.TAG_DATETIME
import androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED
import androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL
import androidx.exifinterface.media.ExifInterface.TAG_MAKE
import androidx.exifinterface.media.ExifInterface.TAG_MODEL
import androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME
import androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_DIGITIZED
import androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL
import androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE
import androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zebra.ai.ppod.R
import com.zebra.ai.ppod.ai.ImageProcessor
import com.zebra.ai.ppod.repositories.PreferenceKeys.PREF_KEY_CAPTURE_RESOLUTION
import com.zebra.ai.ppod.repositories.PreferenceKeys.PREF_KEY_EULA_ACCEPTED
import com.zebra.ai.ppod.repositories.PreferenceKeys.PREF_KEY_SAVE_RESOLUTION
import com.zebra.ai.ppod.repositories.ZPreferences
import com.zebra.ai.ppod.ui.components.mainScreen.BorderState
import com.zebra.ai.ppod.ui.components.mainScreen.ReportType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**************************************************************************************************/
sealed class UiEvent {
    object FinishApp : UiEvent()
}

/**************************************************************************************************/
data class ReportStatus(
    val reportType: ReportType? = null,
    val header: String? = null,
    val issues: List<String>? = null,
    val onRetake: (() -> Unit)? = null,
    val onContinue: (() -> Unit)? = null,
)

/**************************************************************************************************/
class AppViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _uiEvents = Channel<UiEvent>()
    private val _loaded = MutableStateFlow(false)
    private val _licenseValid = MutableStateFlow(true)
    private val _preferences = ZPreferences(application, R.xml.app_preferences)
    private var _imageProcessor: ImageProcessor
    private val _reportStatus = MutableStateFlow<ReportStatus?>(null)
    private val _borderState = MutableStateFlow(BorderState.IDLE)
    private val _resultingImage = MutableStateFlow<Bitmap?>(null)
    private val _countDownTimer = MutableStateFlow(0)
    private val _torchEnabled = MutableStateFlow(false)
    private val _zoomState = MutableStateFlow(0f)
    private val _settingsEnabled = MutableStateFlow(false)
    private val _shutterClick = MediaActionSound()
    private var _outputImage: Bitmap? = null
    private val _rotation = MutableStateFlow(0)
    private val sensorManager: SensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // ========================================
    val uiEvents = _uiEvents.receiveAsFlow()
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    val licenseValid: StateFlow<Boolean> = _licenseValid.asStateFlow()
    val reportStatus: StateFlow<ReportStatus?> = _reportStatus.asStateFlow()
    val borderState: StateFlow<BorderState> = _borderState.asStateFlow()
    val resultingImage: StateFlow<Bitmap?> = _resultingImage.asStateFlow()
    val countDownTimer: StateFlow<Int> = _countDownTimer.asStateFlow()
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()
    val zoomState: StateFlow<Float> = _zoomState.asStateFlow()
    val settingsEnabled: StateFlow<Boolean> = _settingsEnabled.asStateFlow()
    val preferences = _preferences
    var contentUri: Uri? = null
    var dataSaved = false

    /**************************************************************************************************/
    private val sensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            val x: Float = event.values[0]
            val y: Float = event.values[1]
            var deviceRotation = 0

            // Determine orientation
            deviceRotation = if (abs(x) > abs(y)) {
                if (x < 0) {
                    90
                } else {
                    270
                }
            } else if (y < 0) {
                180
            } else {
                0
            }
            _rotation.value = deviceRotation
        }
    }

    /**************************************************************************************************/
    companion object {
        private val SIZES = mapOf(
            "1" to Size(320, 240),
            "2" to Size(640, 480),
            "3" to Size(1280, 960),
            "4" to Size(2560, 1920)
        )
    }

    /**************************************************************************************************/
    init {
        preferences.registerConfigReceiver(application)
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(sensorEventListener, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL)
        _imageProcessor = ImageProcessor(this)
    }

    /**************************************************************************************************/
    override fun onCleared() {
        _shutterClick.release()
        _imageProcessor.release()
        preferences.unregisterConfigReceiver(application)
        sensorManager.unregisterListener(sensorEventListener)
        super.onCleared()
    }

    /**************************************************************************************************/
    fun setImageProcessorLoaded(loaded: Boolean, licenseValid: Boolean) {
        _loaded.value = loaded
        _licenseValid.value = licenseValid
    }

    /**************************************************************************************************/
    fun processImage(imageProxy: ImageProxy) {
        val deviceRotation = _rotation.value
        val imageRotation = imageProxy.imageInfo.rotationDegrees + deviceRotation
        val inputImage = imageProxy.toBitmap().rotate(imageRotation)
        _resultingImage.value = inputImage.rotate(-deviceRotation)
        imageProxy.close()

        viewModelScope.launch {
            _shutterClick.play(MediaActionSound.SHUTTER_CLICK)

            // Is Processor Initialised
            if (!_loaded.value) {
                _reportStatus.value = ReportStatus(
                    reportType = ReportType.INFO,
                    header = application.getString(R.string.waiting)
                )

                val result = withTimeoutOrNull(5_000L) { _loaded.first { it } }
                if (result == null) {
                    _reportStatus.value = ReportStatus(
                        reportType = ReportType.ERROR,
                        header = application.getString(R.string.init_failed)
                    )
                    _borderState.value = BorderState.BAD
                    return@launch
                }
            }

            // Process the image
            _reportStatus.value = ReportStatus(
                reportType = ReportType.INFO,
                header = application.getString(R.string.processing)
            )

            // Process the Image
            _imageProcessor.processImage(inputImage) { imageAttributes ->
                val issuesList: MutableList<String> = mutableListOf()
                if (imageAttributes == null) issuesList.add(application.getString(R.string.init_failed))

                // Populate Issues
                imageAttributes?.let {
                    _outputImage = it.resultingBitmap
                    _resultingImage.value = _outputImage?.rotate(-deviceRotation)
                    if (!it.quality) issuesList.add(application.getString(R.string.error_blurred))
                    if (!it.packageVisible) issuesList.add(application.getString(R.string.error_no_parcel))
                    if (!it.surroundingsVisible) issuesList.add(application.getString(R.string.error_no_environment))
                    if (!it.peopleNotVisible) issuesList.add(application.getString(R.string.error_person))
                }

                // Report any Issues
                if (issuesList.isNotEmpty()) {
                    _reportStatus.value = _reportStatus.value?.copy(
                        reportType = ReportType.ERROR,
                        header = application.getString(R.string.review_issues),
                        issues = issuesList.toList(),
                        onRetake = { clearResults() },
                        onContinue = {
                            saveImage(application.getString(R.string.review_issues) + "\n" + issuesList.joinToString("\n"))
                            clearResults()
                            if (contentUri != null) closeApplication()
                        }
                    )
                    _borderState.value = BorderState.BAD
                } else {
                    // Good Status
                    _reportStatus.value = _reportStatus.value?.copy(
                        reportType = ReportType.SUCCESS,
                        header = application.getString(R.string.successfully_captured),
                    )
                    _borderState.value = BorderState.GOOD
                    _countDownTimer.value = 3
                    saveImage(application.getString(R.string.successfully_captured))
                }
            }
        }
    }

    /**************************************************************************************************/
    fun clearResults() {
        _borderState.value = BorderState.IDLE
        _reportStatus.value = null
        _resultingImage.value = null
        _outputImage = null
    }

    /**************************************************************************************************/
    fun closeApplication() {
        viewModelScope.launch {
            _uiEvents.send(UiEvent.FinishApp)
        }
    }

    /**************************************************************************************************/
    fun countDownTick() {
        _countDownTimer.value -= 1
        if (_countDownTimer.value == 0) {
            if (contentUri != null) closeApplication()
            clearResults()
        }
    }

    /**************************************************************************************************/
    fun setSettingDisplay(value: Boolean) {
        _settingsEnabled.value = value
    }

    /**************************************************************************************************/
    fun toggleTorchEnabled() {
        _torchEnabled.value = !_torchEnabled.value
    }

    /**************************************************************************************************/
    fun performZoomClick() {
        _zoomState.value += 0.25f
        if (_zoomState.value > 1f) _zoomState.value = 0f
    }

    /**************************************************************************************************/
    fun getCaptureSize(): Size {
        return SIZES[preferences[PREF_KEY_CAPTURE_RESOLUTION]] ?: SIZES["1"]!!
    }

    /**************************************************************************************************/
    fun getSavedSize(): Size {
        return SIZES[preferences[PREF_KEY_SAVE_RESOLUTION]] ?: SIZES["1"]!!
    }

    /**************************************************************************************************/
    fun saveImage(comment: String?): Boolean {
        if (_outputImage == null) return false

        val landscape = _outputImage!!.width > _outputImage!!.height
        val width = if (landscape) getSavedSize().width else getSavedSize().height
        val height = if (landscape) getSavedSize().height else getSavedSize().width
        val scaledBitmap = _outputImage!!.scale(width, height, false)

        val filename: String = generateImageFilename()
        val file = File(application.filesDir, filename)

        //Write the file
        try {
            FileOutputStream(file).use { output ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
        } catch (_: Exception) {
            return false
        }

        //Save EXIF data.
        val dateTaken = System.currentTimeMillis()
        val exif = ExifInterface(file)
        val dateTime = Date(dateTaken)
        val dateTimeStr = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(dateTime)
        val offsetStr = SimpleDateFormat("ZZ", Locale.getDefault()).format(dateTime)
        exif.setAttribute(TAG_DATETIME, dateTimeStr)
        exif.setAttribute(TAG_DATETIME_DIGITIZED, dateTimeStr)
        exif.setAttribute(TAG_DATETIME_ORIGINAL, dateTimeStr)
        exif.setAttribute(TAG_OFFSET_TIME, offsetStr)
        exif.setAttribute(TAG_OFFSET_TIME_DIGITIZED, offsetStr)
        exif.setAttribute(TAG_OFFSET_TIME_ORIGINAL, offsetStr)
        exif.setAttribute(TAG_USER_COMMENT, comment)
        exif.setAttribute(TAG_MAKE, Build.MANUFACTURER)
        exif.setAttribute(TAG_MODEL, Build.MODEL)
        exif.setAttribute(TAG_SOFTWARE, "${application.getString(R.string.app_name)} ( ${getVersionName()} )")
        exif.saveAttributes()

        // Content Provider Details
        val resolver: ContentResolver = application.getContentResolver()
        contentUri?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { outStream ->
                    FileInputStream(file).use { fileInputStream ->
                        fileInputStream.copyTo(outStream)
                    }
                }
                return true
            } catch (_: Exception) {
                return false
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
            put(MediaStore.Images.Media.DATE_TAKEN, dateTaken)
            put(MediaStore.Images.Media.DATE_ADDED, dateTaken / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, dateTaken / 1000)
            put(MediaStore.Images.Media.IS_PENDING, 1) // For Android Q and above
        }

        val galleryUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false;
        resolver.openFileDescriptor(galleryUri, "rw")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { outStream ->
                FileInputStream(file).use { fileInputStream ->
                    fileInputStream.copyTo(outStream)
                }
            }
        }
        if (file.exists()) file.delete()
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(galleryUri, values, null, null)
        dataSaved = true
        return true
    }

    /**************************************************************************************************/
    private fun generateImageFilename(): String {
        val dateTaken = System.currentTimeMillis()
        val date = Date(dateTaken)
        val dateFormat = SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss", Locale.getDefault())
        val title: String? = dateFormat.format(date)
        return "$title.jpg"
    }

    /**************************************************************************************************/
    fun getVersionName(): String? {
        val packageInfo: PackageInfo
        try {
            packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
        return packageInfo.versionName
    }
}

/**************************************************************************************************/
/* Helper Methods                                                                                 */
/**************************************************************************************************/
fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix()
    matrix.postRotate(degrees.toFloat())
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
