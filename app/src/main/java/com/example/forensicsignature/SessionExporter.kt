package com.example.forensicsignature

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class SessionExporter {

    fun exportSession(outputDir: File, session: CaptureSession): ExportResult {
        outputDir.mkdirs()
        val baseName = "sig_${session.metadata.sessionId}_${System.currentTimeMillis()}"
        val jsonFile = File(outputDir, "$baseName.json")
        val csvFile = File(outputDir, "$baseName.csv")

        val jsonPayload = toJson(session).toString(2)
        jsonFile.writeText(jsonPayload)
        csvFile.writeText(toCsv(session.samples))

        return ExportResult(
            jsonFile = jsonFile,
            csvFile = csvFile,
            jsonSha256 = sha256Hex(jsonPayload.toByteArray()),
            csvSha256 = sha256Hex(csvFile.readBytes())
        )
    }

    fun buildSession(
        metadataBase: CaptureMetadata,
        samples: List<SignatureSample>
    ): CaptureSession {
        val endedAtUtc = DateTimeFormatter.ISO_INSTANT
            .format(Instant.now().atOffset(ZoneOffset.UTC))
        val metadata = metadataBase.copy(endedAtUtc = endedAtUtc)
        val samplesJson = JSONArray()
        samples.forEach { samplesJson.put(sampleToJson(it)) }
        val samplesHash = sha256Hex(samplesJson.toString().toByteArray())
        val payload = JSONObject()
            .put("metadata", metadataToJson(metadata))
            .put("samples", samplesJson)
            .put("samplesSha256", samplesHash)
        val payloadHash = sha256Hex(payload.toString().toByteArray())
        return CaptureSession(
            metadata = metadata,
            samples = samples,
            samplesSha256 = samplesHash,
            payloadSha256 = payloadHash
        )
    }

    fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun toJson(session: CaptureSession): JSONObject {
        val samplesJson = JSONArray()
        session.samples.forEach { samplesJson.put(sampleToJson(it)) }

        return JSONObject()
            .put("metadata", metadataToJson(session.metadata))
            .put("samples", samplesJson)
            .put("samplesSha256", session.samplesSha256)
            .put("payloadSha256", session.payloadSha256)
    }

    private fun metadataToJson(metadata: CaptureMetadata): JSONObject {
        return JSONObject()
            .put("sessionId", metadata.sessionId)
            .put("startedAtUtc", metadata.startedAtUtc)
            .put("endedAtUtc", metadata.endedAtUtc)
            .put("logicalWidth", metadata.logicalWidth)
            .put("logicalHeight", metadata.logicalHeight)
            .put("sourceWidthPx", metadata.sourceWidthPx)
            .put("sourceHeightPx", metadata.sourceHeightPx)
            .put("deviceManufacturer", metadata.deviceManufacturer)
            .put("deviceModel", metadata.deviceModel)
            .put("sdkInt", metadata.sdkInt)
            .put("buildFingerprint", metadata.buildFingerprint)
            .put("appVersionName", metadata.appVersionName)
            .put("appVersionCode", metadata.appVersionCode)
            .put("timezoneId", metadata.timezoneId)
    }

    private fun sampleToJson(sample: SignatureSample): JSONObject {
        return JSONObject()
            .put("eventTime", sample.eventTimeMs)
            .put("x", sample.xNorm)
            .put("y", sample.yNorm)
            .put("pressure", sample.pressure)
            .put("eventType", sample.eventType.name)
            .put("toolType", sample.toolType.name)
            .put("tilt", sample.tilt)
            .put("orientation", sample.orientation)
            .put("distance", sample.distance)
            .put("pointerId", sample.pointerId)
            .put("elapsedRealtimeNanos", sample.elapsedRealtimeNanos)
    }

    private fun toCsv(samples: List<SignatureSample>): String {
        val header = "eventTime,x,y,pressure,eventType,toolType,tilt,orientation,distance,pointerId,elapsedRealtimeNanos"
        val body = samples.joinToString("\n") { s ->
            listOf(
                s.eventTimeMs.toString(),
                formatFloat(s.xNorm),
                formatFloat(s.yNorm),
                formatFloat(s.pressure),
                s.eventType.name,
                s.toolType.name,
                formatFloat(s.tilt),
                formatFloat(s.orientation),
                formatFloat(s.distance),
                s.pointerId.toString(),
                s.elapsedRealtimeNanos.toString()
            ).joinToString(",")
        }
        return "$header\n$body\n"
    }

    private fun formatFloat(value: Float): String =
        String.format(Locale.US, "%.6f", value)
}

data class ExportResult(
    val jsonFile: File,
    val csvFile: File,
    val jsonSha256: String,
    val csvSha256: String
)
