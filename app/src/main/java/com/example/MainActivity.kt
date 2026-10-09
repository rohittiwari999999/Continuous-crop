package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.ContinuousCropScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WorkspaceBackground
import com.example.ui.viewmodel.ContinuousCropViewModel

class MainActivity : ComponentActivity() {
    private val cropViewModel: ContinuousCropViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WorkspaceBackground
                ) {
                    ContinuousCropScreen(viewModel = cropViewModel)
                }
            }
        }
    }
}
