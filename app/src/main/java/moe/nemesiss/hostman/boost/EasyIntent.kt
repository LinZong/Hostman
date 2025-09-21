package moe.nemesiss.hostman.boost

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.service.quicksettings.TileService
import moe.nemesiss.hostman.boost.EasyIntent.startActivity

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

    /**
     * Call [startActivity] on current [ctx].
     * If current [ctx] is a [TileService], it will automatically wrap [intent] with [PendingIntent] for VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE
     * and call [androidx.core.service.quicksettings.TileServiceCompat.startActivityAndCollapse]
     */
    fun startActivity(ctx: Context, intent: Intent) {
        if (ctx is TileService &&
            VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            val pendingIntent = PendingIntent.getActivity(
                ctx,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE  // Use FLAG_IMMUTABLE for API 23+
            )
            ctx.startActivityAndCollapse(pendingIntent)
        } else {
            ctx.startActivity(intent)
        }
    }
}