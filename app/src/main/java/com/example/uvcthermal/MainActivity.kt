package com.example.uvcthermal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.example.uvcthermal.ui.ThermalScopeApp
import com.example.uvcthermal.ui.theme.UVCThermalTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UVCThermalTheme {
                ThermalScopeApp()
            }
        }
    }
}
