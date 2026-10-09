package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyDarkCharcoal
import com.example.ui.theme.SpotifyElevatedGrey
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenBright
import com.example.ui.theme.SpotifyLauncherTheme
import com.example.ui.theme.SpotifySubtextGrey
import com.example.ui.theme.SpotifyWhite
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

object BraveSpotifyLauncher {
    const val SPOTIFY_WEB_URL = "https://open.spotify.com/"
    val BRAVE_PACKAGES = listOf(
        "com.brave.browser",
        "com.brave.browser_beta",
        "com.brave.browser_nightly"
    )

    fun getInstalledBravePackage(pm: PackageManager): String? {
        return BRAVE_PACKAGES.firstOrNull { isPackageInstalled(pm, it) }
    }

    fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Meluncurkan Spotify di Brave Browser dengan:
     * 1. Mode Fullscreen / Auto-Hide URL Bar via Custom Tabs
     * 2. Tema gelap serasi (#121212) agar menyerupai aplikasi asli
     * 3. Perlindungan iklan agresif bawaan Brave Shields otomatis
     * 4. Paket dikunci ke Brave agar tidak terlempar ke app Spotify native
     */
    fun launchBrave(context: Context): LaunchOutcome {
        val uri = Uri.parse(SPOTIFY_WEB_URL)
        val pm = context.packageManager
        val bravePkg = getInstalledBravePackage(pm)

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(false)
            .setUrlBarHidingEnabled(true)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(0xFF121212.toInt())
                    .setSecondaryToolbarColor(0xFF181818.toInt())
                    .setNavigationBarColor(0xFF121212.toInt())
                    .build()
            )
            .build()

        customTabsIntent.intent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("android.support.customtabs.extra.ENABLE_URLBAR_HIDING", true)
            putExtra("org.chromium.chrome.browser.webapp_display_mode", 1) // Standalone Fullscreen
            putExtra("org.chromium.chrome.browser.webapp_url", SPOTIFY_WEB_URL)
            if (bravePkg != null) {
                setPackage(bravePkg)
            }
        }

        return try {
            customTabsIntent.launchUrl(context, uri)
            LaunchOutcome.Success(isBrave = bravePkg != null)
        } catch (_: Exception) {
            // Fallback via ACTION_VIEW
            try {
                val directIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    if (bravePkg != null) setPackage(bravePkg)
                }
                context.startActivity(directIntent)
                LaunchOutcome.Success(isBrave = bravePkg != null)
            } catch (e: Exception) {
                LaunchOutcome.Failed(e.localizedMessage ?: "Gagal membuka browser")
            }
        }
    }
}

