package sg.qast.player

import android.content.Context
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManagerListener

/**
 * Google Cast sender integration. The phone serves SAF files to the receiver
 * through a short-lived, tokenised LAN URL. No format transcoding is performed.
 */
class QastCastController(private val context: Context) {
    private val server = LocalMediaServer(context.applicationContext)
    private val castContext: CastContext by lazy { CastContext.getSharedInstance(context) }
    var selectedUri: Uri? = null
    var selectedMime: String = "video/mp4"
    var onStatus: (String) -> Unit = {}

    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            onStatus("Connected to ${session.castDevice?.friendlyName ?: "Cast device"}")
            castSelected()
        }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            onStatus("Cast session resumed")
        }
        override fun onSessionEnded(session: CastSession, error: Int) {
            server.stop()
            onStatus("Casting ended")
        }
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            onStatus("Could not connect to Cast device ($error)")
        }
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            onStatus("Cast reconnect failed ($error)")
        }
        override fun onSessionSuspended(session: CastSession, reason: Int) {
            onStatus("Cast connection suspended")
        }
    }

    fun initialise() {
        castContext.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
    }

    fun castSelected(): Boolean {
        val uri = selectedUri ?: run {
            onStatus("Select a video before casting")
            return false
        }
        val session = castContext.sessionManager.currentCastSession ?: return false
        return try {
            val address = server.start(uri)
            val info = MediaInfo.Builder(address)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(context.contentResolver.getType(uri) ?: selectedMime)
                .build()
            session.remoteMediaClient?.load(
                MediaLoadRequestData.Builder().setMediaInfo(info).setAutoplay(true).build()
            )
            onStatus("Sending video to ${session.castDevice?.friendlyName ?: "TV"}")
            true
        } catch (error: Exception) {
            onStatus("Unable to cast: ${error.message ?: "network error"}")
            false
        }
    }

    fun sessionConnected(): Boolean = castContext.sessionManager.currentCastSession?.isConnected == true

    fun pause() { castContext.sessionManager.currentCastSession?.remoteMediaClient?.pause() }
    fun play() { castContext.sessionManager.currentCastSession?.remoteMediaClient?.play() }
    fun stop() {
        castContext.sessionManager.currentCastSession?.remoteMediaClient?.stop()
        server.stop()
    }
    fun release() {
        try { castContext.sessionManager.removeSessionManagerListener(listener, CastSession::class.java) }
        catch (_: Exception) {}
        server.stop()
    }
}
