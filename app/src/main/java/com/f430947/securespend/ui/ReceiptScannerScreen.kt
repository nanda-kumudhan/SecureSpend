package com.f430947.securespend.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Screen that lets the user photograph a receipt and uses ML Kit Text Recognition
 * to automatically extract the total amount.
 *
 * The extracted amount is placed in the previous back-stack entry's SavedStateHandle
 * (key: "scanned_amount") so that AddExpenseScreen can observe it and pre-fill the
 * Amount field without any extra ViewModel coupling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(navController: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var scannedAmount by remember { mutableStateOf<Double?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    // Launcher: take a low-resolution preview photo (no FileProvider needed)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            recognizedText = ""
            scannedAmount = null
            statusMessage = ""
            isProcessing = true

            scope.launch {
                try {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val result = recognizer.process(image).await()
                    recognizedText = result.text
                    scannedAmount = extractTotalAmount(result.text)
                    statusMessage = if (scannedAmount != null) {
                        "Amount detected: £%.2f".format(scannedAmount)
                    } else {
                        "Could not detect a total amount. Please enter it manually."
                    }
                } catch (e: Exception) {
                    statusMessage = "Text recognition failed: ${e.localizedMessage}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Launcher: request CAMERA permission before opening camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePictureLauncher.launch(null)
        else statusMessage = "Camera permission is required to scan receipts."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Take a photo of your receipt to automatically detect the total amount.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Camera capture button
            OutlinedButton(
                onClick = {
                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasCameraPermission) {
                        takePictureLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Take Photo of Receipt")
            }

            // Show captured image thumbnail
            capturedBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Captured receipt",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Processing indicator
            if (isProcessing) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                Text("Scanning receipt…", style = MaterialTheme.typography.bodySmall)
            }

            // Status / result message
            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (scannedAmount != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            // Use amount button — only shown when an amount was successfully detected
            if (scannedAmount != null) {
                Button(
                    onClick = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_amount", scannedAmount)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use £%.2f".format(scannedAmount))
                }
            }
        }
    }
}

/**
 * Parses the raw OCR text from a receipt and returns the most likely "total" amount.
 *
 * Strategy (in priority order):
 * 1. Look for a line containing "total" (case-insensitive) followed by a currency amount.
 * 2. Fall back to the largest currency amount found anywhere in the text (heuristic: totals
 *    are usually the biggest single number on a receipt).
 */
internal fun extractTotalAmount(text: String): Double? {
    // Pattern: optional currency symbol, digits, decimal separator, two digits
    val amountPattern = Regex("""[£$€]?\s*(\d{1,6}[.,]\d{2})\b""")

    // Priority 1: find an amount on a line that contains "total"
    text.lines().forEach { line ->
        if (line.contains("total", ignoreCase = true)) {
            amountPattern.find(line)?.let { match ->
                normalise(match.groupValues[1])?.let { return it }
            }
        }
    }

    // Priority 2: return the largest amount in the document (likely the grand total)
    return amountPattern.findAll(text)
        .mapNotNull { normalise(it.groupValues[1]) }
        .maxOrNull()
}

/** Converts a raw decimal string (which may use comma as separator) to a Double. */
private fun normalise(raw: String): Double? = raw.replace(',', '.').toDoubleOrNull()
