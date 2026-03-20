package com.jiangdg.natives

/** pcm to mp3
 *
 * @author Created by jiangdg on 2022/3/2
 */
object LameMp3 {
    fun lameInit(
        inSampleRate: Int,
        outChannel: Int,
        outSampleRate: Int,
        outBitRate: Int,
        quality: Int
    ) = Unit

    fun lameEncode(
        leftBuf: ShortArray?,
        rightBuf: ShortArray?,
        sampleRate: Int,
        mp3Buf: ByteArray?
    ): Int = 0

    fun lameFlush(mp3buf: ByteArray?): Int = 0

    fun lameClose() = Unit
}
