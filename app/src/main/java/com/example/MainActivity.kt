package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.MainAppNavGraph
import com.example.ui.theme.DrinkYourWaterTheme
import com.example.ui.viewmodel.WaterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WaterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DrinkYourWaterTheme {
                MainAppNavGraph(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }
}
