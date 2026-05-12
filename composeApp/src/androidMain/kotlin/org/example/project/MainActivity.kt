package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.example.project.data.AndroidDatabase
import org.example.project.data.ApiClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val database = AndroidDatabase(this)

        // 10.0.2.2 is Android emulator's localhost
        val serverUrl = "http://10.0.2.2:8080"

        setContent {
            App(database = database, serverUrl = serverUrl)
        }
    }
}
