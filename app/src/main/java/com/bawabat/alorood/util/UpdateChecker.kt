package com.bawabat.alorood.util

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

object UpdateState {
    var downloaded by mutableStateOf(false)
    var manager: AppUpdateManager? = null

    fun complete() { try { manager?.completeUpdate() } catch (e: Exception) { /* ignore */ } }
}

/**
 * التحديثات داخل التطبيق عبر Google Play In-App Updates (الطريقة المسموح بها في Google Play).
 * تعمل فقط للنسخة المثبّتة من المتجر؛ في أي حالة أخرى تفشل بصمت ولا تؤثر على التطبيق.
 */
class UpdateChecker(
    private val activity: ComponentActivity,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>
) {
    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) UpdateState.downloaded = true
    }

    fun check() {
        try {
            val m = AppUpdateManagerFactory.create(activity)
            UpdateState.manager = m
            m.registerListener(listener)
            m.appUpdateInfo.addOnSuccessListener { info ->
                try {
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        UpdateState.downloaded = true
                    } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    ) {
                        m.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
                    }
                } catch (e: Exception) { /* ignore */ }
            }
        } catch (e: Exception) { /* ignore */ }
    }

    fun dispose() {
        try { UpdateState.manager?.unregisterListener(listener) } catch (e: Exception) { /* ignore */ }
    }
}
