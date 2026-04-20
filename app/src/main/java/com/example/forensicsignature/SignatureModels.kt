package com.example.forensicsignature

import android.os.Build
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class CaptureEventType {
    DOWN,
    MOVE,
    MOVE_HIST,
    UP,
    HOVER_ENTER,
    HOVER_MOVE,
    HOVER_EXIT
}

enum class CaptureToolType {
    STYLUS,
    FINGER,
    ERASER,
    UNKNOWN
}

data class SignatureSample(
    val eventTimeMs: Long,
    val xNorm: Float,
    val yNorm: Float,
    val pressure: Float,
    val eventType: CaptureEventType,
    val toolType: CaptureToolType,
    val tilt: Float,
    val orientation: Float,
    val distance: Float,
    val pointerId: Int,
    val elapsedRealtimeNanos: Long
)

data class CaptureMetadata(
    val sessionId: String = UUID.randomUUID().toString(),
    val startedAtUtc: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC)),
    val endedAtUtc: String,
    val logicalWidth: Int,
    val logicalHeight: Int,
    val sourceWidthPx: Int,
    val sourceHeightPx: Int,
    val deviceManufacturer: String = Build.MANUFACTURER,
    val deviceModel: String = Build.MODEL,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val buildFingerprint: String = Build.FINGERPRINT,
    val appVersionName: String,
    val appVersionCode: Long,
    val timezoneId: String
)

data class CaptureSession(
    val metadata: CaptureMetadata,
    val samples: List<SignatureSample>,
    val samplesSha256: String,
    val payloadSha256: String
)
