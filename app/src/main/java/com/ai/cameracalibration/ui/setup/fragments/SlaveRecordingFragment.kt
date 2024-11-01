// app/src/main/java/com/ai/cameracalibration/ui/setup/fragments/SlaveRecordingFragment.kt
package com.ai.cameracalibration.ui.setup.fragments
import java.util.Calendar
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ai.cameracalibration.camera.CameraRecorder
import com.ai.cameracalibration.databinding.FragmentSlaveRecordingBinding
import kotlinx.coroutines.*
import androidx.navigation.fragment.findNavController
import com.ai.cameracalibration.R

private const val TAG = "SlaveRecordingFragment"

class SlaveRecordingFragment : Fragment() {
    private var _binding: FragmentSlaveRecordingBinding? = null
    private val binding get() = _binding!!
    private var cameraRecorder: CameraRecorder? = null
    private var recordingJob: Job? = null

    private val requiredPermissions = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            Log.d(TAG, "All permissions granted")
            initializeCamera()
        } else {
            Log.e(TAG, "Permissions not granted: ${permissions.filter { !it.value }.keys}")
            Toast.makeText(context, "카메라와 오디오 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlaveRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions")
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            Log.d(TAG, "All permissions already granted")
            initializeCamera()
        } else {
            Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun initializeCamera() {
        try {
            lifecycleScope.launch {
                binding.statusText.text = "카메라 초기화 중..."
                cameraRecorder = CameraRecorder(
                    requireContext(),
                    viewLifecycleOwner,
                    binding.previewView
                )
                setupRecordingSchedule()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera: ${e.message}")
            binding.statusText.text = "카메라 초기화 실패: ${e.message}"
        }
    }

    private fun setupRecordingSchedule() {
        recordingJob = lifecycleScope.launch(Dispatchers.Main) {
            try {
                // 현재 시간을 계속 체크하다가 정각이 되면 녹화 시작
                while (isActive) {
                    val currentTime = Calendar.getInstance()
                    val currentSecond = currentTime.get(Calendar.SECOND)
                    val currentMillis = currentTime.get(Calendar.MILLISECOND)

                    if (currentSecond == 0 && currentMillis < 100) {  // 밀리초까지 체크하여 더 정확한 타이밍
                        startRecordingProcess()
                        break  // 녹화가 시작되면 루프 종료
                    }

                    // 남은 시간 표시
                    binding.statusText.text = "다음 분까지 대기 중... (${60 - currentSecond}초)"
                    delay(10)  // 10ms 간격으로 체크
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recording error", e)
                binding.statusText.text = "녹화 오류: ${e.message}"
                navigateToMain()
            }
        }
    }

    private suspend fun startRecordingProcess() {
        try {
            val startTime = Calendar.getInstance()
            binding.statusText.text = "녹화 시작: ${String.format("%02d:%02d",
                startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE))}"

            cameraRecorder?.startRecording(
                onRecordingStarted = {
                    binding.statusText.text = "녹화 중..."
                },
                onRecordingFinished = { file ->
                    if (file.exists() && file.length() > 0) {
                        binding.statusText.text = "녹화 완료: ${file.name} (${file.length() / 1024} KB)"
                        Log.d(TAG, "Recording saved to: ${file.absolutePath}")
                        navigateToMain()
                    } else {
                        binding.statusText.text = "녹화 실패: 파일이 생성되지 않음"
                        Log.e(TAG, "Recording file doesn't exist or is empty")
                        navigateToMain()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Recording start error", e)
            binding.statusText.text = "녹화 시작 오류: ${e.message}"
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        lifecycleScope.launch(Dispatchers.Main) {
            findNavController().navigate(R.id.action_slaveRecordingFragment_to_deviceSetupFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recordingJob?.cancel()
        cameraRecorder = null
        _binding = null
    }
}