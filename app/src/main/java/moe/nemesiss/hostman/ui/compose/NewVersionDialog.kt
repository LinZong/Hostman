package moe.nemesiss.hostman.ui.compose

import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import moe.nemesiss.hostman.BuildConfig
import moe.nemesiss.hostman.R
import moe.nemesiss.hostman.boost.EasyIntent
import moe.nemesiss.hostman.boost.EasyMarkdown
import moe.nemesiss.hostman.model.AppVersion

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun NewVersionDialog(latestVersion: AppVersion, dismiss: () -> Unit = {}) {

    val context = LocalContext.current

    val markwon = remember(context) { EasyMarkdown.createMarkwon(context) }

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
            val body = latestVersion.release?.body ?: stringResource(R.string.no_description_for_this_version)
            AndroidView(modifier = Modifier.fillMaxWidth(),
                        onReset = { it.text = "" },
                        factory = { ctx -> TextView(ctx).apply { markwon.setMarkdown(this, body) } },
                        update = { markwon.setMarkdown(it, body) })
        },
        title = { Text(text = "${BuildConfig.APPLICATION_NAME} ${latestVersion.version} is available") })
}