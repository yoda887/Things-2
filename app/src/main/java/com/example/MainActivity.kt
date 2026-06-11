package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.screens.home.ThingsHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ThingsViewModel

/**
 * Главная Activity приложения, которая служит контейнером для Jetpack Compose интерфейса.
 * Подключается к [ThingsApplication] для получения графа зависимостей и инициализации [ThingsViewModel].
 */
class MainActivity : ComponentActivity() {

  private val viewModel: ThingsViewModel by viewModels {
    val container = (application as ThingsApplication).container
    ThingsViewModel.Factory(container.useCases)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          // Оставляем исходный вызов экрана HomeScreen без изменений
          ThingsHomeScreen(viewModel = viewModel)
        }
      }
    }
  }
}
