package com.wordmatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wordmatch.data.PrefsScoreStore
import com.wordmatch.data.WordRepositoryImpl
import com.wordmatch.game.GameViewModel
import com.wordmatch.ui.MainScreen
import com.wordmatch.ui.theme.WordMatchTheme
import com.wordmatch.util.AndroidSoundManager

class MainActivity : ComponentActivity() {

    // Manual DI (no Hilt for a handful of collaborators). Survives rotation via viewModels().
    private val viewModel: GameViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GameViewModel(
                    repository = WordRepositoryImpl(applicationContext, BuildConfig.JSONBIN_BIN_ID),
                    sound = AndroidSoundManager(applicationContext),
                    store = PrefsScoreStore(applicationContext)
                ) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WordMatchTheme {
                MainScreen(viewModel)
            }
        }
    }
}
