package com.quickbill.barcodescanner

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import java.util.concurrent.Executors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quickbill.barcodescanner.ui.theme.QuickBillBarcodeScannerTheme
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import android.content.Intent
import android.net.Uri

private const val PREFS_NAME = "quickbill_scanner_settings"

private const val PREF_CAMERA = "camera_lens"
private const val PREF_TORCH = "torch_enabled"
private const val PREF_KEEP_SCREEN_AWAKE = "keep_screen_awake"

private const val SERVER_PORT = 8080

private const val FRAME_INTERVAL_MS = 125L

data class AppColors(
    val BrandAccent: Color,
    val Background: Color,
    val Surface: Color,
    val SurfaceLight: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val Border: Color,
    val Green: Color = Color(0xFF22C55E)
)

val LocalAppColors = androidx.compose.runtime.staticCompositionLocalOf<AppColors> {
    error("No AppColors provided")
}

private val BrandBlue: Color @Composable get() = LocalAppColors.current.BrandAccent
private val BrandBlueDark: Color @Composable get() = LocalAppColors.current.BrandAccent

private val Background: Color @Composable get() = LocalAppColors.current.Background
private val Surface: Color @Composable get() = LocalAppColors.current.Surface
private val SurfaceLight: Color @Composable get() = LocalAppColors.current.SurfaceLight
private val TextPrimary: Color @Composable get() = LocalAppColors.current.TextPrimary
private val TextSecondary: Color @Composable get() = LocalAppColors.current.TextSecondary
private val Green: Color @Composable get() = LocalAppColors.current.Green
private val Border: Color @Composable get() = LocalAppColors.current.Border

private enum class AppScreen {
    CAMERA,
    SETTINGS
}

private enum class CameraMode {
    BACK,
    FRONT
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemBars()

