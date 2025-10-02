package moe.nemesiss.hostman.boost

import android.content.Context
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.DefaultMediaDecoder
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.network.NetworkSchemeHandler

object EasyMarkdown {


    /**
     * Create a [Markwon] instance from specific [context], with necessary plugins configured.
     */
    fun createMarkwon(context: Context): Markwon {
        return Markwon
            .builder(context)
            .usePlugin(ImagesPlugin.create { config ->
                config.addSchemeHandler(NetworkSchemeHandler.create())
                config.addMediaDecoder(DefaultMediaDecoder.create())
            })
            .usePlugin(HtmlPlugin.create())
            .build()
    }
}