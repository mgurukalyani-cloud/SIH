package com.itantra.app.presentation.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object InitialSetup : Screen("initial_setup")
    object Dashboard : Screen("dashboard")
    object PushToTalk : Screen("push_to_talk")
    object Conversation : Screen("conversation")
    object DeviceConnection : Screen("device_connection")
    object Settings : Screen("settings")
    object Diagnostics : Screen("diagnostics")
}
