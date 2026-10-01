package com.itantra.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.itantra.app.iTantraApplication
import com.itantra.app.presentation.connection.ConnectionScreen
import com.itantra.app.presentation.connection.ConnectionViewModel
import com.itantra.app.presentation.conversation.ConversationScreen
import com.itantra.app.presentation.conversation.ConversationViewModel
import com.itantra.app.presentation.dashboard.DashboardScreen
import com.itantra.app.presentation.dashboard.DashboardViewModel
import com.itantra.app.presentation.diagnostics.DiagnosticsScreen
import com.itantra.app.presentation.diagnostics.DiagnosticsViewModel
import com.itantra.app.presentation.onboarding.InitialSetupScreen
import com.itantra.app.presentation.onboarding.WelcomeScreen
import com.itantra.app.presentation.recording.PushToTalkScreen
import com.itantra.app.presentation.recording.RecordingViewModel
import com.itantra.app.presentation.settings.SettingsScreen
import com.itantra.app.presentation.settings.SettingsViewModel

import androidx.compose.runtime.remember

@Composable
fun AppNavHost(
    navController: NavHostController,
    app: iTantraApplication,
    startDestination: String = Screen.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onContinueClicked = {
                    navController.navigate(Screen.InitialSetup.route)
                }
            )
        }

        composable(Screen.InitialSetup.route) {
            InitialSetupScreen(
                prefs = app.appPreferences,
                onSetupComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val dashboardVm = remember {
                DashboardViewModel(
                    prefs = app.appPreferences,
                    messageRepository = app.messageRepository,
                    transport = app.activeTransport
                )
            }
            DashboardScreen(
                viewModel = dashboardVm,
                onNavigateToPtt = { navController.navigate(Screen.PushToTalk.route) },
                onNavigateToConversation = { navController.navigate(Screen.Conversation.route) },
                onNavigateToConnection = { navController.navigate(Screen.DeviceConnection.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
            )
        }

        composable(Screen.PushToTalk.route) {
            val recordingVm = remember {
                RecordingViewModel(
                    prefs = app.appPreferences,
                    transcribeAudioUseCase = app.transcribeAudioUseCase,
                    transmitMessageUseCase = app.transmitMessageUseCase
                )
            }
            PushToTalkScreen(
                viewModel = recordingVm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConversation = {
                    navController.navigate(Screen.Conversation.route) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        composable(Screen.Conversation.route) {
            val conversationVm = remember {
                ConversationViewModel(
                    prefs = app.appPreferences,
                    messageRepository = app.messageRepository,
                    synthesizeSpeechUseCase = app.synthesizeSpeechUseCase,
                    transmitMessageUseCase = app.transmitMessageUseCase
                )
            }
            ConversationScreen(
                viewModel = conversationVm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPtt = { navController.navigate(Screen.PushToTalk.route) }
            )
        }

        composable(Screen.DeviceConnection.route) {
            val connectionVm = ConnectionViewModel(
                prefs = app.appPreferences,
                transport = app.activeTransport
            )
            ConnectionScreen(
                viewModel = connectionVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val settingsVm = SettingsViewModel(
                prefs = app.appPreferences,
                messageRepository = app.messageRepository
            )
            SettingsScreen(
                viewModel = settingsVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Diagnostics.route) {
            val diagnosticsVm = DiagnosticsViewModel()
            DiagnosticsScreen(
                viewModel = diagnosticsVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
