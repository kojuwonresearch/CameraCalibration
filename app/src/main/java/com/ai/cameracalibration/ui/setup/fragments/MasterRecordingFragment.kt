// MasterRecordingFragment.kt
package com.ai.cameracalibration.ui.setup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ai.cameracalibration.databinding.FragmentMasterRecordingBinding
import com.ai.cameracalibration.network.WebSocketManager

class MasterRecordingFragment : Fragment() {
    private var _binding: FragmentMasterRecordingBinding? = null
    private val binding get() = _binding!!
    private val webSocketManager = WebSocketManager.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMasterRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSendToSlaves.setOnClickListener {
            webSocketManager.sendMessage("SEND_TO_SLAVES")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}