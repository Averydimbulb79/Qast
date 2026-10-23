package sg.qast.player

import android.net.Uri
import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val Night = Color(0xFF0B121D)
private val Surface = Color(0xFF172636)
private val Ice = Color(0xFF79C8EB)
private val Muted = Color(0xFF9BAFC0)
private val White = Color(0xFFEAF4FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QastApp() }
    }
}

private enum class Page { Home, Videos, Cloud, Settings }

@Composable
private fun QastApp() {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    val casting = remember { QastCastController(context) }
    var castStatus by remember { mutableStateOf("") }
    DisposableEffect(casting) {
        casting.onStatus = { status ->
            castStatus = status
            if (status.startsWith("Sending video")) player.pause()
        }
         onDispose { casting.release() }
    }
    var playbackError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = "Video playback failed: ${error.errorCodeName}"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val airPlay = remember { AirPlayDiscovery(context) }
    var airPlayDevices by remember { mutableStateOf<List<String>>(emptyList()) }
    var airPlayMessage by remember { mutableStateOf("") }
    DisposableEffect(airPlay) {
        airPlay.onDevices = { airPlayDevices = it }
        airPlay.onError = { airPlayMessage = it }
        onDispose { airPlay.stop() }
    }

    var page by remember { mutableStateOf(Page.Home) }
    var current by remember { mutableStateOf<Uri?>(null) }
    var currentName by remember { mutableStateOf("Video") }
    LaunchedEffect(page, current) {
        if (page == Page.Settings && current == null) airPlay.start()
        else airPlay.stop()
    }
    val recent = remember { mutableStateListOf<Pair<String, Uri>>() }

    fun play(name: String, uri: Uri) {
        currentName = name
        current = uri
        playbackError = null
        casting.selectedUri = uri
        casting.selectedMime = context.contentResolver.getType(uri) ?: "video/mp4"
        player.stop()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
        recent.removeAll { it.second == uri }
        recent.add(0, name to uri)
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            val name = queryDisplayName(context, uri)
            play(name, uri)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Ice, onPrimary = Night, background = Night,
        surface = Surface, onSurface = White, onBackground = White
    )) {
        if (current != null) {
            PlayerScreen(
                player = player,
                name = currentName,
                onBack = { current = null },
                casting = casting,
                castStatus = castStatus,
                playbackError = playbackError
            )
        } else {
            Scaffold(
                containerColor = Night,
                topBar = {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.qast_approved_wordmark),
                            contentDescription = "Qast",
                            modifier = Modifier.weight(1f).height(65.dp).wrapContentWidth(Alignment.Start)
                        )
                        Text("ALPHA 0.1.5", color = Muted, fontSize = 10.sp)
                        Spacer(Modifier.width(12.dp))
                        IconButton(onClick = { picker.launch(arrayOf("video/*", "audio/*", "application/octet-stream")) }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Open media", tint = Ice)
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = Surface) {
                        listOf(
                            Triple(Page.Home, Icons.Default.Home, "Home"),
                            Triple(Page.Videos, Icons.Default.VideoLibrary, "Videos"),
                            Triple(Page.Cloud, Icons.Default.Cloud, "Cloud"),
                            Triple(Page.Settings, Icons.Default.Settings, "Settings")
                        ).forEach { (destination, icon, label) ->
                            NavigationBarItem(
                                selected = page == destination,
                                onClick = { page = destination },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Ice, selectedTextColor = Ice,
                                    unselectedIconColor = Muted, unselectedTextColor = Muted,
                                    indicatorColor = Night
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                    when (page) {
                        Page.Home -> {
                            Text("Your media. Beautifully played.", color = Muted, fontSize = 15.sp)
                            Spacer(Modifier.height(24.dp))
                            FeatureCard("Open a video", "Play files stored on your device",
                                Icons.Default.PlayCircle, { picker.launch(arrayOf("video/*", "audio/*", "application/octet-stream")) })
                            Spacer(Modifier.height(12.dp))
                            FeatureCard("Cloud library", "Google Drive, OneDrive and more (coming soon)",
                                Icons.Default.Cloud, { page = Page.Cloud })
                            Spacer(Modifier.height(24.dp))
                            Text("Recently opened", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                            RecentList(recent) { name, uri -> play(name, uri) }
                        }
                        Page.Videos -> {
                            Text("Videos", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("Open local files with Android's secure file picker.", color = Muted)
                            Spacer(Modifier.height(20.dp))
                            FeatureCard("Browse device", "Choose a media file", Icons.Default.FolderOpen,
                                { picker.launch(arrayOf("video/*", "audio/*", "application/octet-stream")) })
                            Spacer(Modifier.height(20.dp))
                            RecentList(recent) { name, uri -> play(name, uri) }
                        }
                        Page.Cloud -> {
                            Text("Network & Cloud", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Connections are planned for upcoming versions. No authentication is active yet.", color = Muted)
                            Spacer(Modifier.height(16.dp))
                            listOf("Google Drive", "OneDrive", "Dropbox", "WebDAV / Nextcloud",
                                "SMB / NAS", "DLNA / UPnP").forEach {
                                CloudRow(it)
                            }
                        }
                        Page.Settings -> {
                            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            SettingsLine("Version", "0.1.5-alpha")
                            SettingsLine("Playback engine", "AndroidX Media3")
                            SettingsLine("Cast", "Google Cast alpha; AirPlay discovery; DLNA next")
                            SettingsLine("AirPlay", "Receivers found: ${airPlayDevices.size} (sending not yet supported)")
                            if (airPlayDevices.isEmpty()) {
                                Text(airPlayMessage.ifEmpty { "No AirPlay receivers discovered on this network." },
                                    color = Muted, fontSize = 12.sp)
                            } else {
                                airPlayDevices.forEach { device ->
                                    Text(device + " · Discovery only", color = Ice, fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 5.dp))
                                }
                            }
                            SettingsLine("Cloud", "Planned: OAuth and network connectors")
                            SettingsLine("Privacy", "No analytics or account sign-in")
                        }
                    }
                }
            }
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String {
    var title = uri.lastPathSegment ?: "Selected video"
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
        null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) title = cursor.getString(0) ?: title
    }
    return title
}

@Composable
private fun FeatureCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                        onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp), color = Surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Ice, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = Muted)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
        }
    }
}

