package com.metmc.os.media

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

object MetmcMediaInfo {

    @Volatile
    var title: String = ""
        private set

    @Volatile
    var artist: String = ""
        private set

    @Volatile
    var album: String = ""
        private set

    @Volatile
    var playing: Boolean = false
        private set

    fun update(controller: MediaController?) {
        if (controller == null) {
            clear()
            return
        }

        val metadata = controller.metadata

        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        playing = controller.playbackState?.state ==
                android.media.session.PlaybackState.STATE_PLAYING
    }

    fun clear() {
        title = ""
        artist = ""
        album = ""
        playing = false
    }

    fun displayText(): String {
        if (title.isBlank() && artist.isBlank()) return ""

        return when {
            title.isNotBlank() && artist.isNotBlank() ->
                "$title — $artist"

            title.isNotBlank() ->
                title

            else ->
                artist
        }
    }
}

class MetmcMediaNotificationListener : NotificationListenerService() {

    private var manager: MediaSessionManager? = null

    private val listener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            val controller = controllers
                ?.firstOrNull { it.playbackState?.state ==
                        android.media.session.PlaybackState.STATE_PLAYING }
                ?: controllers?.firstOrNull()

            MetmcMediaInfo.update(controller)
        }

    override fun onCreate() {
        super.onCreate()

        manager = getSystemService(MediaSessionManager::class.java)

        try {
            manager?.addOnActiveSessionsChangedListener(
                listener,
                componentName
            )
        } catch (_: SecurityException) {
            MetmcMediaInfo.clear()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onDestroy() {
        try {
            manager?.removeOnActiveSessionsChangedListener(listener)
        } catch (_: Exception) {
        }

        MetmcMediaInfo.clear()
        super.onDestroy()
    }

    private fun refresh() {
        try {
            val controllers = manager?.getActiveSessions(componentName)
            val controller = controllers
                ?.firstOrNull { it.playbackState?.state ==
                        android.media.session.PlaybackState.STATE_PLAYING }
                ?: controllers?.firstOrNull()

            MetmcMediaInfo.update(controller)
        } catch (_: SecurityException) {
            MetmcMediaInfo.clear()
        }
    }
}
