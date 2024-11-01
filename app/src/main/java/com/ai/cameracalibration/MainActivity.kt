// app/src/main/java/com/ai/cameracalibration/MainActivity.kt
package com.ai.cameracalibration

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.ai.cameracalibration.databinding.ActivityMainBinding
import com.ai.cameracalibration.network.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val webSocketManager = WebSocketManager.getInstance()
    private var isMaster = false  // 마스터 상태 추적을 위한 변수 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupWebSocketListener()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController)
    }

    private fun setupWebSocketListener() {
        lifecycleScope.launch {
            webSocketManager.messageFlow.collect { message ->
                message?.let {
                    Log.d("MainActivity", "Received message: $it")
                    if (!isMaster && it.contains("M1")) {  // 마스터가 아닐 때만 처리
                        runOnUiThread {
                            try {
                                val currentDestination = navController.currentDestination?.id
                                when (currentDestination) {
                                    R.id.deviceSetupFragment -> {
                                        navController.navigate(R.id.action_to_slave)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Navigation error: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    fun connectAsMaster() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val deviceId = UUID.randomUUID().toString()
                webSocketManager.disconnect()
                isMaster = true  // 마스터 상태로 설정
                webSocketManager.connect(deviceId, "MASTER")
                runOnUiThread {
                    navController.navigate(R.id.action_to_master)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Master connection error: ${e.message}")
                isMaster = false  // 연결 실패시 마스터 상태 해제
            }
        }
    }

    fun connectAsSlave() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val deviceId = UUID.randomUUID().toString()
                webSocketManager.disconnect()
                isMaster = false  // 슬레이브 상태로 설정
                webSocketManager.connect(deviceId, "SLAVE")
            } catch (e: Exception) {
                Log.e("MainActivity", "Slave connection error: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.disconnect()
        isMaster = false
    }
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}
