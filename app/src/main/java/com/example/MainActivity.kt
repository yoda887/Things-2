package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.TaskRepository
import com.example.ui.screens.ThingsHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ThingsViewModel

class MainActivity : ComponentActivity() {

  private val database by lazy {
    Room.databaseBuilder(
      applicationContext,
      AppDatabase::class.java,
      "things_database"
    )
    .fallbackToDestructiveMigration()
    .build()
  }

  private val repository by lazy { TaskRepository(database.taskDao(), applicationContext) }

  private val viewModel: ThingsViewModel by viewModels {
    ThingsViewModel.Factory(repository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          ThingsHomeScreen(viewModel = viewModel)
        }
      }
    }
  }
}
