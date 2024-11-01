// app/src/main/java/com/ai/cameracalibration/ui/setup/DeviceSetupFragment.kt
package com.ai.cameracalibration.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ai.cameracalibration.MainActivity
import com.ai.cameracalibration.databinding.FragmentDeviceSetupBinding

class DeviceSetupFragment : Fragment() {
    private var _binding: FragmentDeviceSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonMaster.setOnClickListener {
            (activity as? MainActivity)?.connectAsMaster()
        }

        binding.buttonSlave.setOnClickListener {
            (activity as? MainActivity)?.connectAsSlave()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}