package com.bawabat.alorood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.bawabat.alorood.data.Repo
import com.bawabat.alorood.ui.AppRoot
import com.bawabat.alorood.ui.BawabatTheme
import com.bawabat.alorood.util.UpdateChecker

class MainActivity : ComponentActivity() {
    private var updater: UpdateChecker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
        updater = UpdateChecker(this, launcher).also { it.check() }
        setContent {
            BawabatTheme(Repo.lang, Repo.themeMode) { AppRoot() }
        }
    }

    override fun onDestroy() {
        updater?.dispose()
        super.onDestroy()
    }
}
