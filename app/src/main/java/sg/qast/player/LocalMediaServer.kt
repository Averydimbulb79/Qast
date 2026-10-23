package sg.qast.player

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Temporary local Wi-Fi HTTP server for Google Cast / UPnP receivers.
 * The token is random per server start and never logged. The file is read via
 * Android's SAF, not copied to public storage. Only one source is served.
 *
 * Alpha limitation: does not transcode media. Keep Qast active during casting.
 */
class LocalMediaServer(private val context: Context) {
    private var listener: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private var activeUri: Uri? = null
    private var fileSize: Long = -1L
    private var mimeType: String = "video/mp4"
    private var token: String = ""

    val running: Boolean get() = listener?.isClosed == false

    @Synchronized
    fun start(uri: Uri): String {
        stop()
        val address = NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress &&
                it.isSiteLocalAddress && !it.hostAddress.orEmpty().startsWith("169.254.") }
            ?.hostAddress
            ?: throw IllegalStateException("Connect your phone to the same Wi-Fi as the receiver.")
        fileSize = querySize(uri)
        mimeType = context.contentResolver.getType(uri) ?: guessMime(uri)
        activeUri = uri
        val secret = ByteArray(24).also { SecureRandom().nextBytes(it) }
        token = secret.joinToString("") { "%02x".format(it) }
        val server = ServerSocket(0)
        listener = server
        thread(name = "Qast-local-media", isDaemon = true) {
            while (!server.isClosed) {
                try {
                    val socket = server.accept()
                    executor.execute { serve(socket) }
                } catch (_: Exception) {
                    if (!server.isClosed) break
                }
            }
        }
        return "http://$address:${server.localPort}/media/$token"
    }

    @Synchronized
    fun stop() {
        listener?.close()
        listener = null
        activeUri = null
    }

    private fun querySize(uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getLong(0)
            }
        return -1L
    }

    private fun guessMime(uri: Uri): String = when {
        uri.toString().endsWith(".mkv", true) -> "video/x-matroska"
        uri.toString().endsWith(".webm", true) -> "video/webm"
        uri.toString().endsWith(".mov", true) -> "video/quicktime"
        else -> "video/mp4"
    }

    private fun serve(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 15000
            try {
                val raw = BufferedInputStream(client.getInputStream())
                val writer = BufferedOutputStream(client.getOutputStream())
                val request = readLine(raw) ?: return
                val parts = request.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val path = parts[1]
                var range: String? = null
                while (true) {
                    val line = readLine(raw) ?: break
                    if (line.isEmpty()) break
                    if (line.startsWith("Range:", ignoreCase = true)) {
                        range = line.substringAfter(':').trim()
                    }
                }
                val uri = activeUri
                if (path != "/media/$token" || uri == null) {
                    writer.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    writer.flush()
                    return
                }
                if (method != "GET" && method != "HEAD") {
                    writer.write("HTTP/1.1 405 Method Not Allowed\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    writer.flush()
                    return
                }
                val total = fileSize
                var from = 0L
                var to = if (total >= 0) total - 1 else Long.MAX_VALUE
                val isRange = range?.startsWith("bytes=") == true && total > 0
                if (isRange) {
                    val spec = range!!.substringAfter("bytes=").substringBefore(',')
                    val first = spec.substringBefore('-')
                    val last = spec.substringAfter('-', "")
                    if (first.isBlank()) {
                        val suffix = last.toLongOrNull()?.coerceAtMost(total) ?: 0L
                        from = total - suffix
                    } else {
                        from = first.toLongOrNull() ?: 0L
                        if (last.isNotBlank()) to = last.toLongOrNull() ?: to
                    }
                    to = to.coerceAtMost(total - 1)
                    if (from >= total || to < from) {
                        writer.write("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */$total\r\nContent-Length: 0\r\n\r\n".toByteArray())
                        writer.flush()
                        return
                    }
                }
                val count = if (total >= 0) (to - from + 1) else -1L
                val header = buildString {
                    append(if (isRange) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
                    append("Content-Type: $mimeType\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    if (count >= 0) append("Content-Length: $count\r\n")
                    if (isRange) append("Content-Range: bytes $from-$to/$total\r\n")
                    append("Connection: close\r\n\r\n")
                }
                writer.write(header.toByteArray())
                writer.flush()
                if (method == "HEAD") return
                context.contentResolver.openInputStream(uri)?.use { source ->
                    var remainingToSkip = from
                    val discard = ByteArray(65536)
                    while (remainingToSkip > 0) {
                        val n = source.read(discard, 0, minOf(discard.size.toLong(), remainingToSkip).toInt())
                        if (n < 0) return@use
                        remainingToSkip -= n
                    }
                    var remaining = count
                    val buffer = ByteArray(65536)
                    while (remaining != 0L) {
                        val allowed = if (remaining < 0) buffer.size else minOf(buffer.size.toLong(), remaining).toInt()
                        val n = source.read(buffer, 0, allowed)
                        if (n < 0) break
                        writer.write(buffer, 0, n)
                        if (remaining > 0) remaining -= n
                    }
                    writer.flush()
                }
            } catch (_: Exception) {
                // Disconnections and receiver cancellation are expected.
            }
        }
    }

    private fun readLine(input: BufferedInputStream): String? {
        val bytes = java.io.ByteArrayOutputStream()
        while (bytes.size() < 8192) {
            val byte = input.read()
            if (byte < 0) return if (bytes.size() == 0) null else bytes.toString("UTF-8")
            if (byte == 10) return bytes.toString("UTF-8").trimEnd('\r')
            bytes.write(byte)
        }
        return null
    }
}
