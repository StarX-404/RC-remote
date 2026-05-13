package com.rcremote

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rcremote.ui.navigation.AppNavGraph
import com.rcremote.ui.theme.BgDeep
import com.rcremote.ui.theme.RCRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep screen on while app is in foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            RCRemoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDeep
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}
