package dev.bartuzen.qbitcontroller

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

actual object Telemetry {
    actual fun setCurrentScreen(screen: String, tab: String?) {
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screen)
            if (tab != null) {
                param("screen_tab", tab)
            }
        }
    }
}
