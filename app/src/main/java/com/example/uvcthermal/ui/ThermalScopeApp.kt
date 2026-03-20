package com.example.uvcthermal.ui

import android.Manifest
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.content.pm.PackageManager
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uvcthermal.camera.ThermalCameraFragment
import com.example.uvcthermal.camera.ThermalCameraUiState
import com.example.uvcthermal.camera.ThermalCameraViewModel

@Composable
fun ThermalScopeApp(
    viewModel: ThermalCameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(hasCameraPermission) {
        viewModel.onPermissionUpdated(hasCameraPermission)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF05090D))
        ) {
            if (hasCameraPermission) {
                ThermalCameraBridge(activity = activity)
            }

            ThermalScopeScreen(
                uiState = uiState,
                hasCameraPermission = hasCameraPermission,
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        hasCameraPermission = true
                    }
                },
                onPaletteToggle = viewModel::cyclePalette,
                onSuperResolutionToggle = viewModel::toggleDetailBoost,
                onCameraPowerToggle = viewModel::toggleCameraPower
            )
        }
    }
}

@Composable
private fun ThermalCameraBridge(activity: FragmentActivity) {
    val bridgeId = remember { android.view.View.generateViewId() }
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = bridgeId
                layoutParams = ViewGroup.LayoutParams(1, 1)
            }
        },
        update = {
            if (activity.supportFragmentManager.findFragmentByTag(ThermalCameraFragment.TAG) == null) {
                activity.supportFragmentManager.beginTransaction()
                    .replace(it.id, ThermalCameraFragment(), ThermalCameraFragment.TAG)
                    .commitNowAllowingStateLoss()
            }
        }
    )

    DisposableEffect(activity) {
        onDispose {
            activity.supportFragmentManager.findFragmentByTag(ThermalCameraFragment.TAG)?.let { fragment ->
                activity.supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
    }
}

@Composable
private fun ThermalScopeScreen(
    uiState: ThermalCameraUiState,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPaletteToggle: () -> Unit,
    onSuperResolutionToggle: () -> Unit,
    onCameraPowerToggle: () -> Unit
) {
    val safePadding = WindowInsets.systemBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        ThermalFeedSurface(
            uiState = uiState,
            hasCameraPermission = hasCameraPermission,
            onRequestPermission = onRequestPermission
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xC407111B),
                            Color.Transparent,
                            Color.Transparent,
                            Color(0xD905090D)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 18.dp,
                    top = safePadding.calculateTopPadding() + 12.dp,
                    end = 18.dp,
                    bottom = safePadding.calculateBottomPadding() + 18.dp
                )
        ) {
            TopOverlay(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
                uiState = uiState,
                onPaletteToggle = onPaletteToggle,
                onSuperResolutionToggle = onSuperResolutionToggle,
                onCameraPowerToggle = onCameraPowerToggle
            )
            BottomOverlay(
                modifier = Modifier.align(Alignment.BottomStart),
                uiState = uiState
            )
        }
    }
}

@Composable
private fun ThermalFeedSurface(
    uiState: ThermalCameraUiState,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val preview = uiState.previewFrame
    if (preview != null) {
        val imageModifier = Modifier
            .fillMaxSize()
            .then(detailBoostModifier(uiState))
        Image(
            bitmap = preview.bitmap.asImageBitmap(),
            contentDescription = "Thermal camera feed",
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
    } else {
        EmptyPreviewState(
            hasCameraPermission = hasCameraPermission,
            onRequestPermission = onRequestPermission
        )
    }
}

@Composable
private fun TopOverlay(
    modifier: Modifier = Modifier,
    uiState: ThermalCameraUiState,
    onPaletteToggle: () -> Unit,
    onSuperResolutionToggle: () -> Unit,
    onCameraPowerToggle: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            StatusBadge(isStreaming = uiState.isStreaming)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PaletteCycleButton(
                    paletteLabel = uiState.selectedPalette.title,
                    paletteColor = Color(uiState.selectedPalette.accentColor),
                    onClick = onPaletteToggle
                )
                SuperResolutionButton(
                    isEnabled = uiState.isDetailBoostEnabled,
                    onClick = onSuperResolutionToggle
                )
                CameraPowerButton(
                    isCameraEnabled = uiState.isCameraEnabled,
                    onClick = onCameraPowerToggle
                )
            }
        }

        if (!uiState.isCameraEnabled || !uiState.isStreaming) {
            Text(
                text = uiState.statusTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF8F2E7)
            )
            Text(
                text = uiState.statusDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xD8E2EAF0),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BottomOverlay(
    modifier: Modifier = Modifier,
    uiState: ThermalCameraUiState
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricPill(label = "Resolution", value = uiState.resolutionLabel)
            MetricPill(label = "Frame Rate", value = uiState.fpsLabel)
            MetricPill(label = "Palette", value = uiState.selectedPalette.title)
        }
    }
}

