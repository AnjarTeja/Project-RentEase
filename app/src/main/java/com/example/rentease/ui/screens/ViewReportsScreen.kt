package com.example.rentease.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.ReportItem
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.StatCard
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.PurpleAccent
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReportsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val pdfFileNameFormat = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()) }
    var reports by remember { mutableStateOf(listOf<ReportItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var statVal1 by remember { mutableStateOf("-") }
    var statLabel1 by remember { mutableStateOf("-") }
    var statVal2 by remember { mutableStateOf("-") }
    var statLabel2 by remember { mutableStateOf("-") }
    var showExportError by remember { mutableStateOf(false) }
    var exportErrorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val reportTypes = listOf("Penyewaan (Rentals)", "Barang (Items)", "Pengguna (Users)")

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            generateAndSavePdf(uri, reports, reportTypes[selectedType], statLabel1, statVal1, statLabel2, statVal2, dateFormat, context.contentResolver, { showExportError = true; exportErrorMessage = it }, { showExportError = false })
        }
    }

    fun loadRentalReports() {
        isLoading = true
        statLabel1 = "Total Sewa"
        statLabel2 = "Disetujui"
        db.collection("rentals").get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<ReportItem>()
                var approvedCount = 0
                for (doc in documents) {
                    val status = doc.getString("status") ?: "pending"
                    if (status == "approved") approvedCount++
                    list.add(
                        ReportItem(
                            title = "Sewa: ${doc.getString("itemName") ?: "Barang"}",
                            subtitle = "Penyewa: ${doc.getString("renterName") ?: "Penyewa"} - Status: ${status.uppercase()}",
                            dateStr = dateFormat.format(Date(doc.getLong("createdAt") ?: 0L)),
                            timestamp = doc.getLong("createdAt") ?: 0L
                        )
                    )
                }
                reports = list.sortedByDescending { it.timestamp }
                statVal1 = documents.size().toString()
                statVal2 = approvedCount.toString()
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    fun loadItemReports() {
        isLoading = true
        statLabel1 = "Total Barang"
        statLabel2 = "Tersedia"
        db.collection("items").get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<ReportItem>()
                var availableCount = 0
                for (doc in documents) {
                    val status = doc.getString("status") ?: "available"
                    if (status == "available") availableCount++
                    list.add(
                        ReportItem(
                            title = "Barang: ${doc.getString("name") ?: "Barang"}",
                            subtitle = "Harga: Rp${(doc.getDouble("price") ?: 0.0).toInt()} - Status: ${status.uppercase()}",
                            dateStr = dateFormat.format(Date(doc.getLong("createdAt") ?: 0L)),
                            timestamp = doc.getLong("createdAt") ?: 0L
                        )
                    )
                }
                reports = list.sortedByDescending { it.timestamp }
                statVal1 = documents.size().toString()
                statVal2 = availableCount.toString()
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    fun loadUserReports() {
        isLoading = true
        statLabel1 = "Total User"
        statLabel2 = "Total Staff"
        db.collection("users").get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<ReportItem>()
                var regularUserCount = 0
                var staffCount = 0
                for (doc in documents) {
                    val role = doc.getString("role") ?: "user"
                    when (role) {
                        "user" -> regularUserCount++
                        "petugas", "admin" -> staffCount++
                    }
                    list.add(
                        ReportItem(
                            title = "User: ${doc.getString("name") ?: "Tanpa Nama"}",
                            subtitle = "Email: ${doc.getString("email") ?: "-"} - Role: ${role.uppercase()}",
                            dateStr = dateFormat.format(Date(doc.getLong("createdAt") ?: System.currentTimeMillis())),
                            timestamp = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    )
                }
                reports = list.sortedByDescending { it.timestamp }
                statVal1 = regularUserCount.toString()
                statVal2 = staffCount.toString()
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    fun loadReports(type: Int) {
        when (type) {
            0 -> loadRentalReports()
            1 -> loadItemReports()
            2 -> loadUserReports()
        }
    }

    LaunchedEffect(selectedType) {
        loadReports(selectedType)
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(
                title = "Laporan",
                onBackClick = onBack,
                trailingIcon = {
                    IconButton(onClick = {
                        if (reports.isEmpty()) {
                            showExportError = true
                            exportErrorMessage = "Tidak ada data untuk diekspor"
                        } else {
                            val fileName = "RentEase_Laporan_${reportTypes[selectedType].substringBefore(" ").lowercase()}_${pdfFileNameFormat.format(Date())}.pdf"
                            createDocumentLauncher.launch(fileName)
                        }
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export PDF", tint = Primary)
                    }
                }
            )

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = reportTypes[selectedType],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        reportTypes.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label, color = TextDark) },
                                onClick = {
                                    selectedType = index
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = when (selectedType) { 0 -> Icons.Default.ShoppingCart; 1 -> Icons.Default.Inventory; else -> Icons.Default.Person },
                        value = statVal1,
                        label = statLabel1,
                        modifier = Modifier.weight(1f),
                        iconTint = Primary,
                        valueColor = Primary
                    )
                    StatCard(
                        icon = Icons.Default.Description,
                        value = statVal2,
                        label = statLabel2,
                        modifier = Modifier.weight(1f),
                        iconTint = SuccessColor,
                        valueColor = SuccessColor
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                    }
                } else if (reports.isEmpty()) {
                    Text(
                        text = "Tidak ada data laporan",
                        color = TextHint,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    reports.forEach { report ->
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Text(text = report.title, style = MaterialTheme.typography.titleSmall, color = TextDark)
                                Text(text = report.subtitle, style = MaterialTheme.typography.bodySmall, color = TextLight)
                                Text(text = report.dateStr, style = MaterialTheme.typography.labelSmall, color = TextHint)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showExportError) {
        AlertDialog(
            onDismissRequest = { showExportError = false },
            title = { Text("Ekspor PDF", color = TextDark) },
            text = { Text(exportErrorMessage, color = TextLight) },
            confirmButton = {
                TextButton(onClick = { showExportError = false }) {
                    Text("OK", color = Primary)
                }
            },
            containerColor = TechCardBg
        )
    }
}

private fun generateAndSavePdf(
    uri: Uri,
    reports: List<ReportItem>,
    category: String,
    statLabel1: String,
    statVal1: String,
    statLabel2: String,
    statVal2: String,
    dateFormat: SimpleDateFormat,
    contentResolver: android.content.ContentResolver,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    var currentPage = pdfDocument.startPage(pageInfo)
    var canvas = currentPage.canvas

    val titlePaint = android.graphics.Paint().apply {
        textSize = 24f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        color = android.graphics.Color.rgb(0, 100, 200)
    }
    val headerPaint = android.graphics.Paint().apply {
        textSize = 14f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        color = android.graphics.Color.BLACK
    }
    val textPaint = android.graphics.Paint().apply {
        textSize = 12f
        color = android.graphics.Color.BLACK
    }
    val lightTextPaint = android.graphics.Paint().apply {
        textSize = 10f
        color = android.graphics.Color.DKGRAY
    }
    val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 1f
    }

    val margin = 50f
    var yPos = 60f

    canvas.drawText("Laporan Sistem RentEase", margin, yPos, titlePaint)
    yPos += 30f
    canvas.drawText("Kategori Laporan: $category", margin, yPos, textPaint)
    yPos += 20f
    canvas.drawText("Dicetak pada: ${dateFormat.format(Date())}", margin, yPos, textPaint)
    yPos += 20f

    val stat1 = "$statLabel1: $statVal1"
    val stat2 = "$statLabel2: $statVal2"
    canvas.drawText("Ringkasan -> $stat1 | $stat2", margin, yPos, headerPaint)
    yPos += 20f
    canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, linePaint)
    yPos += 30f

    canvas.drawText("No.", margin, yPos, headerPaint)
    canvas.drawText("Informasi Utama", margin + 40f, yPos, headerPaint)
    canvas.drawText("Tanggal", pageInfo.pageWidth - margin - 100f, yPos, headerPaint)
    yPos += 10f
    canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, linePaint)
    yPos += 20f

    for ((index, item) in reports.withIndex()) {
        if (yPos > pageInfo.pageHeight - 60f) {
            pdfDocument.finishPage(currentPage)
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            yPos = 60f
            canvas.drawText("No.", margin, yPos, headerPaint)
            canvas.drawText("Informasi Utama", margin + 40f, yPos, headerPaint)
            canvas.drawText("Tanggal", pageInfo.pageWidth - margin - 100f, yPos, headerPaint)
            yPos += 10f
            canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, linePaint)
            yPos += 20f
        }
        canvas.drawText("${index + 1}.", margin, yPos, textPaint)
        canvas.drawText(item.title, margin + 40f, yPos, textPaint)
        canvas.drawText(item.dateStr, pageInfo.pageWidth - margin - 100f, yPos, textPaint)
        yPos += 15f
        canvas.drawText(item.subtitle, margin + 40f, yPos, lightTextPaint)
        yPos += 20f
        canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, linePaint)
        yPos += 15f
    }

    pdfDocument.finishPage(currentPage)

    try {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        onSuccess()
    } catch (e: Exception) {
        onError("Gagal menyimpan laporan: ${e.localizedMessage}")
    } finally {
        pdfDocument.close()
    }
}
