package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.AbhiAiTheme
import com.example.ui.viewmodel.ChatViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val chatViewModel: ChatViewModel = viewModel()
            val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
            var showSplash by remember { mutableStateOf(true) }

            AbhiAiTheme(
                darkTheme = uiState.isDarkMode,
                accentTheme = uiState.accentTheme
            ) {
                // Speech Recognizer Launcher
                val speechLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val spokenText = result.data
                            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                            ?.firstOrNull()
                        if (!spokenText.isNullOrBlank()) {
                            chatViewModel.sendMessage(overrideText = spokenText)
                        }
                    }
                }

                fun launchVoiceInput() {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Abhi AI...")
                        }
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "Voice input is not available on this device",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 400),
                    label = "splash_crossfade"
                ) { isSplashActive ->
                    if (isSplashActive) {
                        SplashScreen(
                            onFinished = { showSplash = false }
                        )
                    } else {
                        ChatScreen(
                            viewModel = chatViewModel,
                            onVoiceInputClick = { launchVoiceInput() }
                        )
                    }
                }
            }
        }
    }
}
