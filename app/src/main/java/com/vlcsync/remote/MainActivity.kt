package com.vlcsync.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vlcsync.remote.ui.MainScreen
import com.vlcsync.remote.ui.VLCSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VLCSyncTheme {
                MainScreen()
            }
        }
    }
}