package sg.qast.player

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
    DisposableEffect(player) { onDispose { player.release() } }

    var page by remember { mutableStateOf(Page.Home) }
    var current by remember { mutableStateOf<Uri?>(null) }
    var currentName by remember { mutableStateOf("Video") }
    val recent = remember { mutableStateListOf<Pair<String, Uri>>() }

    fun play(name: String, uri: Uri) {
        currentName = name
        current = uri
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
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
                onCast = { /* Casting will be wired to Google Cast and DLNA in v0.3. */ }
            )
        } else {
            Scaffold(
                containerColor = Night,
                topBar = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color.White, shape = RoundedCornerShape(11.dp)) {
                                Image(
                                    painter = painterResource(R.drawable.qast_mark),
                                    contentDescription = "Qast play logo",
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("ast", fontSize = 30.sp, fontWeight = FontWeight.Light,
                                letterSpacing = 3.sp, color = White)
                            Spacer(Modifier.width(3.dp))
                            Text("◔", color = Ice, fontSize = 17.sp, modifier = Modifier.offset(y = (-13).dp))
                        }
                        Text("ALPHA 0.1", color = Muted, fontSize = 10.sp)
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
                            SettingsLine("Version", "0.1.1-alpha")
                            SettingsLine("Playback engine", "AndroidX Media3")
                            SettingsLine("Cast", "Planned: Google Cast + DLNA")
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

@Composable
private fun PlayerScreen(player: ExoPlayer, name: String, onBack: () -> Unit, onCast: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Night).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
            }
            Text(name, color = White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onCast, enabled = false) {
                Icon(Icons.Default.Cast, contentDescription = "Cast (coming soon)", tint = Muted)
            }
        }
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
    }
}
