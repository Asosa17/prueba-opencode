package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.project.data.DesktopDatabase
import org.example.project.data.ApiClient

fun main() = application {
    val database = DesktopDatabase()
    val serverUrl = "http://localhost:8080"

    Window(
        onCloseRequest = ::exitApplication,
        title = "Pokémon App",
    ) {
        App(database = database, serverUrl = serverUrl)
    }
}
