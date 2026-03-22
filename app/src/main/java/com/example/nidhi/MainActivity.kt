package com.example.nidhi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.nidhi.navigation.NavGraph
import com.example.nidhi.ui.theme.NidhiTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            NidhiTheme {
                NavGraph()
            }
        }
    }
}