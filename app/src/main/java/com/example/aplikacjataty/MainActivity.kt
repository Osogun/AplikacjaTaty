package com.example.aplikacjataty

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var signatureCaptureView: SignatureCaptureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signatureCaptureView = findViewById(R.id.signatureCaptureView)

        val clearButton: Button = findViewById(R.id.btnClear)
        val saveButton: Button = findViewById(R.id.btnSaveCsv)

        clearButton.setOnClickListener {
            signatureCaptureView.clearSignature()
            Toast.makeText(this, "Wyczyszczono podpis", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            saveCsv()
        }
    }

    private fun saveCsv() {
        val points = signatureCaptureView.getCapturedPoints()
        if (points.isEmpty()) {
            Toast.makeText(this, "Brak danych do zapisu", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "signature_${timestamp}.csv"
        val documentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        if (documentsDir == null) {
            Toast.makeText(this, "Nie można uzyskać katalogu Documents", Toast.LENGTH_LONG).show()
            return
        }

        val outFile = File(documentsDir, fileName)

        try {
            FileWriter(outFile).use { writer ->
                writer.appendLine("time,x,y,pressure,eventType,toolType,tilt,orientation,distance")
                points.forEach { p ->
                    writer.append(p.time.toString()).append(',')
                        .append(p.x.toString()).append(',')
                        .append(p.y.toString()).append(',')
                        .append(p.pressure.toString()).append(',')
                        .append(p.eventType).append(',')
                        .append(p.toolType).append(',')
                        .append(p.tilt.toString()).append(',')
                        .append(p.orientation.toString()).append(',')
                        .append(p.distance.toString())
                        .appendLine()
                }
            }

            Toast.makeText(this, "Zapisano: ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd zapisu CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
