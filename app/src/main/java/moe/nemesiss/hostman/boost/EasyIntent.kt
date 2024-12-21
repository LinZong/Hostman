package moe.nemesiss.hostman.boost

import android.content.Context
import android.content.Intent
import android.net.Uri

object EasyIntent {

    // Function to open the URL
    fun openBrowser(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            // Check if there's an app that can handle the Intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        }
    }
}