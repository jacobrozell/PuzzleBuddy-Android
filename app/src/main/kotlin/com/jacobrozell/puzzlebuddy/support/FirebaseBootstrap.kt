package com.jacobrozell.puzzlebuddy.support

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseBootstrap @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var analytics: FirebaseAnalytics? = null

    var isAnalyticsCollectionEnabled: Boolean = false
        private set
    var isCrashlyticsCollectionEnabled: Boolean = false
        private set

    val shouldConfigure: Boolean
        get() = hasValidConfiguration() && !TelemetryRuntime.isRunningUnderInstrumentedTest()

    fun initialize(app: Application) {
        if (!hasValidConfiguration()) {
            isAnalyticsCollectionEnabled = false
            isCrashlyticsCollectionEnabled = false
            return
        }

        if (TelemetryRuntime.isRunningUnderInstrumentedTest()) {
            isAnalyticsCollectionEnabled = false
            isCrashlyticsCollectionEnabled = false
            return
        }

        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app)
        }

        val remoteEnabled = TelemetryRuntime.isRemoteTelemetryCollectionEnabled()
        isAnalyticsCollectionEnabled = remoteEnabled
        isCrashlyticsCollectionEnabled = remoteEnabled

        FirebaseAnalytics.getInstance(app).setAnalyticsCollectionEnabled(remoteEnabled)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = remoteEnabled
        analytics = if (remoteEnabled) FirebaseAnalytics.getInstance(app) else null
    }

    fun analyticsInstance(): FirebaseAnalytics? = analytics

    private fun hasValidConfiguration(): Boolean {
        val resourceId = context.resources.getIdentifier("google_app_id", "string", context.packageName)
        if (resourceId == 0) return false
        val appId = context.getString(resourceId)
        return appId.isNotBlank() && !appId.contains("REPLACE_WITH", ignoreCase = true)
    }
}
