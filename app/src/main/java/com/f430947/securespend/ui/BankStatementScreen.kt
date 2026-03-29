package com.f430947.securespend.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f430947.securespend.data.Expense
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Screen for uploading a bank-statement image and automatically extracting transactions.
 *
 * Workflow:
 * 1. User picks an image from the gallery via [ActivityResultContracts.GetContent].
 * 2. ML Kit Text Recognition reads all text from the image.
 * 3. [parseTransactions] attempts to find rows with a date, description, and debit amount.
 * 4. The user can deselect unwanted rows and then bulk-import the rest as Expenses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankStatementScreen(viewModel: ExpenseViewModel) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    // Detected transactions (mutable list so toggling selection triggers recomposition)
    val detectedTransactions = remember { mutableStateListOf<ParsedTransaction>() }
    // Which rows are selected for import (true = will be imported)
    val selected = remember { mutableStateListOf<Boolean>() }

    // Image picker – no storage permission required; the system grants a temporary URI
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        detectedTransactions.clear()
        selected.clear()
        isProcessing = true
        statusMessage = ""

        scope.launch {
            try {
                val bitmap: Bitmap? = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                if (bitmap == null) {
                    statusMessage = "Could not decode image."
                    isProcessing = false
                    return@launch
                }
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val result = recognizer.process(image).await()
                val transactions = parseTransactions(result.text)
                detectedTransactions.addAll(transactions)
                // Start with all rows selected
                repeat(transactions.size) { selected.add(true) }
                statusMessage = if (transactions.isEmpty())
                    "No transactions found. Try a clearer image."
                else
                    "${transactions.size} transaction(s) detected."
            } catch (e: Exception) {
                statusMessage = "Could not process image: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bank Statement") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Upload a photo of your bank statement to automatically import transactions.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                onClick = { pickImageLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Text("  Choose Statement Image")
            }

            if (isProcessing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                Text(
                    "Analysing statement…",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (detectedTransactions.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            if (detectedTransactions.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Detected Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap the circle to deselect any rows you don't want to import.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detectedTransactions.indices.toList()) { index ->
                        val tx = detectedTransactions[index]
                        TransactionRow(
                            transaction = tx,
                            isSelected = selected.getOrElse(index) { true },
                            onToggle = { if (index < selected.size) selected[index] = !selected[index] }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val selectedCount = selected.count { it }
                Button(
                    onClick = {
                        var importCount = 0
                        detectedTransactions.forEachIndexed { index, tx ->
                            if (selected.getOrElse(index) { false }) {
                                viewModel.insertExpense(
                                    Expense(
                                        title = tx.description,
                                        amount = tx.amount,
                                        date = tx.date,
                                        category = "Other"
                                    )
                                )
                                importCount++
                            }
                        }
                        detectedTransactions.clear()
                        selected.clear()
                        statusMessage = ""
                        scope.launch {
                            snackbarHostState.showSnackbar("Imported $importCount expense(s).")
                        }
                    },
                    enabled = selectedCount > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import $selectedCount selected expense(s)")
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: ParsedTransaction,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(transaction.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "£%.2f".format(transaction.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Deselect" else "Select",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** A single transaction extracted from OCR text. */
data class ParsedTransaction(val date: String, val description: String, val amount: Double)

/** Maximum plausible single transaction amount; anything above this is ignored as OCR noise. */
private const val MAX_TRANSACTION_AMOUNT = 100_000.0

/** Regex that identifies header or summary rows which should be skipped during parsing. */
private val HEADER_LINE_PATTERN = Regex(
    "balance|opening|closing|statement|account|sort code|date\\s+desc",
    RegexOption.IGNORE_CASE
)

/**
 * Parses OCR text from a bank statement image and returns a list of detected transactions.
 *
 * Matches lines that contain:
 * - A date component (DD/MM/YY or DD/MM/YYYY or DD Jan YYYY etc.)
 * - A debit/amount component (e.g. 12.99 or £12.99)
 *
 * The function is intentionally lenient – it errs on the side of returning candidates
 * that the user can deselect, rather than missing real transactions.
 */
internal fun parseTransactions(text: String): List<ParsedTransaction> {
    val results = mutableListOf<ParsedTransaction>()

    // Pattern: optional £/$/€ + digits + decimal separator + 2 digits
    val amountPattern = Regex("""[£$€]?\s*(\d{1,6}[.,]\d{2})\b""")

    // Date patterns we recognise (UK-first ordering):
    // DD/MM/YYYY, DD/MM/YY, DD-MM-YYYY, YYYY-MM-DD, DD Mon YYYY, DD Mon YY
    val datePatterns = listOf(
        Regex("""(\d{2})[/-](\d{2})[/-](\d{4})"""),  // DD/MM/YYYY or DD-MM-YYYY
        Regex("""(\d{2})[/-](\d{2})[/-](\d{2})\b"""), // DD/MM/YY
        Regex("""(\d{4})-(\d{2})-(\d{2})"""),          // YYYY-MM-DD (ISO)
        Regex("""(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{2})\b""", RegexOption.IGNORE_CASE)
    )

    val monthMap = mapOf(
        "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
        "may" to "05", "jun" to "06", "jul" to "07", "aug" to "08",
        "sep" to "09", "oct" to "10", "nov" to "11", "dec" to "12"
    )

    for (line in text.lines()) {
        val trimmed = line.trim()
        if (trimmed.length < 5) continue
        // Skip header/summary lines
        if (trimmed.contains(HEADER_LINE_PATTERN)) continue

        // Try to find a date in this line
        var parsedDate: String? = null
        for (pattern in datePatterns) {
            val match = pattern.find(trimmed) ?: continue
            parsedDate = when (datePatterns.indexOf(pattern)) {
                0 -> { // DD/MM/YYYY
                    val (d, m, y) = match.destructured
                    "$y-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
                }
                1 -> { // DD/MM/YY
                    val (d, m, y) = match.destructured
                    val fullYear = "20$y"
                    "$fullYear-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
                }
                2 -> { // YYYY-MM-DD
                    val (y, m, d) = match.destructured
                    "$y-$m-$d"
                }
                3, 4 -> { // DD Mon YYYY or DD Mon YY
                    val groups = match.groupValues
                    val day = groups[1].padStart(2, '0')
                    val month = monthMap[groups[2].lowercase().take(3)] ?: continue
                    val rawYear = groups[3]
                    val year = if (rawYear.length == 2) "20$rawYear" else rawYear
                    "$year-$month-$day"
                }
                else -> null
            }
            break
        }

        if (parsedDate == null) continue

        // Find all amounts on this line; take the last one (typically the debit amount)
        val amounts = amountPattern.findAll(trimmed).mapNotNull { m ->
            m.groupValues[1].replace(',', '.').toDoubleOrNull()
        }.toList()
        val amount = amounts.lastOrNull() ?: continue
        if (amount <= 0.0 || amount > MAX_TRANSACTION_AMOUNT) continue

        // Build a description from the text between the date match and the amount match
        // Strip the raw date token and amount token; what remains is the description
        var desc = trimmed
        datePatterns.forEach { p -> desc = p.replace(desc, "") }
        desc = amountPattern.replace(desc, "").trim()
            .replace(Regex("""[£$€\|]"""), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
        if (desc.isBlank()) desc = "Transaction"

        results += ParsedTransaction(date = parsedDate, description = desc, amount = amount)
    }

    return results
}
