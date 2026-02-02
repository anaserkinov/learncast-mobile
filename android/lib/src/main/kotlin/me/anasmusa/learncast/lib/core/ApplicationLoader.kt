package me.anasmusa.learncast.lib.core

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import me.anasmusa.learncast.core.AppConfig
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.getModules
import me.anasmusa.learncast.lib.R
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

abstract class ApplicationLoader : me.anasmusa.learncast.ApplicationLoader() {
    override fun onCreate() {
        super.onCreate()

        AppConfig.Companion.update(
            R.string.download_notification_title,
            R.string.download_notification_message,
        )

        startKoin {
            androidContext(this@ApplicationLoader)
            modules(getModules())
        }

        val placeHolderImage = getDrawable(appConfig.mainLogoInt)!!.asImage(true)
        SingletonImageLoader.setSafe { context ->
            ImageLoader
                .Builder(context)
                .components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = { OkHttpClient() },
                        ),
                    )
                }.placeholder(placeHolderImage)
                .error(placeHolderImage)
                .build()
        }
    }
}
