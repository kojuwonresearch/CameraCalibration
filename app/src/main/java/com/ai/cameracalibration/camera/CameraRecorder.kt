package com.ai.cameracalibration.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var currentRecordingFile: File? = null

    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(context).apply {
            addListener({
                try {
                    continuation.resume(get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    suspend fun startRecording(
        onRecordingStarted: () -> Unit,
        onRecordingFinished: (File) -> Unit
    ) = withContext(Dispatchers.Main) {
        try {
            val cameraProvider = getCameraProvider()

            // 녹화 설정
            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )

            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch(e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                throw e
            }

            currentRecordingFile = createOutputFile()
            val fileOptions = FileOutputOptions.Builder(currentRecordingFile!!)
                .build()

            recording = videoCapture?.output?.prepareRecording(context, fileOptions)
                ?.apply {
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        withAudioEnabled()
                    }
                }
                ?.start(ContextCompat.getMainExecutor(context)) { event ->
                    when(event) {
                        is VideoRecordEvent.Start -> {
                            Log.i(TAG, "Recording started")
                            onRecordingStarted()
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (event.hasError()) {
                                Log.e(TAG, "Recording failed: ${event.cause}")
                                currentRecordingFile?.delete()
                            } else {
                                Log.i(TAG, "Recording finished, saved to: ${currentRecordingFile?.absolutePath}")
                                currentRecordingFile?.let { file ->
                                    if (file.exists() && file.length() > 0) {
                                        onRecordingFinished(file)
                                    } else {
                                        Log.e(TAG, "Recording file is empty or doesn't exist")
                                    }
                                }
                            }
                            recording = null
                        }
                    }
                }

            // 2분 후 녹화 중지
            delay(2 * 60 * 1000)
            stopRecording()

        } catch (e: Exception) {
            Log.e(TAG, "Error in startRecording", e)
            throw e
        }
    }

    private fun createOutputFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.also {
            if (!it.exists()) {
                it.mkdirs()
            }
        } ?: throw IllegalStateException("Failed to get external files directory")

        return File(storageDir, "REC_$timestamp.mp4").apply {
            if (exists()) {
                delete()
            }
        }
    }

    fun stopRecording() {
        try {
            recording?.stop()
            recording = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }

    companion object {
        private const val TAG = "CameraRecorder"
    }
}