package com.itantra.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.core.permissions.PermissionHandler
import com.itantra.app.iTantraApplication
import com.itantra.app.presentation.navigation.AppNavHost
import com.itantra.app.presentation.theme.ITantraTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            TacticalLogger.i("MainActivity", "All runtime permissions granted")
        } else {
            TacticalLogger.w("MainActivity", "Some permissions were denied. Certain offline hardware functions may be limited.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions for audio and bluetooth
        val needed = PermissionHandler.getRequiredPermissions().filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }

        val app = application as iTantraApplication

        setContent {
            ITantraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        app = app
                    )
                }
            }
        }
    }
}
