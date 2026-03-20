package com.example.uvcthermal.camera

import android.graphics.Bitmap

class ThermalFrameRenderer {
    private var argbBuffer = IntArray(0)
    private var bitmapBuffers: Array<Bitmap?> = arrayOfNulls(2)
    private var activeBufferIndex = 0

    fun render(
        data: ByteArray,
        width: Int,
        height: Int,
        palette: ThermalPalette
    ): Bitmap {
        val pixelCount = width * height
        if (argbBuffer.size != pixelCount) {
            argbBuffer = IntArray(pixelCount)
        }

        val buffer0 = bitmapBuffers[0]
        if (buffer0 == null || buffer0.width != width || buffer0.height != height) {
            bitmapBuffers = arrayOf(
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888),
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            )
            activeBufferIndex = 0
        }

        val lookup = palette.colorTable()
        for (index in 0 until pixelCount) {
            argbBuffer[index] = lookup[data[index].toInt() and 0xFF]
        }

        val nextBufferIndex = (activeBufferIndex + 1) % bitmapBuffers.size
        val targetBitmap = bitmapBuffers[nextBufferIndex]!!
        targetBitmap.setPixels(argbBuffer, 0, width, 0, 0, width, height)
        activeBufferIndex = nextBufferIndex
        return targetBitmap
    }
}
