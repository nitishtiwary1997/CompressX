package com.hire.smartcompress.utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.VideoCompressionConfig
import com.hire.smartcompress.domain.model.VideoQuality
import com.hire.smartcompress.domain.model.VideoResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCompressor @Inject constructor(
    private val fileUtils: FileUtils
) {
    companion object {
        private const val TIMEOUT_US = 10_000L
        private const val VIDEO_MIME = "video/avc"
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1
    }

    @SuppressLint("WrongConstant")
    fun compress(
        context: Context,
        fileItem: FileItem,
        config: VideoCompressionConfig
    ): Flow<CompressionResult> = flow {
        val startTime = System.currentTimeMillis()
        val originalSize = fileUtils.getFileSizeFromUri(context, fileItem.uri)
        emit(CompressionResult.Progress(fileItem.uri, 5))

        val tempOutput = File.createTempFile("sc_vid_", ".mp4", context.cacheDir)

        try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(fileItem.uri, "r")
                ?: throw IllegalStateException("Cannot open video file")

            val extractor = MediaExtractor()
            extractor.setDataSource(fileDescriptor.fileDescriptor)
            fileDescriptor.close()

            val videoTrackIndex = findTrackIndex(extractor, "video/")
            val audioTrackIndex = findTrackIndex(extractor, "audio/")

            if (videoTrackIndex < 0) throw IllegalStateException("No video track found")

            val inputVideoFormat = extractor.getTrackFormat(videoTrackIndex)
            val (targetW, targetH) = calculateTargetDimensions(inputVideoFormat, config.resolution)
            val videoBitrate = calculateBitrate(targetW, targetH, config)
            val outputVideoFormat = createVideoFormat(targetW, targetH, videoBitrate)

            // Pre-fetch audio format before any encoding so we can add it to the muxer upfront.
            // All tracks must be added to MediaMuxer BEFORE calling muxer.start().
            val audioFormat = if (audioTrackIndex >= 0) extractor.getTrackFormat(audioTrackIndex) else null

            emit(CompressionResult.Progress(fileItem.uri, 10))

            val videoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME)
            videoEncoder.configure(outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val encoderInputSurface = videoEncoder.createInputSurface()
            videoEncoder.start()

            val inputMime = inputVideoFormat.getString(MediaFormat.KEY_MIME) ?: VIDEO_MIME
            val videoDecoder = MediaCodec.createDecoderByType(inputMime)
            // Decoder outputs frames directly to the encoder's input surface (surface pipeline)
            videoDecoder.configure(inputVideoFormat, encoderInputSurface, null, 0)
            videoDecoder.start()

            extractor.selectTrack(videoTrackIndex)

            val muxer = MediaMuxer(tempOutput.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var muxerStarted = false
            var encoderDone = false
            var decoderInputDone = false

            val decoderInfo = MediaCodec.BufferInfo()
            val encoderInfo = MediaCodec.BufferInfo()
            val totalDuration = runCatching { inputVideoFormat.getLong(MediaFormat.KEY_DURATION) }.getOrDefault(0L)

            while (!encoderDone) {
                // 1. Feed compressed frames from extractor → decoder input
                if (!decoderInputDone) {
                    val inIdx = videoDecoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val buf = videoDecoder.getInputBuffer(inIdx)
                            ?: throw IllegalStateException("Decoder input buffer null at index $inIdx")
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            // No more data: signal EOS to decoder
                            videoDecoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            decoderInputDone = true
                        } else {
                            videoDecoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // 2. Drain decoder output → renders frames to the encoder's input surface
                val decOutIdx = videoDecoder.dequeueOutputBuffer(decoderInfo, TIMEOUT_US)
                if (decOutIdx >= 0) {
                    // For surface-pipeline decoding: size is always 0 because data goes to surface.
                    // We must pass render=true to forward the frame; render=false discards it.
                    val isEos = (decoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    videoDecoder.releaseOutputBuffer(decOutIdx, !isEos)
                    if (isEos) {
                        // Decoder finished — tell encoder no more frames are coming
                        videoEncoder.signalEndOfInputStream()
                    }
                }

                // 3. Drain encoder output → write to muxer
                val encOutIdx = videoEncoder.dequeueOutputBuffer(encoderInfo, TIMEOUT_US)
                when {
                    encOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Encoder output format is now known — add tracks and start muxer
                        muxerVideoTrack = muxer.addTrack(videoEncoder.outputFormat)
                        if (audioFormat != null) {
                            muxerAudioTrack = muxer.addTrack(audioFormat)
                        }
                        muxer.start()
                        muxerStarted = true
                    }
                    encOutIdx >= 0 -> {
                        val buf = videoEncoder.getOutputBuffer(encOutIdx)
                            ?: throw IllegalStateException("Encoder output buffer null at index $encOutIdx")
                        val isConfig = (encoderInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (!isConfig && muxerStarted && encoderInfo.size > 0) {
                            muxer.writeSampleData(muxerVideoTrack, buf, encoderInfo)
                            if (totalDuration > 0) {
                                val pct = (encoderInfo.presentationTimeUs.toFloat() / totalDuration * 80f)
                                    .toInt().coerceIn(0, 80)
                                emit(CompressionResult.Progress(fileItem.uri, 10 + pct))
                            }
                        }
                        videoEncoder.releaseOutputBuffer(encOutIdx, false)
                        if ((encoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoderDone = true
                        }
                    }
                }
            }

            // 4. Release codec resources before writing audio
            videoEncoder.stop()
            videoEncoder.release()
            videoDecoder.stop()
            videoDecoder.release()
            encoderInputSurface.release()

            // 5. Passthrough-copy audio track (no re-encode needed — just mux raw audio)
            if (audioTrackIndex >= 0 && muxerStarted) {
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                val audioBuf = ByteBuffer.allocate(256 * 1024)
                val audioSampleInfo = MediaCodec.BufferInfo()
                while (true) {
                    val sampleSize = extractor.readSampleData(audioBuf, 0)
                    if (sampleSize < 0) break
                    audioSampleInfo.set(0, sampleSize, extractor.sampleTime, extractor.sampleFlags)
                    muxer.writeSampleData(muxerAudioTrack, audioBuf, audioSampleInfo)
                    extractor.advance()
                }
            }

            extractor.release()
            muxer.stop()
            muxer.release()

            emit(CompressionResult.Progress(fileItem.uri, 95))

            val baseName = (fileUtils.getFileNameFromUri(context, fileItem.uri) ?: "video")
                .substringBeforeLast('.')
            val savedUri = fileUtils.saveToMediaStore(context, tempOutput, "video/mp4", "${baseName}_compressed.mp4")
                ?: throw IllegalStateException("Failed to save compressed video")

            tempOutput.delete()

            val compressedSize = fileUtils.getFileSizeFromUri(context, savedUri)
            val elapsed = System.currentTimeMillis() - startTime

            emit(CompressionResult.Progress(fileItem.uri, 100))
            emit(
                CompressionResult.Success(
                    originalUri = fileItem.uri,
                    compressedUri = savedUri,
                    originalSize = originalSize,
                    compressedSize = compressedSize,
                    processingTimeMs = elapsed,
                    fileType = FileType.VIDEO
                )
            )
        } catch (e: Exception) {
            tempOutput.delete()
            emit(CompressionResult.Error(fileItem.uri, e.message ?: "Video compression failed", e))
        }
    }.flowOn(Dispatchers.IO)

    private fun findTrackIndex(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(mimePrefix)) return i
        }
        return -1
    }

    private fun calculateTargetDimensions(
        inputFormat: MediaFormat,
        resolution: VideoResolution
    ): Pair<Int, Int> {
        val origW = inputFormat.getInteger(MediaFormat.KEY_WIDTH)
        val origH = inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        if (origW <= resolution.width && origH <= resolution.height) return origW to origH
        val ratio = origW.toFloat() / origH.toFloat()
        return if (ratio >= 1f) {
            val w = resolution.width.coerceAtMost(origW)
            val h = (w / ratio).toInt().let { if (it % 2 != 0) it + 1 else it }
            w to h
        } else {
            val h = resolution.height.coerceAtMost(origH)
            val w = (h * ratio).toInt().let { if (it % 2 != 0) it + 1 else it }
            w to h
        }
    }

    private fun calculateBitrate(width: Int, height: Int, config: VideoCompressionConfig): Int {
        val pixels = width * height
        val base = when {
            pixels >= 1920 * 1080 -> 4_000_000
            pixels >= 1280 * 720  -> 2_000_000
            pixels >= 854  * 480  -> 1_000_000
            pixels >= 640  * 360  ->   600_000
            else                  ->   300_000
        }
        return when (config.quality) {
            VideoQuality.LOW   -> (base * 0.5).toInt()
            VideoQuality.MEDIUM -> base
            VideoQuality.HIGH  -> (base * 1.5).toInt()
            VideoQuality.ULTRA -> (base * 2.0).toInt()
        }
    }

    private fun createVideoFormat(width: Int, height: Int, bitrate: Int): MediaFormat =
        MediaFormat.createVideoFormat(VIDEO_MIME, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }
}
