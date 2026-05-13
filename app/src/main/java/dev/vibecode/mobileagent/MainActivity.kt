package dev.vibecode.mobileagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.compose.rememberNavController
import dev.vibecode.mobileagent.data.SettingsRepository
import dev.vibecode.mobileagent.ui.MobileNavHost
import dev.vibecode.mobileagent.ui.theme.MobileAIShellTheme

val LocalSettingsRepository = staticCompositionLocalOf<SettingsRepository> {
    error("SettingsRepository not provided")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = applicationContext as MobileAgentApp
        setContent {
            CompositionLocalProvider(LocalSettingsRepository provides app.settingsRepository) {
                MobileAIShellTheme {
                    val navController = rememberNavController()
                    MobileNavHost(navController = navController)
                }
            }
        }
    }
}
