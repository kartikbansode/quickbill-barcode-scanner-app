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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
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

private const val PREFS_NAME = "quickbill_scanner_settings"

private const val PREF_CAMERA = "camera_lens"
private const val PREF_TORCH = "torch_enabled"
private const val PREF_KEEP_SCREEN_AWAKE = "keep_screen_awake"

private const val SERVER_PORT = 8080

private const val FRAME_INTERVAL_MS = 125L

private val BrandBlue = Color(0xFF2563EB)
private val BrandBlueDark = Color(0xFF1D4ED8)

private val Background = Color(0xFF07111F)
private val Surface = Color(0xFF0F1B2D)
private val SurfaceLight = Color(0xFF16243A)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)
private val Green = Color(0xFF22C55E)
private val Border = Color(0xFF24344D)

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
                QuickBillScannerApp(
                    activity = this
                )
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
    activity: MainActivity
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

            if (!server.isAlive) {
                server.start()
            }

            serverRunning = true

        } catch (_: Exception) {

            serverRunning = false
        }
    }

    /*
     * Refresh the detected network address.
     *
     * The Android phone's IP is never manually entered.
     */
    LaunchedEffect(serverRunning) {

        if (serverRunning) {

            while (true) {

                val newUrl =
                    buildStreamUrl(context)

                if (
                    newUrl != streamUrl &&
                    newUrl.isNotEmpty()
                ) {
                    streamUrl = newUrl
                }

                kotlinx.coroutines.delay(5000)
            }
        }
    }

    /*
     * Capture frames from the camera preview and
     * provide them to the MJPEG server.
     *
     * The interval is intentionally limited to reduce
     * CPU usage and battery consumption.
     */
    DisposableEffect(
        previewView,
        server,
        serverRunning,
        cameraReady
    ) {

        val handler =
            Handler(Looper.getMainLooper())

        val frameRunnable =
            object : Runnable {

                override fun run() {

                    if (
                        serverRunning &&
                        cameraReady
                    ) {

                        try {

                            val bitmap =
                                previewView.bitmap

                            if (
                                bitmap != null &&
                                !bitmap.isRecycled
                            ) {

                                server.updateFrame(
                                    bitmap
                                )
                            }

                        } catch (_: Exception) {
                        }
                    }

                    handler.postDelayed(
                        this,
                        FRAME_INTERVAL_MS
                    )
                }
            }

        handler.post(frameRunnable)

        onDispose {

            handler.removeCallbacks(
                frameRunnable
            )
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
            cameraMode = cameraMode
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

                SettingsScreen(
                    cameraMode = cameraMode,
                    torchEnabled = torchEnabled,
                    keepScreenAwake = keepScreenAwake,
                    serverRunning = serverRunning,
                    streamUrl = streamUrl,

                    onBack = {
                        screen = AppScreen.CAMERA
                    },

                    onCameraChange = { mode ->

                        cameraMode = mode

                        preferences.edit()
                            .putString(
                                PREF_CAMERA,
                                if (
                                    mode ==
                                    CameraMode.FRONT
                                ) {
                                    "FRONT"
                                } else {
                                    "BACK"
                                }
                            )
                            .apply()
                    },

                    onTorchChange = {
                        torchEnabled = it
                    },

                    onKeepScreenAwakeChange = {
                        keepScreenAwake = it
                    },

                    onRestartServer = {

                        try {
                            server.stop()
                        } catch (_: Exception) {
                        }

                        try {

                            server.start()

                            serverRunning = true
                            streamUrl =
                                buildStreamUrl(context)

                        } catch (_: Exception) {

                            serverRunning = false
                        }
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
        modifier = Modifier.fillMaxSize()
    ) {

        AndroidView(
            factory = {
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        /*
         * Top overlay.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .background(
                    Color.Black.copy(
                        alpha = 0.62f
                    )
                )
                .align(Alignment.TopCenter)
        )

        /*
         * Bottom overlay.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Color.Black.copy(
                        alpha = 0.72f
                    )
                )
                .align(Alignment.BottomCenter)
        )

        /*
         * Branding.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    top = 22.dp,
                    end = 18.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(BrandBlue),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier =
                        Modifier.size(27.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "QuickBill",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = "BARCODE SCANNER",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight =
                        FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            IconButton(
                onClick = onSettings
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Settings,
                    contentDescription =
                        "Settings",
                    tint = Color.White,
                    modifier =
                        Modifier.size(28.dp)
                )
            }
        }

        /*
         * Live status.
         */
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 22.dp,
                    bottom = 88.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (
                            serverRunning &&
                            cameraReady
                        ) {
                            Green
                        } else {
                            Color(0xFFEF4444)
                        }
                    )
            )

            Spacer(
                modifier = Modifier.width(9.dp)
            )

            Text(
                text =
                    when {

                        !cameraReady ->
                            "STARTING CAMERA"

                        !serverRunning ->
                            "SERVER OFFLINE"

                        else ->
                            "LIVE CAMERA"
                    },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        /*
         * Stream URL.
         */
        if (streamUrl.isNotEmpty()) {

            Text(
                text = streamUrl,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 22.dp,
                        bottom = 55.dp
                    ),
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp
            )
        }

        /*
         * Torch.
         */
        if (cameraReady) {

            IconButton(
                onClick = onTorch,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 20.dp,
                        bottom = 54.dp
                    )
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(
                            alpha = 0.12f
                        )
                    )
            ) {

                Icon(
                    imageVector =
                        if (torchEnabled) {
                            Icons.Default.FlashOn
                        } else {
                            Icons.Default.FlashOff
                        },
                    contentDescription =
                        "Torch",
                    tint =
                        if (torchEnabled) {
                            Color(0xFFFBBF24)
                        } else {
                            Color.White
                        },
                    modifier =
                        Modifier.size(28.dp)
                )
            }
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
    onBack: () -> Unit,
    onCameraChange: (CameraMode) -> Unit,
    onTorchChange: (Boolean) -> Unit,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    onRestartServer: () -> Unit
) {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(
                rememberScrollState()
            )
            .navigationBarsPadding()
            .padding(
                horizontal = 22.dp,
                vertical = 18.dp
            )
    ) {

        /*
         * Header.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,
                    contentDescription =
                        "Back",
                    tint = Color.White
                )
            }

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "QuickBill Barcode Scanner",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        /* CAMERA */

        SectionTitle("CAMERA")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            CameraOption(
                title = "Back Camera",
                subtitle =
                    "Recommended for barcode scanning",
                selected =
                    cameraMode ==
                            CameraMode.BACK,
                onClick = {
                    onCameraChange(
                        CameraMode.BACK
                    )
                }
            )

            DividerLine()

            CameraOption(
                title = "Front Camera",
                subtitle =
                    "Use the front-facing camera",
                selected =
                    cameraMode ==
                            CameraMode.FRONT,
                onClick = {
                    onCameraChange(
                        CameraMode.FRONT
                    )
                }
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        /* DEVICE */

        SectionTitle("DEVICE")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            SettingSwitchRow(
                icon = Icons.Default.Videocam,
                title = "Keep Screen Awake",
                subtitle =
                    "Prevent the display from sleeping while scanning",
                checked = keepScreenAwake,
                onCheckedChange =
                    onKeepScreenAwakeChange
            )

            DividerLine()

            SettingInfoRow(
                icon = Icons.Default.Wifi,
                title = "Network Camera",
                subtitle =
                    if (serverRunning) {
                        "Camera server is running"
                    } else {
                        "Camera server is stopped"
                    },
                status =
                    if (serverRunning) {
                        "READY"
                    } else {
                        "OFF"
                    }
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        /* TORCH */

        SectionTitle("TORCH")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            SettingSwitchRow(
                icon =
                    if (torchEnabled) {
                        Icons.Default.FlashOn
                    } else {
                        Icons.Default.FlashOff
                    },
                title = "Camera Torch",
                subtitle =
                    if (torchEnabled) {
                        "Torch is enabled"
                    } else {
                        "Torch is disabled"
                    },
                checked = torchEnabled,
                onCheckedChange =
                    onTorchChange
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        /* NETWORK CAMERA */

        SectionTitle("NETWORK CAMERA")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (serverRunning) {
                                    Green
                                } else {
                                    Color(0xFFEF4444)
                                }
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )

                    Text(
                        text =
                            if (serverRunning) {
                                "SERVER RUNNING"
                            } else {
                                "SERVER STOPPED"
                            },
                        color =
                            if (serverRunning) {
                                Green
                            } else {
                                Color(0xFFEF4444)
                            },
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                Text(
                    text = "Stream URL",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(12.dp)
                        )
                        .background(
                            Color(0xFF091524)
                        )
                        .border(
                            1.dp,
                            Border,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {

                    Text(
                        text =
                            if (
                                streamUrl.isNotEmpty()
                            ) {
                                streamUrl
                            } else {
                                "Detecting device IP..."
                            },
                        color =
                            Color(0xFFBFDBFE),
                        fontSize = 14.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                Button(
                    onClick = {

                        if (
                            streamUrl.isNotEmpty()
                        ) {

                            val clipboard =
                                context.getSystemService(
                                    Context.CLIPBOARD_SERVICE
                                ) as ClipboardManager

                            clipboard.setPrimaryClip(
                                android.content.ClipData
                                    .newPlainText(
                                        "QuickBill Stream URL",
                                        streamUrl
                                    )
                            )
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                BrandBlue
                        ),
                    shape =
                        RoundedCornerShape(12.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ContentCopy,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(19.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text(
                        text = "COPY STREAM URL",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedButton(
                    onClick =
                        onRestartServer,
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text =
                            "RESTART CAMERA SERVER",
                        color = Color.White,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        /* HOW TO CONNECT */

        SectionTitle("HOW TO CONNECT")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {

                InstructionRow(
                    number = "1",
                    text =
                        "Keep this phone and the QuickBill computer on the same Wi-Fi network."
                )

                InstructionRow(
                    number = "2",
                    text =
                        "Copy the Stream URL shown above."
                )

                InstructionRow(
                    number = "3",
                    text =
                        "Paste the URL into QuickBill Desktop camera settings."
                )

                InstructionRow(
                    number = "4",
                    text =
                        "QuickBill Desktop will read the camera stream and scan barcodes."
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        /* ABOUT */

        SectionTitle("ABOUT")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(
                            RoundedCornerShape(13.dp)
                        )
                        .background(
                            BrandBlue
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Videocam,
                        contentDescription =
                            null,
                        tint = Color.White
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(14.dp)
                )

                Column {

                    Text(
                        text =
                            "QuickBill - Barcode Scanner",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Network camera companion",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "Version 1.0.0",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )
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
private fun SectionTitle(
    text: String
) {

    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.8.sp
    )
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(17.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Surface
            ),
        border =
            BorderStroke(
                1.dp,
                Border
            )
    ) {

        Column(
            modifier =
                Modifier.fillMaxWidth(),
            content = content
        )
    }
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
            .clip(
                RoundedCornerShape(12.dp)
            )
            .clickable(
                onClick = onClick
            )
            .background(
                if (selected) {
                    BrandBlue.copy(
                        alpha = 0.10f
                    )
                } else {
                    Color.Transparent
                }
            )
            .padding(17.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Spacer(
            modifier =
                Modifier.width(10.dp)
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

@Composable
private fun InstructionRow(
    number: String,
    text: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 9.dp
            ),
        verticalAlignment =
            Alignment.Top
    ) {

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    BrandBlue.copy(
                        alpha = 0.18f
                    )
                ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = number,
                color =
                    Color(0xFF93C5FD),
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.width(12.dp)
        )

        Text(
            text = text,
            modifier =
                Modifier.weight(1f),
            color =
                Color(0xFFCBD5E1),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

/* ============================================================
   CAMERA
   ============================================================ */

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraMode: CameraMode,
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

            val newCamera =
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview
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
     * First try NetworkInterface.
     */
    try {

        val interfaces =
            Collections.list(
                NetworkInterface
                    .getNetworkInterfaces()
            )

        for (
        networkInterface
        in interfaces
        ) {

            if (
                !networkInterface.isUp ||
                networkInterface.isLoopback
            ) {
                continue
            }

            val addresses =
                Collections.list(
                    networkInterface.inetAddresses
                )

            for (
            address
            in addresses
            ) {

                if (
                    address is Inet4Address &&
                    !address.isLoopbackAddress
                ) {

                    val ip =
                        address.hostAddress

                    if (
                        ip != null &&
                        ip.isNotEmpty()
                    ) {

                        return ip
                    }
                }
            }
        }

    } catch (_: Exception) {
    }

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