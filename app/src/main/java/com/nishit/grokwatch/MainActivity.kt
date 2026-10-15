package com.nishit.grokwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nishit.grokwatch.ui.CrashScreen
import com.nishit.grokwatch.ui.theme.GrokTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var crashTrace by remember { mutableStateOf(CrashLog.read(this@MainActivity)) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                GrokTheme {
                    val trace = crashTrace
                    if (trace != null) {
                        CrashScreen(
                            trace = trace,
                            onClear = {
                                CrashLog.clear(this@MainActivity)
                                crashTrace = null
                            },
                        )
                    } else {
                        GrokApp()
                    }
                }
            }
        }
    }
}
