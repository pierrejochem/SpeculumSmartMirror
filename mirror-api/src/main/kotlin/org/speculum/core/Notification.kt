package org.speculum.core

/**
 * Inter-module messaging, equivalent to MagicMirror's
 * sendNotification / notificationReceived bus. Modules can broadcast
 * events (e.g. "CLOCK_MINUTE", "WEATHER_UPDATED") that others react to.
 */
data class Notification(
    val name: String,
    val payload: Any? = null,
    val sender: String? = null
)

interface NotificationListener {
    fun onNotification(notification: Notification)
}