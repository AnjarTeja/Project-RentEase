package com.example.rentease.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ReportItem
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.StatCard
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
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
    var selectedDateFilter by remember { mutableStateOf(0) }
    var expandedType by remember { mutableStateOf(false) }
    var expandedDate by remember { mutableStateOf(false) }
    var statVal1 by remember { mutableStateOf("-") }
    var statLabel1 by remember { mutableStateOf("-") }
    var statVal2 by remember { mutableStateOf("-") }
    var statLabel2 by remember { mutableStateOf("-") }
    var showExportError by remember { mutableStateOf(false) }
    var exportErrorMessage by remember { mutableStateOf("") }
    var accessError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val authManager = FirebaseAuthManager()
        authManager.getUserRole(
            onSuccess = { role ->
                if (role != "admin" && role != "petugas") {
                    accessError = "Anda tidak memiliki akses ke halaman ini"
                }
            },
            onFailure = { accessError = "Gagal memverifikasi akses" }
        )
    }

    val context = LocalContext.current
    val reportTypes = listOf("Penyewaan (Rentals)", "Barang (Items)", "Pengguna (Users)", "Laporan Barang")
    val dateFilters = listOf("Semua Waktu", "Hari Ini", "7 Hari", "30 Hari", "Bulan Ini")

    val periodLabel = dateFilters[selectedDateFilter]

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            generateAndSavePdf(
                uri = uri,
                reports = reports,
                category = reportTypes[selectedType],
                period = periodLabel,
                statLabel1 = statLabel1,
                statVal1 = statVal1,
                statLabel2 = statLabel2,
                statVal2 = statVal2,
                dateFormat = dateFormat,
                contentResolver = context.contentResolver,
                onError = { showExportError = true; exportErrorMessage = it },
                onSuccess = { showExportError = false }
            )
        }
    }

    fun getFilterStartTime(): Long {
        val cal = java.util.Calendar.getInstance()
        return when (selectedDateFilter) {
            1 -> { // Hari Ini
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            2 -> System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 // 7 Hari
            3 -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 // 30 Hari
            4 -> { // Bulan Ini
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L // Semua Waktu
        }
    }

    fun loadRentalReports(startTime: Long) {
        isLoading = true
        errorMessage = null
        statLabel1 = "Total Sewa"
        statLabel2 = "Disetujui"
        db.collection("rentals").get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<ReportItem>()
                var approvedCount = 0
                var totalCount = 0
                for (doc in documents) {
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    if (createdAt < startTime) continue
                    totalCount++
                    val status = doc.getString("status") ?: "pending"
                    if (status == "approved") approvedCount++
                    list.add(
                        ReportItem(
                            title = "Sewa: ${doc.getString("itemName") ?: "Barang"}",
                            subtitle = "Penyewa: ${doc.getString("renterName") ?: "Penyewa"} - Status: ${status.uppercase()}",
                            dateStr = dateFormat.format(Date(createdAt)),
                            timestamp = createdAt
                        )
                    )
                }
                reports = list.sortedByDescending { it.timestamp }
                statVal1 = totalCount.toString()
                statVal2 = approvedCount.toString()
                isLoading = false
            }
            .addOnFailureListener {
                errorMessage = "Gagal memuat data laporan"
                isLoading = false
            }
    }

    fun loadItemsReport(startTime: Long) {
        isLoading = true
        errorMessage = null
        statLabel1 = "Total Barang"
        statLabel2 = "Tersedia"
        db.collection("items").get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<ReportItem>()
                var availableCount = 0
                var totalCount = 0
                for (doc in documents) {
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    if (createdAt < startTime) continue
                    totalCount++
                    val status = doc.getString("status") ?: "available"
                    if (status == "available") availableCount++
                    list.add(
                        ReportItem(
                            title = "Barang: ${doc.getString("name") ?: "Barang"}",
                            subtitle = "Harga: Rp${(doc.getDouble("price") ?: 0.0).toInt()} - Status: ${status.uppercase()}",
                            dateStr = dateFormat.format(Date(createdAt)),
                            timestamp = createdAt
                        )
                    )
                }
                reports = list.sortedByDescending { it.timestamp }
                statVal1 = totalCount.toString()
                statVal2 = availableCount.toString()
                isLoading = false
            }
            .addOnFailureListener {
                errorMessage = "Gagal memuat data laporan"
                isLoading = false
            }
    }

    fun loadItemReports(startTime: Long) {
        isLoading = true
        errorMessage = null
        statLabel1 = "Total Laporan"
        statLabel2 = "Pending"
        db.collection("item_reports").get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<ReportItem>()
                var pendingCount = 0
                var totalCount = 0
                for (doc in documents) {
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    if (createdAt < startTime) continue
                    totalCount++
                    val status = doc.getString("status") ?: "pending"
                    if (status == "pending") pendingCount++
                    list.add(
                        ReportItem(
                            title = "Laporan Barang: ${doc.getString("itemName") ?: "Barang"}",
                            subtitle = "Pelapor: ${doc.getString("reporterName") ?: "-"} | Alasan: ${doc.getString("reason") ?: "-"} | ${doc.getString("description") ?: ""}",
                            dateStr = dateFormat.format(Date(createdAt)),
                            timestamp = createdAt
                        )
                    )
                }
                reports = list.sortedByDescending { it.timestamp }
                statVal1 = totalCount.toString()
                statVal2 = pendingCount.toString()
                isLoading = false
            }
            .addOnFailureListener {
                errorMessage = "Gagal memuat data laporan"
                isLoading = false
            }
    }

    fun loadUserReports(startTime: Long) {
        isLoading = true
        errorMessage = null
        statLabel1 = "Total User"
        statLabel2 = "Total Staff"
        db.collection("users").get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<ReportItem>()
                var regularUserCount = 0
                var staffCount = 0
                var totalCount = 0
                for (doc in documents) {
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    if (createdAt < startTime) continue
                    totalCount++
                    val role = doc.getString("role") ?: "user"
                    when (role) {
                        "user" -> regularUserCount++
                        "petugas", "admin" -> staffCount++
                    }
                    list.add(
                        ReportItem(
                            title = "User: ${doc.getString("name") ?: "Tanpa Nama"}",
                            subtitle = "Email: ${doc.getString("email") ?: "-"} - Role: ${role.uppercase()}",
                            dateStr = dateFormat.format(Date(createdAt)),
                            timestamp = createdAt
                        )
                    )
                }
                reports = list.sortedByDescending { it.timestamp }
                statVal1 = regularUserCount.toString()
                statVal2 = staffCount.toString()
                isLoading = false
            }
            .addOnFailureListener {
                errorMessage = "Gagal memuat data laporan"
                isLoading = false
            }
    }

    fun loadReports(type: Int, startTime: Long) {
        when (type) {
            0 -> loadRentalReports(startTime)
            1 -> loadItemsReport(startTime)
            2 -> loadUserReports(startTime)
            3 -> loadItemReports(startTime)
        }
    }

    LaunchedEffect(selectedType, selectedDateFilter) {
        loadReports(selectedType, getFilterStartTime())
    }

    if (accessError != null) {
        GalaxyBackground(starAlpha = 0.3f) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppToolbar(title = "Akses Ditolak", onBackClick = onBack)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = accessError!!,
                        color = ErrorColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = reportTypes[selectedType],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Jenis", color = TextHint) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        reportTypes.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label, color = TextDark) },
                                onClick = {
                                    selectedType = index
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedDate,
                    onExpandedChange = { expandedDate = !expandedDate },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = dateFilters[selectedDateFilter],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Periode", color = TextHint) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDate) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDate,
                        onDismissRequest = { expandedDate = false }
                    ) {
                        dateFilters.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label, color = TextDark) },
                                onClick = {
                                    selectedDateFilter = index
                                    expandedDate = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = when (selectedType) { 0 -> Icons.Default.ShoppingCart; 1 -> Icons.Default.Inventory; 2 -> Icons.Default.Person; else -> Icons.Default.Description },
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

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = ErrorColor,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    reports.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada data laporan",
                                color = TextHint,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            items(reports, key = { "${it.title}_${it.timestamp}" }) { report ->
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        Text(
                                            text = report.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = report.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextLight
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = report.dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextHint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
    period: String,
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
    val pageW = 595f
    val pageH = 842f
    val margin = 50f
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), 1).create()
    var pageNumber = 1
    var currentPage = pdfDocument.startPage(pageInfo)
    var canvas = currentPage.canvas
    var yPos = margin

    val primaryColor = android.graphics.Color.rgb(0, 100, 200)
    val darkBg = android.graphics.Color.rgb(30, 30, 60)
    val lightGray = android.graphics.Color.rgb(245, 245, 250)
    val white = android.graphics.Color.WHITE
    val black = android.graphics.Color.BLACK
    val darkGray = android.graphics.Color.DKGRAY

    val titleFont = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    val regularFont = android.graphics.Typeface.DEFAULT

    val bigTitlePaint = android.graphics.Paint().apply {
        textSize = 26f; typeface = titleFont; color = darkBg
    }
    val subTitlePaint = android.graphics.Paint().apply {
        textSize = 11f; typeface = regularFont; color = darkGray
    }
    val sectionPaint = android.graphics.Paint().apply {
        textSize = 14f; typeface = titleFont; color = primaryColor
    }
    val headerCellPaint = android.graphics.Paint().apply {
        textSize = 10f; typeface = titleFont; color = white; isAntiAlias = true
    }
    val cellPaint = android.graphics.Paint().apply {
        textSize = 10f; typeface = regularFont; color = black; isAntiAlias = true
    }
    val lightCellPaint = android.graphics.Paint().apply {
        textSize = 9f; typeface = regularFont; color = darkGray; isAntiAlias = true
    }
    val statValPaint = android.graphics.Paint().apply {
        textSize = 22f; typeface = titleFont; color = primaryColor; isAntiAlias = true
    }
    val statLabelPaint = android.graphics.Paint().apply {
        textSize = 9f; typeface = regularFont; color = darkGray; isAntiAlias = true
    }
    val pageNumPaint = android.graphics.Paint().apply {
        textSize = 9f; typeface = regularFont; color = darkGray; textAlign = android.graphics.Paint.Align.CENTER
    }
    val infoLabelPaint = android.graphics.Paint().apply {
        textSize = 11f; typeface = titleFont; color = darkBg; isAntiAlias = true
    }
    val infoValuePaint = android.graphics.Paint().apply {
        textSize = 11f; typeface = regularFont; color = darkGray; isAntiAlias = true
    }

    fun drawHeader() {
        // Brand bar
        val barPaint = android.graphics.Paint().apply { color = primaryColor }
        canvas.drawRect(0f, 0f, pageW, 6f, barPaint)

        // Title
        yPos = margin
        canvas.drawText("RENTEASE", margin, yPos + 20f, bigTitlePaint)

        // Divider below title
        val dividerPaint = android.graphics.Paint().apply { color = primaryColor; strokeWidth = 2f }
        canvas.drawLine(margin, yPos + 30f, pageW - margin, yPos + 30f, dividerPaint)
        yPos += 48f

        // Info block - two columns
        val col1X = margin
        val col2X = pageW / 2f + 10f

        canvas.drawText("Jenis Laporan", col1X, yPos, infoLabelPaint)
        canvas.drawText(":  $category", col1X + 100f, yPos, infoValuePaint)
        canvas.drawText("Periode", col2X, yPos, infoLabelPaint)
        canvas.drawText(":  $period", col2X + 70f, yPos, infoValuePaint)
        yPos += 18f

        canvas.drawText("Waktu Cetak", col1X, yPos, infoLabelPaint)
        canvas.drawText(":  ${dateFormat.format(Date())}", col1X + 100f, yPos, infoValuePaint)
        canvas.drawText("Jumlah Data", col2X, yPos, infoLabelPaint)
        canvas.drawText(":  ${reports.size} record", col2X + 70f, yPos, infoValuePaint)
        yPos += 30f
    }

    fun drawSummary() {
        val boxW = (pageW - 2 * margin - 20f) / 2f
        val boxH = 55f
        val boxY = yPos
        val radius = 8f

        val bgPaint = android.graphics.Paint().apply { color = lightGray; isAntiAlias = true }
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(220, 220, 230)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Box 1
        canvas.drawRoundRect(margin, boxY, margin + boxW, boxY + boxH, radius, radius, bgPaint)
        canvas.drawRoundRect(margin, boxY, margin + boxW, boxY + boxH, radius, radius, borderPaint)
        canvas.drawText(statVal1, margin + 14f, boxY + 28f, statValPaint)
        canvas.drawText(statLabel1, margin + 14f, boxY + 46f, statLabelPaint)

        // Box 2
        val box2X = margin + boxW + 20f
        canvas.drawRoundRect(box2X, boxY, box2X + boxW, boxY + boxH, radius, radius, bgPaint)
        canvas.drawRoundRect(box2X, boxY, box2X + boxW, boxY + boxH, radius, radius, borderPaint)
        canvas.drawText(statVal2, box2X + 14f, boxY + 28f, statValPaint)
        canvas.drawText(statLabel2, box2X + 14f, boxY + 46f, statLabelPaint)

        yPos += boxH + 24f
    }

    fun drawTableHeader() {
        val cols = listOf(
            30f to "No",
            250f to "Informasi",
            130f to "Detail",
            (pageW - 2 * margin - 30f - 250f - 130f) to "Tanggal"
        )
        val headerBg = android.graphics.Paint().apply { color = darkBg }
        val headerY = yPos
        val rowH = 22f

        canvas.drawRect(margin, headerY, pageW - margin, headerY + rowH, headerBg)
        var xOffset = margin + 6f
        for ((w, label) in cols) {
            canvas.drawText(label, xOffset, headerY + 15f, headerCellPaint)
            xOffset += w
        }
        yPos += rowH + 2f
    }

    fun drawFooter() {
        val footerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(200, 200, 210)
            textSize = 8f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val linePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(220, 220, 230)
            strokeWidth = 0.5f
        }
        canvas.drawLine(margin, pageH - 30f, pageW - margin, pageH - 30f, linePaint)
        canvas.drawText("RentEase — Laporan Sistem — Halaman $pageNumber", pageW / 2f, pageH - 14f, footerPaint)
    }

    fun drawTableBody(): Boolean {
        val rowH = 20f
        val evenBg = android.graphics.Paint().apply { color = lightGray }
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(220, 220, 230)
            strokeWidth = 0.5f
        }

        for ((index, item) in reports.withIndex()) {
            val rowY = yPos
            if (rowY + rowH > pageH - 40f) {
                drawFooter()
                pdfDocument.finishPage(currentPage)
                pageNumber++
                val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), pageNumber).create()
                currentPage = pdfDocument.startPage(newPageInfo)
                canvas = currentPage.canvas
                yPos = margin
                drawHeader()
                canvas.drawText("Lanjutan ...", pageW - margin - 60f, yPos - 18f, subTitlePaint)
                drawSummary()
                drawTableHeader()
                // Re-check row after page break
                if (yPos + rowH > pageH - 40f) return false
            }

            // Row background (alternating)
            if (index % 2 == 1) {
                canvas.drawRect(margin, rowY, pageW - margin, rowY + rowH, evenBg)
            }

            // Cell borders
            canvas.drawLine(margin, rowY, pageW - margin, rowY, borderPaint)

            // Draw cells
            val serial = "${index + 1}."
            canvas.drawText(serial, margin + 8f, rowY + 14f, cellPaint)

            // Truncate text if too long
            val titleText = if (item.title.length > 30) item.title.take(28) + ".." else item.title
            canvas.drawText(titleText, margin + 36f, rowY + 14f, cellPaint)

            val subtitleText = if (item.subtitle.length > 22) item.subtitle.take(20) + ".." else item.subtitle
            canvas.drawText(subtitleText, margin + 280f, rowY + 14f, lightCellPaint)

            canvas.drawText(item.dateStr, pageW - margin - 100f, rowY + 14f, lightCellPaint)

            yPos += rowH
        }
        // Draw bottom border
        canvas.drawLine(margin, yPos, pageW - margin, yPos, borderPaint)
        yPos += 10f
        return true
    }

    // === BUILD PDF ===
    drawHeader()
    canvas.drawText("RINGKASAN", margin, yPos, sectionPaint)
    yPos += 4f
    val thinLine = android.graphics.Paint().apply { color = primaryColor; strokeWidth = 1.5f }
    canvas.drawLine(margin, yPos, margin + 120f, yPos, thinLine)
    yPos += 14f
    drawSummary()

    canvas.drawText("DATA LAPORAN", margin, yPos, sectionPaint)
    yPos += 4f
    canvas.drawLine(margin, yPos, margin + 140f, yPos, thinLine)
    yPos += 14f

    drawTableHeader()
    drawTableBody()
    drawFooter()

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
