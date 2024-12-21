package moe.nemesiss.hostman.model.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import moe.nemesiss.hostman.BuildConfig
import moe.nemesiss.hostman.boost.EasyDebug
import moe.nemesiss.hostman.model.AppVersion
import moe.nemesiss.hostman.model.NewVersionAvailableResult
import moe.nemesiss.hostman.service.CheckUpdateService

class CheckUpdateViewModel : ViewModel() {

    private val TAG = "CheckUpdateViewModel"

    val hasLatestVersion = MutableLiveData(false)

    val latestVersion = MutableLiveData(AppVersion(BuildConfig.VERSION_NAME))

    val newVersionAvailableDialogShown = MutableLiveData(false)

    suspend fun checkNewVersionAvailable() {
        EasyDebug.info(TAG) { "Begin checking new version for app." }
        val trace = Firebase.performance.newTrace("check_new_version_available")
        trace.start()
        val result = CheckUpdateService.checkNewVersionAvailable()
        trace.stop()
        EasyDebug.info(TAG) { "Check new version for app done." }
        if (result is NewVersionAvailableResult) {
            hasLatestVersion.value = true
            latestVersion.value = result.latestVersion
        } else {
            hasLatestVersion.value = false
            latestVersion.value = null
        }
    }

    fun showNewVersionDialog() {
        newVersionAvailableDialogShown.value = true
    }

    fun hideNewVersionDialog() {
        newVersionAvailableDialogShown.value = false
    }
}