@Composable
private fun RecentList(recent: List<Pair<String, Uri>>, onPlay: (String, Uri) -> Unit) {
    if (recent.isEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text("Your recently opened files will appear here.", color = Muted, fontSize = 13.sp)
    } else {
        LazyColumn {
            items(recent) { (name, uri) ->
                Row(Modifier.fillMaxWidth().clickable { onPlay(name, uri) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Ice)
                    Spacer(Modifier.width(14.dp))
                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Ice)
                }
                HorizontalDivider(color = Surface)
            }
        }
    }
}

@Composable
private fun CloudRow(name: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Ice)
        Spacer(Modifier.width(16.dp))
        Text(name, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Text("Soon", color = Muted, fontSize = 12.sp)
    }
    HorizontalDivider(color = Surface)
}

@Composable
private fun SettingsLine(title: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Text(title, color = White, fontWeight = FontWeight.Medium)
        Text(value, color = Muted, fontSize = 13.sp)
    }
    HorizontalDivider(color = Surface)
}

/**
 * Player touch zones occupy the outer 28% of the video area.
 * Center playback controls and the bottom transport bar remain accessible.
 * Swiping up increases brightness/volume; swiping down decreases it.
 */
@Composable
private fun PlayerScreen(player: ExoPlayer, name: String, onBack: () -> Unit, casting: QastCastController, castStatus: String, playbackError: String?) {
    var showCastDevices by remember { mutableStateOf(false) }
    var castSetupError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(showCastDevices) {
        if (showCastDevices) {
            try { casting.initialise() }
            catch (ex: Exception) { castSetupError = ex.message ?: "Google Cast unavailable" }
        }
    }
    val context = LocalContext.current
    val activity = context as Activity
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    val initialBrightness = remember(activity) {
        activity.window.attributes.screenBrightness.takeIf { it >= 0f }
            ?: (Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, 128
            ) / 255f)
    }

    var brightness by remember { mutableFloatStateOf(initialBrightness.coerceIn(0.02f, 1f)) }
    var volume by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
        )
    }
    var gestureKind by remember { mutableStateOf<String?>(null) }
    var volumeAccumulator by remember { mutableFloatStateOf(volume) }
    var dragging by remember { mutableStateOf(false) }
    var gestureFinished by remember { mutableIntStateOf(0) }
    var videoHeightPx by remember { mutableIntStateOf(1) }

    // Brightness is per-window; leave the user's system brightness unchanged and
    // restore the window's previous override when leaving the playback screen.
    DisposableEffect(activity) {
        val previous = activity.window.attributes.screenBrightness
        onDispose {
            val params = activity.window.attributes
            params.screenBrightness = previous
            activity.window.attributes = params
        }
    }
    LaunchedEffect(gestureFinished) {
        if (gestureFinished > 0) {
            delay(1100)
            if (!dragging) gestureKind = null
        }
    }

    fun updateBrightness(deltaY: Float) {
        brightness = (brightness - deltaY / videoHeightPx).coerceIn(0.02f, 1f)
        val params = activity.window.attributes
        params.screenBrightness = brightness
        activity.window.attributes = params
    }

    fun updateVolume(deltaY: Float) {
        volumeAccumulator = (volumeAccumulator - deltaY / videoHeightPx).coerceIn(0f, 1f)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (volumeAccumulator * maxVolume).roundToInt().coerceIn(0, maxVolume),
            0
        )
        // Reflect the quantised device volume in the indicator.
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat()
    }

    if (showCastDevices) {
        AlertDialog(
            onDismissRequest = { showCastDevices = false },
            title = { Text("Cast to a device") },
            text = {
                Column {
                    Text("Select a Google Cast device on your Wi-Fi network. Open the device selector below.")
                    Spacer(Modifier.height(12.dp))
                    if (castSetupError != null) {
                        Text(castSetupError ?: "", color = MaterialTheme.colorScheme.error)
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                MediaRouteButton(ctx).apply {
                                    CastButtonFactory.setUpMediaRouteButton(ctx, this)
                                    contentDescription = "Open Google Cast receiver list"
                                }
                            },
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCastDevices = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = { casting.stop(); showCastDevices = false }) { Text("Stop casting") }
            }
        )
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Night).statusBarsPadding().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
            }
            Text(name, color = White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { showCastDevices = true }) {
                Icon(Icons.Default.Cast, contentDescription = "Choose casting device", tint = Ice)
            }
        }
        if (castStatus.isNotEmpty()) {
            Text(castStatus, color = Ice, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().background(Night).padding(horizontal = 15.dp, vertical = 4.dp))
        }
        if (playbackError != null) {
            Text(playbackError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().background(Night).padding(horizontal = 15.dp, vertical = 6.dp))
        }
        Box(
            modifier = Modifier.fillMaxSize().onSizeChanged {
                videoHeightPx = it.height.coerceAtLeast(1)
            }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        controllerShowTimeoutMs = 3500
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { it.player = player }
            )
            // Leave the central 44% unobstructed. Exclude the lowest 88dp
            // so seeking, navigation and the native timeline remain usable.
            Row(Modifier.fillMaxSize().padding(bottom = 88.dp)) {
                Box(
                    Modifier.fillMaxHeight().weight(0.28f)
                        .pointerInput(videoHeightPx) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    dragging = true
                                    gestureKind = "Brightness"
                                },
                                onVerticalDrag = { change, delta ->
                                    change.consume()
                                    updateBrightness(delta)
                                },
                                onDragEnd = {
                                    dragging = false
                                    gestureFinished++
                                },
                                onDragCancel = {
                                    dragging = false
                                    gestureFinished++
                                }
                            )
                        }
                )
                Spacer(Modifier.fillMaxHeight().weight(0.44f))
                Box(
                    Modifier.fillMaxHeight().weight(0.28f)
                        .pointerInput(videoHeightPx) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    dragging = true
                                    gestureKind = "Volume"
                                    volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) /
                                        maxVolume.toFloat()
                                    volumeAccumulator = volume
                                },
                                onVerticalDrag = { change, delta ->
                                    change.consume()
                                    updateVolume(delta)
                                },
                                onDragEnd = {
                                    dragging = false
                                    gestureFinished++
                                },
                                onDragCancel = {
                                    dragging = false
                                    gestureFinished++
                                }
                            )
                        }
                )
            }
            if (gestureKind != null) {
                Surface(
                    color = Night.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.width(140.dp).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (gestureKind == "Brightness") Icons.Default.Brightness6
                            else if (volume == 0f) Icons.Default.VolumeOff
                            else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Ice,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(gestureKind ?: "", color = White, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        val level = if (gestureKind == "Brightness") brightness else volume
                        LinearProgressIndicator(
                            progress = { level },
                            modifier = Modifier.fillMaxWidth(),
                            color = Ice,
                            trackColor = Muted.copy(alpha = 0.28f)
                        )
                        Spacer(Modifier.height(7.dp))
                        Text("${(level * 100).roundToInt()}%", color = White, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
