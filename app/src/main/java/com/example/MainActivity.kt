package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.RoverApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.RoverViewModel

class MainActivity : ComponentActivity() {

    private val roverViewModel: RoverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RoverApp(viewModel = roverViewModel)
            }
        }
    }
}