sealed class LaunchOutcome {
    data class Success(val isBrave: Boolean) : LaunchOutcome()
    data class Failed(val reason: String) : LaunchOutcome()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpotifyLauncherTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isBraveInstalled by remember { mutableStateOf(false) }
    var autoLaunchOnStart by remember { mutableStateOf(false) }
    var showInAppPlayer by remember { mutableStateOf(false) }
    var hasAutoLaunched by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        isBraveInstalled = BraveSpotifyLauncher.getInstalledBravePackage(pm) != null
        if (autoLaunchOnStart && !hasAutoLaunched) {
            hasAutoLaunched = true
            BraveSpotifyLauncher.launchBrave(context)
        }
    }

    if (showInAppPlayer) {
        AggressiveAdBlockSpotifyWebView(
            url = BraveSpotifyLauncher.SPOTIFY_WEB_URL,
            onClose = { showInAppPlayer = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = SpotifyBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D2214), // Spotify green subtle ambience
                                SpotifyBlack,
                                SpotifyBlack
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Badge Fullscreen & Aggressive Adblock
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = SpotifyDarkCharcoal,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield Icon",
                                tint = SpotifyGreenBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Brave Shields Aggressive Ad-Block & Fullscreen",
                                color = SpotifyGreenBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Judul Aplikasi
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = SpotifyWhite,
                            fontSize = 32.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tampilan Fullscreen mirip app asli di Brave Browser",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SpotifySubtextGrey,
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // Button Utama: Sekali klik langsung buka Spotify di Brave
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(290.dp, 100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(270.dp, 80.dp)
                                .scale(pulseScale)
                                .clip(RoundedCornerShape(40.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x661DB954),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Button(
                            onClick = {
                                val outcome = BraveSpotifyLauncher.launchBrave(context)
                                scope.launch {
                                    when (outcome) {
                                        is LaunchOutcome.Success -> {
                                            if (outcome.isBrave) {
                                                snackbarHostState.showSnackbar("Membuka Spotify di Brave (Fullscreen + Ad-Block)...")
                                            } else {
                                                snackbarHostState.showSnackbar("Brave belum terpasang, membuka via browser default...")
                                            }
                                        }
                                        is LaunchOutcome.Failed -> {
                                            snackbarHostState.showSnackbar(outcome.reason)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .testTag("btn_buka_spotify")
                                .fillMaxWidth(0.9f)
                                .height(64.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    ambientColor = SpotifyGreen,
                                    spotColor = SpotifyGreen
                                ),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyGreen,
                                contentColor = SpotifyBlack
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Icon",
                                    tint = SpotifyBlack,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Buka Spotify di Brave",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpotifyBlack
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Status Panel & Pengaturan
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpotifyDarkCharcoal.copy(alpha = 0.6f))
                            .border(1.dp, SpotifyElevatedGrey, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        // Status Brave Terpasang
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isBraveInstalled) SpotifyGreen else Color(0xFFFF9800))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBraveInstalled) "Brave Browser Terdeteksi" else "Brave Belum Terpasang",
                                    color = SpotifyWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (!isBraveInstalled) {
                                OutlinedButton(
                                    onClick = {
                                        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.brave.browser"))
                                        try {
                                            context.startActivity(marketIntent)
                                        } catch (_: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.brave.browser")))
                                        }
                                    },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Install Brave", fontSize = 11.sp, color = SpotifyGreenBright)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch Auto-Launch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Auto-Launch saat App Dibuka",
                                    color = SpotifyWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Langsung buka Brave tanpa klik tombol",
                                    color = SpotifySubtextGrey,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = autoLaunchOnStart,
                                onCheckedChange = { autoLaunchOnStart = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SpotifyBlack,
                                    checkedTrackColor = SpotifyGreen,
                                    uncheckedThumbColor = SpotifySubtextGrey,
                                    uncheckedTrackColor = SpotifyElevatedGrey
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mode Cadangan: In-App Player dengan Built-in Aggressive Ad-Blocker
                        OutlinedButton(
                            onClick = { showInAppPlayer = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SpotifyGreenBright
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                modifier = Modifier.size(16.dp),
                                tint = SpotifyGreenBright
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buka Mode In-App Player (Alternatif Bebas Iklan)",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * In-App Fullscreen Web Player dengan Desktop UA dan Aggressive Ad-Blocking
 * untuk memblokir audio/video ad requests Spotify secara lokal.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AggressiveAdBlockSpotifyWebView(
    url: String,
    onClose: () -> Unit
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        // Desktop User-Agent agar bebas dari batasan mobile dan tidak memaksa buka aplikasi
                        userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webViewClient = object : WebViewClient() {
                        // Aggressive Ad Blocking: Memblokir domain dan endpoint iklan Spotify
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val requestUrl = request?.url?.toString()?.lowercase() ?: ""
                            val isAdRequest = listOf(
                                "doubleclick.net",
                                "googleads",
                                "pagead",
                                "audio-fa.spotifycdn.com",
                                "video-fa.spotifycdn.com",
                                "spclient.wg.spotify.com/ads",
                                "adeventtracker.spotify.com",
                                "crashdump.spotify.com",
                                "scorecardresearch",
                                "googletagmanager"
                            ).any { requestUrl.contains(it) }

                            if (isAdRequest) {
                                // Kembalikan respon kosong (blokir request iklan)
                                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // CSS Injection untuk menyembunyikan banner iklan atau promosi
                            val hideAdsCss = """
                                var style = document.createElement('style');
                                style.innerHTML = '[data-testid="ad-iframe"], .ad-container, [aria-label*="Advertisement"] { display: none !important; }';
                                document.head.appendChild(style);
                            """.trimIndent()
                            view?.evaluateJavascript(hideAdsCss, null)
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(url)
                    webViewInstance = this
                }
            }
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(SpotifyElevatedGrey.copy(alpha = 0.85f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali ke Launcher",
                tint = SpotifyWhite
            )
        }
    }
}
