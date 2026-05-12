package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.example.project.data.ApiClient
import org.example.project.data.Database
import org.example.project.data.Repository
import org.example.project.ui.home.HomeScreen
import org.example.project.ui.home.HomeViewModel
import org.example.project.ui.login.LoginScreen
import org.example.project.ui.login.LoginViewModel

@Composable
fun App(database: Database, serverUrl: String = "http://10.0.2.2:8080") {
    val apiClient = remember { ApiClient(serverUrl) }
    val repository = remember { Repository(apiClient, database) }

    MaterialTheme {
        var isLoggedIn by remember { mutableStateOf(false) }

        if (isLoggedIn) {
            val homeViewModel = remember { HomeViewModel(repository) }
            HomeScreen(
                viewModel = homeViewModel,
                onLogout = { isLoggedIn = false }
            )
        } else {
            val loginViewModel = remember { LoginViewModel(repository) }
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { isLoggedIn = true }
            )
        }
    }
}
