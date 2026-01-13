package me.anasmusa.learncast

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

open class ApplicationLoader: Application(){

    companion object {
        lateinit var context: Context
            private set
        var currentActivity: Activity? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                if (currentActivity === activity) currentActivity = null
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
        Napier.base(DebugAntilog())
    }

}