package com.example.forensicsignature

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var signatureView: SignatureCaptureView
    private lateinit var tvStatus: TextView
    private val exporter = SessionExporter()
    private var sessionStartUtc: String = ""
    private var sessionId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signatureView = findViewById(R.id.signatureView)
        tvStatus = findViewById(R.id.tvStatus)
        val btnNew: Button = findViewById(R.id.btnNewSession)
        val btnClear: Button = findViewById(R.id.btnClear)
        val btnSave: Button = findViewById(R.id.btnSave)

        startNewSession(clearCanvas = false)

        signatureView.onSampleCountChanged = { count ->
            tvStatus.text = "Session: $sessionId\nSamples: $count"
        }

        btnNew.setOnClickListener {
            startNewSession(clearCanvas = true)
            Toast.makeText(this, "New session started", Toast.LENGTH_SHORT).show()
        }

        btnClear.setOnClickListener {
            signatureView.clearAll()
            Toast.makeText(this, "Canvas and captured samples cleared", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            saveCurrentSession()
        }
    }

    private fun startNewSession(clearCanvas: Boolean) {
        if (clearCanvas) {
            signatureView.clearAll()
        }
        val metaTemplate = CaptureMetadata(
            endedAtUtc = "",
            logicalWidth = SignatureCaptureView.LOGICAL_WIDTH,
            logicalHeight = SignatureCaptureView.LOGICAL_HEIGHT,
            sourceWidthPx = signatureView.width,
            sourceHeightPx = signatureView.height,
            appVersionName = appVersionName(),
            appVersionCode = appVersionCode(),
            timezoneId = TimeZone.getDefault().id
        )
        sessionId = metaTemplate.sessionId
        sessionStartUtc = metaTemplate.startedAtUtc
        tvStatus.text = "Session: $sessionId\nSamples: 0"
    }

    private fun saveCurrentSession() {
        val samples = signatureView.getSamplesSnapshot()
        if (samples.isEmpty()) {
            Toast.makeText(this, "No samples recorded", Toast.LENGTH_SHORT).show()
            return
        }

        val metadata = CaptureMetadata(
            sessionId = sessionId,
            startedAtUtc = sessionStartUtc,
            endedAtUtc = "",
            logicalWidth = SignatureCaptureView.LOGICAL_WIDTH,
            logicalHeight = SignatureCaptureView.LOGICAL_HEIGHT,
            sourceWidthPx = signatureView.width,
            sourceHeightPx = signatureView.height,
            appVersionName = appVersionName(),
            appVersionCode = appVersionCode(),
            timezoneId = TimeZone.getDefault().id
        )

        val captureSession = exporter.buildSession(metadata, samples)
        val outputDir = File(getExternalFilesDir(null), "forensic_sessions")
        val result = exporter.exportSession(outputDir, captureSession)

        val message = "Saved JSON+CSV\nJSON SHA-256: ${result.jsonSha256.take(16)}...\nCSV SHA-256: ${result.csvSha256.take(16)}..."
        tvStatus.text = "Session: $sessionId\nSamples: ${samples.size}\n${result.jsonFile.name}"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun appVersionName(): String = try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: "unknown"
    } catch (_: PackageManager.NameNotFoundException) {
        "unknown"
    }

    @Suppress("DEPRECATION")
    private fun appVersionCode(): Long = try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        -1L
    }
}
