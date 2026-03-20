package com.example.uvcthermal.camera

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ThermalPreviewFrame(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val sequence: Long
)

data class ThermalCameraUiState(
    val selectedPalette: ThermalPalette = ThermalPalette.BLACK_HOT,
    val statusTitle: String = "Camera Permission Required",
    val statusDetail: String = "Grant camera access to initialize the UVC thermal stream.",
    val resolutionLabel: String = "--",
    val fpsLabel: String = "--",
    val isDetailBoostEnabled: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val isStreaming: Boolean = false,
    val previewFrame: ThermalPreviewFrame? = null
)

class ThermalCameraViewModel : ViewModel() {
    sealed interface CameraCommand {
        data object Enable : CameraCommand
        data object Disable : CameraCommand
    }

    private data class FramePacket(
        val data: ByteArray,
        val width: Int,
        val height: Int
    )

    private val renderer = ThermalFrameRenderer()
    private val frameStream = MutableSharedFlow<FramePacket>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val cameraCommandStream = MutableSharedFlow<CameraCommand>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val uiState = MutableStateFlow(ThermalCameraUiState())

    private var latestFrame: FramePacket? = null
    private var frameSequence = 0L
    private var fpsWindowStart = SystemClock.elapsedRealtime()
    private var fpsFrames = 0

    val state: StateFlow<ThermalCameraUiState> = uiState.asStateFlow()
    val cameraCommands = cameraCommandStream.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            frameStream.collectLatest { frame ->
                val bitmap = renderer.render(
                    data = frame.data,
                    width = frame.width,
                    height = frame.height,
                    palette = uiState.value.selectedPalette
                )
                val fps = measureFps()
                val preview = ThermalPreviewFrame(
                    bitmap = bitmap,
                    width = frame.width,
                    height = frame.height,
                    sequence = ++frameSequence
                )
                withContext(Dispatchers.Main) {
                    uiState.update {
                        it.copy(
                            statusTitle = "Live Thermal Feed",
                            statusDetail = "Streaming from the attached UVC camera over OTG.",
                            resolutionLabel = "${frame.width} x ${frame.height}",
                            fpsLabel = "$fps FPS",
                            isStreaming = true,
                            previewFrame = preview
                        )
                    }
                }
            }
        }
    }

    fun onPermissionUpdated(granted: Boolean) {
        if (!granted) {
            uiState.update {
                it.copy(
                    statusTitle = "Camera Permission Required",
                    statusDetail = "Grant camera access to initialize the UVC thermal stream.",
                    isCameraEnabled = false,
                    isStreaming = false,
                    fpsLabel = "--"
                )
            }
            return
        }
        if (uiState.value.previewFrame == null) {
            uiState.update {
                it.copy(
                    statusTitle = "Connect A UVC Thermal Camera",
                    statusDetail = "Attach your camera with an OTG adapter. USB permission will be requested automatically.",
                    resolutionLabel = "--",
                    fpsLabel = "--"
                )
            }
        }
    }

    fun onUsbMonitorReady() {
        if (uiState.value.previewFrame != null) {
            return
        }
        uiState.update {
            it.copy(
                statusTitle = "Waiting For Camera",
                statusDetail = "Listening for compatible UVC devices on USB.",
                resolutionLabel = "--",
                fpsLabel = "--",
                isStreaming = false
            )
        }
    }

    fun onCameraOpened() {
        uiState.update {
            it.copy(
                statusTitle = "Thermal Feed Starting",
                statusDetail = "Camera connection established. Rendering the first frames now.",
                isCameraEnabled = true,
                isStreaming = false
            )
        }
    }

    fun onCameraClosed() {
        uiState.update {
            if (it.isCameraEnabled) {
                it.copy(
                    statusTitle = "Camera Disconnected",
                    statusDetail = "Reconnect the UVC thermal camera to resume the feed.",
                    resolutionLabel = "--",
                    fpsLabel = "--",
                    isStreaming = false,
                    previewFrame = null
                )
            } else {
                it.copy(
                    statusTitle = "Camera Off",
                    statusDetail = "Shutter closed. Tap Camera On to resume the live feed.",
                    resolutionLabel = "--",
                    fpsLabel = "--",
                    isStreaming = false,
                    previewFrame = null
                )
            }
        }
    }

    fun onCameraError(message: String?) {
        uiState.update {
            it.copy(
                statusTitle = "Camera Error",
                statusDetail = message ?: "The camera stream could not be opened on this device.",
                resolutionLabel = "--",
                fpsLabel = "--",
                isStreaming = false,
                previewFrame = null
            )
        }
    }

    fun selectPalette(palette: ThermalPalette) {
        if (uiState.value.selectedPalette == palette) {
            return
        }
        uiState.update { it.copy(selectedPalette = palette) }
        latestFrame?.let { cached ->
            viewModelScope.launch {
                frameStream.emit(cached)
            }
        }
    }

    fun cyclePalette() {
        val palettes = ThermalPalette.entries
        val currentIndex = palettes.indexOf(uiState.value.selectedPalette)
        val nextIndex = (currentIndex + 1) % palettes.size
        selectPalette(palettes[nextIndex])
    }

    fun toggleDetailBoost() {
        uiState.update {
            it.copy(isDetailBoostEnabled = !it.isDetailBoostEnabled)
        }
        latestFrame?.let { cached ->
            viewModelScope.launch {
                frameStream.emit(cached)
            }
        }
    }

    fun toggleCameraPower() {
        if (uiState.value.isCameraEnabled) {
            uiState.update {
                it.copy(
                    isCameraEnabled = false,
                    statusTitle = "Camera Off",
                    statusDetail = "Closing the shutter and stopping the live feed.",
                    resolutionLabel = "--",
                    fpsLabel = "--",
                    isStreaming = false,
                    previewFrame = null
                )
            }
            cameraCommandStream.tryEmit(CameraCommand.Disable)
            return
        }

        uiState.update {
            it.copy(
                isCameraEnabled = true,
                statusTitle = "Reopening Camera",
                statusDetail = "Requesting the active UVC stream.",
                isStreaming = false
            )
        }
        cameraCommandStream.tryEmit(CameraCommand.Enable)
    }

    fun submitFrame(data: ByteArray, width: Int, height: Int) {
        val packet = FramePacket(data = data, width = width, height = height)
        latestFrame = packet
        frameStream.tryEmit(packet)
    }

    private fun measureFps(): Int {
        fpsFrames += 1
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - fpsWindowStart
        if (elapsed < 1_000L) {
            return uiState.value.fpsLabel.removeSuffix(" FPS").toIntOrNull() ?: fpsFrames
        }
        val fps = ((fpsFrames * 1_000f) / elapsed).toInt().coerceAtLeast(1)
        fpsFrames = 0
        fpsWindowStart = now
        return fps
    }
}
