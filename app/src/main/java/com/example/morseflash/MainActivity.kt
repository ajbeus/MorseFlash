package com.example.morseflash

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MorseApp() }
    }
}

private enum class Output { Flashlight, Screen, Beep }

@Composable
private fun MorseApp() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var dark by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val isDark = dark ?: isSystemInDarkTheme()

    // Status/nav bar icons: light icons on the black splash and in dark mode.
    val view = LocalView.current
    val lightBars = !isDark && !showSplash
    SideEffect {
        WindowCompat.getInsetsController((view.context as Activity).window, view).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
    }

    Crossfade(targetState = showSplash, label = "splash") { splash ->
        if (splash) {
            SplashScreen(onDone = { showSplash = false })
        } else {
            MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MorseScreen(isDark = isDark, onDarkChange = { dark = it })
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 1600, easing = FastOutSlowInEasing))
        onDone()
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(R.drawable.splash_art),
            contentDescription = "Morse Flash",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
        )
        LinearProgressIndicator(
            progress = { progress.value },
            color = Color(0xFFF3E9D2),
            trackColor = Color.White.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 64.dp)
                .fillMaxWidth(0.5f)
                .height(4.dp),
        )
    }
}

@Composable
private fun MorseScreen(isDark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val window = (context as Activity).window
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val torchId = remember {
        cameraManager.cameraIdList.firstOrNull {
            val c = cameraManager.getCameraCharacteristics(it)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }

    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var wpm by remember { mutableFloatStateOf(10f) }
    var output by remember { mutableStateOf(if (torchId != null) Output.Flashlight else Output.Screen) }
    var job by remember { mutableStateOf<Job?>(null) }
    var lit by remember { mutableStateOf(false) }
    val sending = job?.isActive == true

    fun setTorch(on: Boolean) {
        torchId?.let { runCatching { cameraManager.setTorchMode(it, on) } }
    }

    fun setScreenMode(active: Boolean) {
        val attrs = window.attributes
        attrs.screenBrightness =
            if (active) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = attrs
        if (active) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun send() {
        val unitMs = (1200f / wpm).toLong()
        val pulses = Morse.pulses(text)
        val mode = output
        job = scope.launch {
            if (mode == Output.Screen) setScreenMode(true)
            try {
                if (mode == Output.Beep) {
                    Beeper.play(pulses, unitMs)
                } else {
                    for (p in pulses) {
                        if (mode == Output.Screen) lit = p.on else setTorch(p.on)
                        delay(p.units * unitMs)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                withContext(NonCancellable) {
                    setTorch(false)
                    lit = false
                    setScreenMode(false)
                }
                job = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            job?.cancel()
            setTorch(false)
            setScreenMode(false)
        }
    }

    Column(
        modifier = Modifier.safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Morse Flash", style = MaterialTheme.typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dark")
                Switch(checked = isDark, onCheckedChange = onDarkChange)
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !sending,
        )

        Text(
            Morse.encode(text).ifEmpty { "Morse appears here" },
            style = MaterialTheme.typography.titleLarge,
        )

        Text("Speed: ${wpm.toInt()} WPM")
        Slider(value = wpm, onValueChange = { wpm = it }, valueRange = 5f..25f, enabled = !sending)

        Text("Send with")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = output == Output.Flashlight,
                onClick = { output = Output.Flashlight },
                label = { Text("Flashlight") },
                enabled = torchId != null && !sending,
            )
            FilterChip(
                selected = output == Output.Screen,
                onClick = { output = Output.Screen },
                label = { Text("Screen") },
                enabled = !sending,
            )
            FilterChip(
                selected = output == Output.Beep,
                onClick = { output = Output.Beep },
                label = { Text("Beep") },
                enabled = !sending,
            )
        }
        if (torchId == null) {
            Text("No flashlight found on this device.", color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { if (sending) job?.cancel() else send() },
            enabled = Morse.encode(text).isNotEmpty() || sending,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    sending -> "Stop"
                    output == Output.Beep -> "Beep it"
                    else -> "Flash it"
                },
            )
        }
    }

    if (sending && output == Output.Screen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (lit) Color.White else Color.Black)
                .clickable { job?.cancel() },
        )
    }
}
