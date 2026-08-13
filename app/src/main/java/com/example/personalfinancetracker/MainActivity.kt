package com.example.personalfinancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.navigation.FinanceApp
import com.example.personalfinancetracker.ui.theme.PersonalFinanceTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalFinanceTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val initialType = intent.getStringExtra(EXTRA_TRANSACTION_TYPE)
                        ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                    FinanceApp(initialType)
                }
            }
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }
}
