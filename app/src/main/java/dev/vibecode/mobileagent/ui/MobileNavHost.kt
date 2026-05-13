package dev.vibecode.mobileagent.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.vibecode.mobileagent.LocalSettingsRepository
import dev.vibecode.mobileagent.agent.StubStreamingAgentBridge
import dev.vibecode.mobileagent.agent.TermuxOpenClaudeBridge
import dev.vibecode.mobileagent.data.SettingsRepository
import dev.vibecode.mobileagent.ui.chat.ChatScreen
import dev.vibecode.mobileagent.ui.chat.ChatViewModel
import dev.vibecode.mobileagent.ui.settings.SettingsScreen

@Composable
fun MobileNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            val repo = LocalSettingsRepository.current
            val appCtx = LocalContext.current.applicationContext
            val backendId = repo.getAgentBackendId()
            val bridge =
                remember(backendId) {
                    when (backendId) {
                        SettingsRepository.AGENT_BACKEND_TERMUX_OPENCLAUDE ->
                            TermuxOpenClaudeBridge(appCtx)
                        else -> StubStreamingAgentBridge()
                    }
                }
            val viewModel: ChatViewModel =
                viewModel(
                    key = backendId,
                    factory = ChatViewModel.factory(bridge),
                )
            ChatScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