        setContent {
            QuickBillBarcodeScannerTheme {
                val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                
                var themeState by androidx.compose.runtime.remember { 
                    androidx.compose.runtime.mutableStateOf(preferences.getString("app_theme", "System") ?: "System") 
                }
                var accentState by androidx.compose.runtime.remember { 
                    androidx.compose.runtime.mutableStateOf(preferences.getString("app_accent", "Blue") ?: "Blue") 
                }
                
                val isDark = when(themeState) {
                    "Light" -> false
                    "Dark" -> true
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }
                
                val accentColor = when(accentState) {
                    "Green" -> Color(0xFF10B981)
                    "Orange" -> Color(0xFFF59E0B)
                    else -> Color(0xFF2563EB)
                }
                
                val appColors = if (isDark) {
                    AppColors(
                        BrandAccent = accentColor,
                        Background = Color(0xFF07111F),
                        Surface = Color(0xFF0F1B2D),
                        SurfaceLight = Color(0xFF16243A),
                        TextPrimary = Color(0xFFF8FAFC),
                        TextSecondary = Color(0xFF94A3B8),
                        Border = Color(0xFF24344D)
                    )
                } else {
                    AppColors(
                        BrandAccent = accentColor,
                        Background = Color(0xFFF8FAFC),
                        Surface = Color(0xFFFFFFFF),
                        SurfaceLight = Color(0xFFF1F5F9),
                        TextPrimary = Color(0xFF0F172A),
                        TextSecondary = Color(0xFF475569),
                        Border = Color(0xFFE2E8F0)
                    )
                }
                
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAppColors provides appColors
                ) {
                    QuickBillScannerApp(
                        activity = this@MainActivity,
                        currentTheme = themeState,
                        currentAccent = accentState,
                        onThemeChange = { 
                            themeState = it
                            preferences.edit().putString("app_theme", it).apply()
                        },
                        onAccentChange = { 
                            accentState = it
                            preferences.edit().putString("app_accent", it).apply()
                        }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.decorView.systemUiVisibility =
            (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
    }

    fun setKeepScreenAwake(enabled: Boolean) {

        if (enabled) {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
private fun QuickBillScannerApp(
    activity: MainActivity,
    currentTheme: String,
    currentAccent: String,
    onThemeChange: (String) -> Unit,
    onAccentChange: (String) -> Unit
) {

    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            cameraPermissionGranted = granted
        }

    var screen by remember {
        mutableStateOf(AppScreen.CAMERA)
    }

    var cameraMode by remember {

        mutableStateOf(
            if (
                preferences.getString(
                    PREF_CAMERA,
                    "BACK"
                ) == "FRONT"
            ) {
                CameraMode.FRONT
            } else {
                CameraMode.BACK
            }
        )
    }

    var torchEnabled by remember {

        mutableStateOf(
            preferences.getBoolean(
                PREF_TORCH,
                false
            )
        )
    }

    var autoStartServer by remember {
        mutableStateOf(preferences.getBoolean("auto_start_server", true))
    }

    var streamQuality by remember {
        mutableStateOf(preferences.getInt("stream_quality", 72))
    }

    var streamFps by remember {
        mutableStateOf(preferences.getInt("stream_fps", 12))
    }

    var keepScreenAwake by remember {

        mutableStateOf(
            preferences.getBoolean(
                PREF_KEEP_SCREEN_AWAKE,
                true
            )
        )
    }

    var serverRunning by remember {
        mutableStateOf(false)
    }

    var cameraReady by remember {
        mutableStateOf(false)
    }

    var streamUrl by remember {
        mutableStateOf("")
    }

    val server = remember {
        CameraStreamServer(SERVER_PORT)
    }

    val previewView = remember {
        PreviewView(context).apply {

            // Show the complete camera frame.
            // Do not crop the sides.
            scaleType =
                PreviewView.ScaleType.FIT_CENTER

            implementationMode =
                PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    var camera by remember {
        mutableStateOf<Camera?>(null)
    }

    val lifecycleOwner =
        LocalLifecycleOwner.current

    /*
     * Apply screen-awake preference immediately.
     */
    LaunchedEffect(keepScreenAwake) {

        activity.setKeepScreenAwake(
            keepScreenAwake
        )

        preferences.edit()
            .putBoolean(
                PREF_KEEP_SCREEN_AWAKE,
                keepScreenAwake
            )
            .apply()
    }

    /*
     * Request camera permission.
     */
    LaunchedEffect(Unit) {

        if (!cameraPermissionGranted) {

            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    /*
     * Start the camera HTTP server automatically.
     */
    LaunchedEffect(Unit) {

        streamUrl = buildStreamUrl(context)

        try {
            if (autoStartServer) {
                if (!server.isAlive) {
                    server.start()
                }
                serverRunning = true
            }
        } catch (_: Exception) {
            serverRunning = false
        }
    }

    LaunchedEffect(streamQuality, streamFps) {
        server.jpegQuality = streamQuality
        server.targetFps = streamFps
    }

    /*
     * Refresh the detected network address.
     *
     * The Android phone's IP is never manually entered.
     */
    LaunchedEffect(serverRunning) {
        if (serverRunning) {
            while (true) {
                val newUrl = buildStreamUrl(context)
                if (newUrl != streamUrl) {
                    streamUrl = newUrl
                }
                
                if (!server.isAlive) {
                    try {
                        server.start()
                    } catch (_: Exception) {}
                }

                kotlinx.coroutines.delay(5000)
            }
        }
    }



    /*
     * Start CameraX.
     */
    LaunchedEffect(
        cameraMode,
        screen,
        cameraPermissionGranted
    ) {

        if (
            screen != AppScreen.CAMERA ||
            !cameraPermissionGranted
        ) {
            return@LaunchedEffect
        }

        cameraReady = false

        startCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            cameraMode = cameraMode,
            server = server
        ) { newCamera ->

            camera = newCamera

            cameraReady = true

            if (
                newCamera.cameraInfo.hasFlashUnit()
            ) {

                newCamera.cameraControl
                    .enableTorch(
                        torchEnabled
                    )
            }
        }
    }

    /*
     * Apply torch state.
     */
    LaunchedEffect(
        torchEnabled,
        camera
    ) {

        camera?.let {

            if (
                it.cameraInfo.hasFlashUnit()
            ) {

                it.cameraControl.enableTorch(
                    torchEnabled
                )
            }
        }

        preferences.edit()
            .putBoolean(
                PREF_TORCH,
                torchEnabled
            )
            .apply()
    }

    /*
     * Stop server when Activity is destroyed.
     */
    DisposableEffect(Unit) {

        onDispose {

            try {
                server.stop()
            } catch (_: Exception) {
            }

            activity.setKeepScreenAwake(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        when (screen) {

            AppScreen.CAMERA -> {

                CameraScreen(
                    previewView = previewView,
                    torchEnabled = torchEnabled,
                    serverRunning = serverRunning,
                    cameraReady = cameraReady,
                    streamUrl = streamUrl,
                    onSettings = {
                        screen = AppScreen.SETTINGS
                    },
                    onTorch = {
                        torchEnabled =
                            !torchEnabled
                    }
                )
            }

            AppScreen.SETTINGS -> {
                BackHandler {
                    screen = AppScreen.CAMERA
                }

                SettingsScreen(
                    cameraMode = cameraMode,
                    torchEnabled = torchEnabled,
                    keepScreenAwake = keepScreenAwake,
                    serverRunning = serverRunning,
                    streamUrl = streamUrl,
                    currentTheme = currentTheme,
                    currentAccent = currentAccent,
                    autoStartServer = autoStartServer,
                    streamQuality = streamQuality,
                    streamFps = streamFps,
                    onBack = { screen = AppScreen.CAMERA },
                    onCameraChange = { mode -> 
                        cameraMode = mode
                        preferences.edit().putString(PREF_CAMERA, if (mode == CameraMode.FRONT) "FRONT" else "BACK").apply()
                    },
                    onTorchChange = { torchEnabled = it },
                    onKeepScreenAwakeChange = { keepScreenAwake = it },
                    onThemeChange = onThemeChange,
                    onAccentChange = onAccentChange,
                    onAutoStartChange = { autoStartServer = it; preferences.edit().putBoolean("auto_start_server", it).apply() },
                    onQualityChange = { streamQuality = it; preferences.edit().putInt("stream_quality", it).apply() },
                    onFpsChange = { streamFps = it; preferences.edit().putInt("stream_fps", it).apply() },
                    onRestartServer = {
                        try { server.stop() } catch (_: Exception) {}
                        try { server.start(); serverRunning = true; streamUrl = buildStreamUrl(context) } catch (_: Exception) { serverRunning = false }
                    },
                    onResetSettings = {
                        preferences.edit().clear().apply()
                        cameraMode = CameraMode.BACK
                        torchEnabled = false
                        keepScreenAwake = true
                        onThemeChange("System")
                        onAccentChange("Blue")
                        autoStartServer = true
                        streamQuality = 72
                        streamFps = 12
                    }
                )
            }
        }
    }
}

/* ============================================================
   CAMERA SCREEN
   ============================================================ */

@Composable
private fun CameraScreen(
    previewView: PreviewView,
    torchEnabled: Boolean,
    serverRunning: Boolean,
    cameraReady: Boolean,
    streamUrl: String,
    onSettings: () -> Unit,
    onTorch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Top App Bar Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.quickbill_logo_full),
                contentDescription = "QuickBill Barcode Scanner",
                modifier = Modifier.height(28.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom Info Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (serverRunning && cameraReady && streamUrl.isNotEmpty()) Green 
                            else Color(0xFFEF4444)
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when {
                        !cameraReady -> "STARTING CAMERA..."
                        !serverRunning -> "SERVER OFFLINE"
                        streamUrl.isEmpty() -> "NETWORK DISCONNECTED"
                        else -> "SCANNER READY"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (cameraReady) {
                    IconButton(
                        onClick = onTorch,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (torchEnabled) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (torchEnabled) Color(0xFFFBBF24) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (streamUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = streamUrl,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Use the phone camera as a wireless barcode scanner for QuickBill Desktop.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

/* ============================================================
   SETTINGS SCREEN
   ============================================================ */

@Composable
private fun SettingsScreen(
    cameraMode: CameraMode,
    torchEnabled: Boolean,
    keepScreenAwake: Boolean,
    serverRunning: Boolean,
    streamUrl: String,
    currentTheme: String,
    currentAccent: String,
    autoStartServer: Boolean,
    streamQuality: Int,
    streamFps: Int,
    onBack: () -> Unit,
    onCameraChange: (CameraMode) -> Unit,
    onTorchChange: (Boolean) -> Unit,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onAccentChange: (String) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onQualityChange: (Int) -> Unit,
    onFpsChange: (Int) -> Unit,
    onRestartServer: () -> Unit,
    onResetSettings: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Professional App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        DividerLine()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            
            // GENERAL
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(text = "GENERAL", modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingActionRow(
                icon = Icons.Default.Settings,
                title = "Theme",
                subtitle = currentTheme,
                onClick = { 
                    val next = when(currentTheme) {
                        "System" -> "Light"
                        "Light" -> "Dark"
                        else -> "System"
                    }
                    onThemeChange(next)
                }
            )
            DividerLine()
            SettingActionRow(
                icon = Icons.Default.Settings,
                title = "Accent Color",
                subtitle = currentAccent,
                onClick = { 
                    val next = when(currentAccent) {
                        "Blue" -> "Green"
                        "Green" -> "Orange"
                        else -> "Blue"
                    }
                    onAccentChange(next)
                }
            )
            DividerLine()
            SettingSwitchRow(
                icon = Icons.Default.Videocam,
                title = "Keep Screen Awake",
                subtitle = "Prevent the display from sleeping",
                checked = keepScreenAwake,
                onCheckedChange = onKeepScreenAwakeChange
            )

            // CAMERA
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(text = "CAMERA", modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            CameraOption(
                title = "Back Camera",
                subtitle = "Recommended for barcode scanning",
                selected = cameraMode == CameraMode.BACK,
                onClick = { onCameraChange(CameraMode.BACK) }
            )
            DividerLine()
            CameraOption(
                title = "Front Camera",
                subtitle = "Use the front-facing camera",
                selected = cameraMode == CameraMode.FRONT,
                onClick = { onCameraChange(CameraMode.FRONT) }
            )
            DividerLine()
            SettingSwitchRow(
                icon = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                title = "Camera Torch",
                subtitle = if (torchEnabled) "Torch is enabled" else "Torch is disabled",
                checked = torchEnabled,
                onCheckedChange = onTorchChange
            )

            // STREAMING
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(text = "STREAMING", modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingSwitchRow(
                icon = Icons.Default.Settings,
                title = "Auto-start Server",
                subtitle = "Start server when app opens",
                checked = autoStartServer,
                onCheckedChange = onAutoStartChange
            )
            DividerLine()
            SettingActionRow(
                icon = Icons.Default.Settings,
                title = "Stream FPS",
                subtitle = "$streamFps fps",
                onClick = { 
                    val next = when(streamFps) {
                        12 -> 24
                        24 -> 30
                        30 -> 8
                        else -> 12
                    }
                    onFpsChange(next)
                }
            )
            DividerLine()
            SettingActionRow(
                icon = Icons.Default.Settings,
                title = "Stream Quality",
                subtitle = "$streamQuality%",
                onClick = { 
                    val next = when(streamQuality) {
                        72 -> 90
                        90 -> 50
                        else -> 72
                    }
                    onQualityChange(next)
                }
            )

            // NETWORK
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(text = "NETWORK", modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingInfoRow(
                icon = Icons.Default.Wifi,
                title = "Network Server",
                subtitle = if (serverRunning) "Running on port $SERVER_PORT" else "Server stopped",
                status = if (serverRunning) "READY" else "OFF"
            )
            
            if (streamUrl.isNotEmpty()) {
                DividerLine()
                SettingActionRow(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy Stream URL",
                    subtitle = streamUrl,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("QuickBill Stream URL", streamUrl))
                    }
                )
            }
            DividerLine()
            SettingActionRow(
                icon = Icons.Default.Settings,
                title = "Restart Camera Server",
                subtitle = "Restart if connection is dropped",
                onClick = onRestartServer
            )

            // ABOUT
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.quickbill_logo_full),
                    contentDescription = "QuickBill",
                    modifier = Modifier.height(32.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "See Documentation",
                    color = Color(0xFF2563EB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://quickbill.kartikbansode.dev/documentation")
                        )
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Version 1.1.0",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy",
                        color = Color(0xFF2563EB),
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://quickbill.kartikbansode.dev/privacy")
                            )
                            context.startActivity(intent)
                        }
                    )

                    Text(
                        text = "  •  ",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "Terms",
                        color = Color(0xFF2563EB),
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://quickbill.kartikbansode.dev/terms")
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onResetSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Reset Default Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

/* ============================================================
   SETTINGS COMPONENTS
   ============================================================ */

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .background(SurfaceLight),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier =
                    Modifier.size(23.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.width(14.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange =
                onCheckedChange
        )
    }
}

@Composable
private fun SettingInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    status: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .background(SurfaceLight),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier =
                    Modifier.size(23.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.width(14.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Text(
            text = status,
            color =
                if (status == "READY") {
                    Green
                } else {
                    Color(0xFFEF4444)
                },
            fontSize = 11.sp,
            fontWeight =
                FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}


@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.8.sp,
        modifier = modifier
    )
}

@Composable
private fun CameraOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border)
    )
}

/* ============================================================
   CAMERA
   ============================================================ */

private val analysisExecutor = Executors.newSingleThreadExecutor()

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraMode: CameraMode,
    server: CameraStreamServer,
    onCameraReady: (Camera) -> Unit
) {

    val providerFuture =
        ProcessCameraProvider.getInstance(
            context
        )

    providerFuture.addListener({

        try {

            val provider =
                providerFuture.get()

            provider.unbindAll()

            val selector =
                if (
                    cameraMode ==
                    CameraMode.FRONT
                ) {

                    CameraSelector.DEFAULT_FRONT_CAMERA

                } else {

                    CameraSelector.DEFAULT_BACK_CAMERA
                }

            if (
                !provider.hasCamera(selector)
            ) {
                return@addListener
            }

            /*
             * 1280x720 is enough for barcode scanning
             * and avoids unnecessarily high camera load.
             */
            val preview =
                Preview.Builder()
                    .setTargetResolution(
                        android.util.Size(
                            1280,
                            720
                        )
                    )
                    .build()

            preview.setSurfaceProvider(
                previewView.surfaceProvider
            )

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                try {
                    val bitmap = imageProxy.toBitmap()
                    server.updateFrame(bitmap)
                } catch (_: Exception) {
                } finally {
                    imageProxy.close()
                }
            }

            val newCamera =
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )

            onCameraReady(
                newCamera
            )

        } catch (_: Exception) {
        }

    }, ContextCompat.getMainExecutor(context))
}

/* ============================================================
   NETWORK
   ============================================================ */

private fun buildStreamUrl(
    context: Context
): String {

    val ip =
        getLocalIpAddress(context)

    return if (ip.isNotEmpty()) {

        "http://$ip:$SERVER_PORT/video"

    } else {

        ""
    }
}

private fun getLocalIpAddress(
    context: Context
): String {

    /*
     * First try NetworkInterface, prioritizing wifi interfaces.
     */
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        var fallbackIp = ""

        for (networkInterface in interfaces) {
            if (!networkInterface.isUp || networkInterface.isLoopback) continue

            val name = networkInterface.name.lowercase()
            val isWifi = name.contains("wlan") || name.contains("ap") || name.contains("wifi")
            val isCellular = name.contains("rmnet") || name.contains("pdp") || name.contains("ccmni")

            if (isCellular) continue

            val addresses = Collections.list(networkInterface.inetAddresses)
            for (address in addresses) {
                if (address is Inet4Address && !address.isLoopbackAddress) {
                    val ip = address.hostAddress
                    if (ip != null && ip.isNotEmpty()) {
                        if (isWifi) {
                            return ip // Strongly prefer wifi
                        } else if (fallbackIp.isEmpty()) {
                            fallbackIp = ip
                        }
                    }
                }
            }
        }
        
        if (fallbackIp.isNotEmpty()) {
            return fallbackIp
        }
    } catch (_: Exception) {}

    /*
     * Fallback to ConnectivityManager.
     */
    try {

        val connectivityManager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            connectivityManager.activeNetwork
                ?: return ""

        val linkProperties =
            connectivityManager
                .getLinkProperties(network)
                ?: return ""

        for (
        linkAddress: LinkAddress
        in linkProperties.linkAddresses
        ) {

            val address =
                linkAddress.address

            if (
                address is Inet4Address &&
                !address.isLoopbackAddress
            ) {

                return address.hostAddress
                    ?: ""
            }
        }

    } catch (_: Exception) {
    }

    return ""
}