@Composable
private fun PaletteCycleButton(
    paletteLabel: String,
    paletteColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = paletteColor,
            contentColor = Color(0xFF091118)
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.Cameraswitch,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = paletteLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Cycle Palette",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun SuperResolutionButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isEnabled) Color(0xFFFFC061) else Color(0xCC101925)
    val contentColor = if (isEnabled) Color(0xFF091118) else Color(0xFFF7F1E7)
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = if (isEnabled) "Detail Boost On" else "Detail Boost Off",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CameraPowerButton(
    isCameraEnabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isCameraEnabled) Color(0xFF78F0A7) else Color(0xCC101925)
    val contentColor = if (isCameraEnabled) Color(0xFF091118) else Color(0xFFF7F1E7)
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = if (isCameraEnabled) "Camera On" else "Camera Off",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isCameraEnabled) "Tap To Close" else "Tap To Resume",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun EmptyPreviewState(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B1218),
                        Color(0xFF141D24),
                        Color(0xFF06090D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0x1FFFFFFF)
            ) {
                Icon(
                    imageVector = if (hasCameraPermission) Icons.Rounded.CameraAlt else Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(18.dp),
                    tint = Color(0xFFF7F1E7)
                )
            }
            Text(
                text = if (hasCameraPermission) {
                    "Connect your thermal camera"
                } else {
                    "Enable camera permission"
                },
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFF7F1E7),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (hasCameraPermission) {
                    "Attach the UVC thermal camera through OTG. The feed will take over the full screen when the stream starts."
                } else {
                    "Android requires camera permission before the external UVC stream can open."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBBC7D2)
            )
            if (!hasCameraPermission) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = onRequestPermission) {
                    Icon(imageVector = Icons.Rounded.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isStreaming: Boolean) {
    val tone = if (isStreaming) Color(0xFF78F0A7) else Color(0xFFFFC061)
    val label = if (isStreaming) "Streaming" else "Standby"
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0x33000000)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(tone, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFF7F1E7)
            )
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Surface(
        color = Color(0x66101925),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = Color(0xFFA7B5C0),
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFF7F1E7)
            )
        }
    }
}

@Composable
private fun detailBoostModifier(uiState: ThermalCameraUiState): Modifier {
    if (!uiState.isDetailBoostEnabled || !uiState.isStreaming || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return Modifier
    }
    val shader = rememberCrispShader()
    val renderEffect = remember(uiState.previewFrame?.sequence) {
        AndroidRenderEffect.createRuntimeShaderEffect(shader, "image")
    }
    return Modifier.graphicsLayer {
        this.renderEffect = renderEffect.asComposeRenderEffect()
    }
}

@Composable
private fun rememberCrispShader(): RuntimeShader {
    val shader = remember { RuntimeShader(CRISP_SHADER_SRC) }
    shader.setFloatUniform("uSharpen", 1.05f)
    shader.setFloatUniform("uContrast", 1.08f)
    shader.setFloatUniform("uSaturation", 1.03f)
    return shader
}

private const val CRISP_SHADER_SRC = """
uniform shader image;
uniform float uSharpen;
uniform float uContrast;
uniform float uSaturation;

half4 main(float2 coord) {
    half4 centerSample = image.eval(coord);
    half3 center = centerSample.rgb;
    half3 left = image.eval(coord + float2(-1.0, 0.0)).rgb;
    half3 right = image.eval(coord + float2(1.0, 0.0)).rgb;
    half3 up = image.eval(coord + float2(0.0, -1.0)).rgb;
    half3 down = image.eval(coord + float2(0.0, 1.0)).rgb;
    half3 blur = (center * 4.0 + left + right + up + down) / 8.0;
    half3 sharpened = clamp(center + ((center - blur) * uSharpen), 0.0, 1.0);

    half luminance = dot(sharpened, half3(0.299, 0.587, 0.114));
    half3 contrasted = clamp(((sharpened - 0.5) * uContrast) + 0.5, 0.0, 1.0);
    half3 saturated = clamp(mix(half3(luminance), contrasted, uSaturation), 0.0, 1.0);
    return half4(saturated, centerSample.a);
}
"""
