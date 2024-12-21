package moe.nemesiss.hostman.ui.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import moe.nemesiss.hostman.BuildConfig
import moe.nemesiss.hostman.R
import moe.nemesiss.hostman.boost.EasyIntent
import moe.nemesiss.hostman.model.AppVersion

@Composable
fun NewVersionDialog(latestVersion: AppVersion, dismiss: () -> Unit = {}) {

    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {
            dismiss()
        }, confirmButton = {
            TextButton(onClick = {
                latestVersion.downloadUrl?.let { url -> EasyIntent.openBrowser(context, url) }
            }) {
                Text(stringResource(R.string.download))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                dismiss()
            }) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = {
            Text(text = latestVersion.release?.body ?: "No description for this version >_<")
        },
        title = { Text(text = "${BuildConfig.APPLICATION_NAME} ${latestVersion.version} is available") })
}