package com.example.uvcthermal.camera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.IAspectRatio
import kotlinx.coroutines.launch

class ThermalCameraFragment : CameraFragment(), IPreviewDataCallBack {
    private val viewModel: ThermalCameraViewModel by activityViewModels()
    private var isPreviewCallbackAttached = false

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        return FrameLayout(inflater.context).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
        }
    }

    override fun initData() {
        super.initData()
        viewModel.onUsbMonitorReady()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cameraCommands.collect { command ->
                    when (command) {
                        ThermalCameraViewModel.CameraCommand.Disable -> disableCameraStream()
                        ThermalCameraViewModel.CameraCommand.Enable -> enableCameraStream()
                    }
                }
            }
        }
    }

    override fun clear() {
        if (isPreviewCallbackAttached) {
            removePreviewDataCallBack(this)
            isPreviewCallbackAttached = false
        }
        super.clear()
    }

    override fun getCameraView(): IAspectRatio? = null

    override fun getCameraViewContainer(): ViewGroup? = null

    override fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(640)
            .setPreviewHeight(480)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
            .setRawPreviewData(true)
            .setAspectRatioShow(false)
            .create()
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                if (!isPreviewCallbackAttached) {
                    addPreviewDataCallBack(this)
                    isPreviewCallbackAttached = true
                }
                viewModel.onCameraOpened()
            }

            ICameraStateCallBack.State.CLOSED -> {
                if (isPreviewCallbackAttached) {
                    removePreviewDataCallBack(this)
                    isPreviewCallbackAttached = false
                }
                viewModel.onCameraClosed()
            }

            ICameraStateCallBack.State.ERROR -> {
                if (isPreviewCallbackAttached) {
                    removePreviewDataCallBack(this)
                    isPreviewCallbackAttached = false
                }
                viewModel.onCameraError(msg)
            }
        }
    }

    override fun onPreviewData(
        data: ByteArray?,
        width: Int,
        height: Int,
        format: IPreviewDataCallBack.DataFormat
    ) {
        if (format != IPreviewDataCallBack.DataFormat.NV21 || data == null) {
            return
        }
        viewModel.submitFrame(data, width, height)
    }

    private fun disableCameraStream() {
        if (isCameraOpened()) {
            closeCamera()
        }
    }

    private fun enableCameraStream() {
        if (isCameraOpened()) {
            return
        }
        val device = getDeviceList()?.firstOrNull() ?: return
        requestPermission(device)
    }

    companion object {
        const val TAG = "ThermalCameraFragment"
    }
}
