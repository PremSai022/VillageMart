package com.mart.villagemart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mart.villagemart.notification.VillageMartMessagingService
import com.mart.villagemart.ui.VillageMartRoot
import com.mart.villagemart.ui.theme.VillageMartTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        VillageMartMessagingService.ensureChannel(this)
        setContent {
            VillageMartTheme {
                VillageMartRoot()
            }
        }
    }
}
