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
                context = context,
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
                        Icon(Icons.Default.FileDownload, contentDescription = "Export PDF", tint = com.example.rentease.ui.theme.White)
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
    context: android.content.Context,
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
    val margin = 48f
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), 1).create()
    var pageNumber = 1
    var currentPage = pdfDocument.startPage(pageInfo)
    var canvas = currentPage.canvas
    var yPos = margin

    val primaryBlue = android.graphics.Color.rgb(21, 101, 192)
    val primaryDark = android.graphics.Color.rgb(13, 71, 161)
    val accentColor = android.graphics.Color.rgb(0, 200, 180)
    val darkBg = android.graphics.Color.rgb(25, 35, 55)
    val lightBg = android.graphics.Color.rgb(245, 247, 250)
    val white = android.graphics.Color.WHITE
    val black = android.graphics.Color.rgb(40, 40, 50)
    val darkGray = android.graphics.Color.rgb(100, 110, 125)
    val mediumGray = android.graphics.Color.rgb(180, 190, 200)
    val evenRowBg = android.graphics.Color.rgb(240, 244, 250)
    val statBoxBg = android.graphics.Color.rgb(235, 240, 250)
    val headerBg = android.graphics.Color.rgb(25, 45, 80)

    val boldTypeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    val regularTypeface = android.graphics.Typeface.DEFAULT

    val logoTextPaint = android.graphics.Paint().apply {
        textSize = 22f; typeface = boldTypeface; color = primaryDark; isAntiAlias = true
        letterSpacing = 0.15f
    }
    val logoSubPaint = android.graphics.Paint().apply {
        textSize = 8f; typeface = regularTypeface; color = darkGray; isAntiAlias = true
        letterSpacing = 0.05f
    }
    val headerAccentPaint = android.graphics.Paint().apply {
        textSize = 9f; typeface = boldTypeface; color = accentColor; isAntiAlias = true
    }
    val sectionTitlePaint = android.graphics.Paint().apply {
        textSize = 12f; typeface = boldTypeface; color = primaryDark; isAntiAlias = true
    }
    val infoLabelPaint = android.graphics.Paint().apply {
        textSize = 10f; typeface = boldTypeface; color = darkBg; isAntiAlias = true
    }
    val infoValuePaint = android.graphics.Paint().apply {
        textSize = 10f; typeface = regularTypeface; color = darkGray; isAntiAlias = true
    }
    val statLabelPaint = android.graphics.Paint().apply {
        textSize = 8f; typeface = regularTypeface; color = darkGray; isAntiAlias = true
    }
    val cellPaint = android.graphics.Paint().apply {
        textSize = 9f; typeface = regularTypeface; color = black; isAntiAlias = true
    }
    val lightCellPaint = android.graphics.Paint().apply {
        textSize = 8.5f; typeface = regularTypeface; color = darkGray; isAntiAlias = true
    }
    val watermarkPaint = android.graphics.Paint().apply {
        textSize = 48f; typeface = boldTypeface; color = android.graphics.Color.argb(20, 21, 101, 192); isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val thinLinePaint = android.graphics.Paint().apply { color = primaryBlue; strokeWidth = 1.5f }
    val lightLinePaint = android.graphics.Paint().apply { color = mediumGray; strokeWidth = 0.5f }

    val logoBitmap = try {
        android.graphics.BitmapFactory.decodeResource(context.resources, com.example.rentease.R.drawable.logo_aplikasi)
    } catch (e: Exception) { null }

    fun drawTopBar() {
        val barPaint = android.graphics.Paint().apply { color = primaryBlue }
        val barPaint2 = android.graphics.Paint().apply { color = primaryDark }
        canvas.drawRect(0f, 0f, pageW, 5f, barPaint)
        canvas.drawRect(0f, 5f, pageW, 7f, barPaint2)
    }

    fun drawHeader() {
        yPos = margin + 4f

        // Logo image
        val logoSize = 44f
        val logoY = yPos
        if (logoBitmap != null) {
            val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logoBitmap, logoSize.toInt(), logoSize.toInt(), true)
            canvas.drawBitmap(scaledLogo, margin, logoY, null)
        } else {
            val logoBg = android.graphics.Paint().apply { color = primaryBlue }
            canvas.drawRoundRect(margin, logoY, margin + logoSize, logoY + logoSize, 8f, 8f, logoBg)
            val fallbackPaint = android.graphics.Paint().apply {
                textSize = 22f; typeface = boldTypeface; color = white; isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("RE", margin + 22f, logoY + 30f, fallbackPaint)
        }

        canvas.drawText("RentEase", margin + logoSize + 12f, yPos + 18f, logoTextPaint)
        canvas.drawText("SISTEM INFORMASI RENTAL", margin + logoSize + 12f, yPos + 32f, logoSubPaint)
        canvas.drawText("LAPORAN RESMI", margin + logoSize + 12f, yPos + 42f, headerAccentPaint)

        // Date right-aligned
        val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
        val rightDatePaint = android.graphics.Paint().apply {
            textSize = 9f; typeface = regularTypeface; color = darkGray; isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        canvas.drawText(todayStr, pageW - margin, yPos + 18f, rightDatePaint)

        yPos += 56f

        // Watermark
        canvas.drawText("LAPORAN", pageW / 2f, pageH / 2f - 40f, watermarkPaint)

        // Info card
        val infoCardY = yPos
        val cardPaint = android.graphics.Paint().apply { color = lightBg; isAntiAlias = true }
        val cardBorderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(215, 225, 240)
            style = android.graphics.Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true
        }
        canvas.drawRoundRect(margin, infoCardY, pageW - margin, infoCardY + 56f, 6f, 6f, cardPaint)
        canvas.drawRoundRect(margin, infoCardY, pageW - margin, infoCardY + 56f, 6f, 6f, cardBorderPaint)

        val leftAccent = android.graphics.Paint().apply { color = primaryBlue }
        canvas.drawRect(margin, infoCardY, margin + 4f, infoCardY + 56f, leftAccent)

        val col1X = margin + 16f
        val col2X = pageW / 2f + 8f

        canvas.drawText("Jenis Laporan", col1X, infoCardY + 20f, infoLabelPaint)
        canvas.drawText(":  $category", col1X + 110f, infoCardY + 20f, infoValuePaint)
        canvas.drawText("Periode", col2X, infoCardY + 20f, infoLabelPaint)
        canvas.drawText(":  $period", col2X + 65f, infoCardY + 20f, infoValuePaint)

        canvas.drawText("Waktu Cetak", col1X, infoCardY + 40f, infoLabelPaint)
        canvas.drawText(":  ${SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())}", col1X + 110f, infoCardY + 40f, infoValuePaint)
        canvas.drawText("Jumlah Data", col2X, infoCardY + 40f, infoLabelPaint)
        canvas.drawText(":  ${reports.size} record", col2X + 65f, infoCardY + 40f, infoValuePaint)

        yPos = infoCardY + 68f
    }

    fun drawSummary() {
        val boxW = (pageW - 2 * margin - 16f) / 2f
        val boxH = 52f
        val boxY = yPos

        fun drawStatBox(x: Float, value: String, label: String, valueColor: Int) {
            val bgPaint = android.graphics.Paint().apply { color = statBoxBg; isAntiAlias = true }
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(200, 210, 230)
                style = android.graphics.Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true
            }
            canvas.drawRoundRect(x, boxY, x + boxW, boxY + boxH, 6f, 6f, bgPaint)
            canvas.drawRoundRect(x, boxY, x + boxW, boxY + boxH, 6f, 6f, borderPaint)

            val topAccent = android.graphics.Paint().apply { color = valueColor }
            canvas.drawRect(x + 12f, boxY, x + 40f, boxY + 3f, topAccent)

            val valPaint = android.graphics.Paint().apply {
                textSize = 22f; typeface = boldTypeface; color = valueColor; isAntiAlias = true
            }
            canvas.drawText(value, x + 14f, boxY + 30f, valPaint)

            canvas.drawText(label, x + 14f, boxY + 44f, statLabelPaint)
        }

        drawStatBox(margin, statVal1, statLabel1, primaryBlue)
        drawStatBox(margin + boxW + 16f, statVal2, statLabel2, accentColor)

        yPos += boxH + 20f
    }

    fun drawSectionTitle(title: String) {
        canvas.drawText(title.uppercase(), margin, yPos, sectionTitlePaint)
        yPos += 3f
        canvas.drawLine(margin, yPos, margin + 100f, yPos, thinLinePaint)
        yPos += 12f
    }

    fun drawTableHeader() {
        val cols = listOf(
            28f to "No",
            220f to "Informasi",
            170f to "Detail",
            (pageW - 2 * margin - 28f - 220f - 170f) to "Tanggal"
        )
        val headerY = yPos
        val rowH = 20f

        canvas.drawRoundRect(margin, headerY, pageW - margin, headerY + rowH, 4f, 4f, android.graphics.Paint().apply { color = headerBg })
        val headerPaint = android.graphics.Paint().apply {
            color = headerBg; isAntiAlias = true
        }
        canvas.drawRect(margin, headerY + 4f, pageW - margin, headerY + rowH, headerPaint)

        var xOffset = margin + 8f
        for ((w, label) in cols) {
            val colHeaderPaint = android.graphics.Paint().apply {
                textSize = 9f; typeface = boldTypeface; color = white; isAntiAlias = true
            }
            canvas.drawText(label, xOffset, headerY + 13.5f, colHeaderPaint)
            xOffset += w
        }
        yPos += rowH + 1f
    }

    fun drawFooter() {
        val linePaint2 = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(200, 210, 225)
            strokeWidth = 1f
        }
        canvas.drawLine(margin, pageH - 32f, pageW - margin, pageH - 32f, linePaint2)

        val leftFooterPaint = android.graphics.Paint().apply {
            textSize = 7f; typeface = regularTypeface; color = mediumGray; isAntiAlias = true
        }
        canvas.drawText("RentEase — Laporan Resmi Sistem", margin, pageH - 16f, leftFooterPaint)

        val pagePaint = android.graphics.Paint().apply {
            textSize = 8f; typeface = boldTypeface; color = primaryBlue; isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("Halaman $pageNumber", pageW / 2f, pageH - 16f, pagePaint)

        val rightFooterPaint = android.graphics.Paint().apply {
            textSize = 7f; typeface = regularTypeface; color = mediumGray; isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        canvas.drawText(dateFormat.format(Date()), pageW - margin, pageH - 16f, rightFooterPaint)
    }

    fun drawTableBody(): Boolean {
        val rowH = 18f

        for ((index, item) in reports.withIndex()) {
            val rowY = yPos
            if (rowY + rowH > pageH - 42f) {
                drawFooter()
                pdfDocument.finishPage(currentPage)
                pageNumber++
                val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), pageNumber).create()
                currentPage = pdfDocument.startPage(newPageInfo)
                canvas = currentPage.canvas
                yPos = margin
                drawHeader()
                drawSectionTitle("DATA LAPORAN (Lanjutan)")
                drawTableHeader()
                if (yPos + rowH > pageH - 42f) return false
            }

            if (index % 2 == 1) {
                canvas.drawRoundRect(margin, rowY, pageW - margin, rowY + rowH, 3f, 3f, android.graphics.Paint().apply {
                    color = evenRowBg; isAntiAlias = true
                })
            }

            canvas.drawLine(margin, rowY, pageW - margin, rowY, lightLinePaint)

            val serial = "${index + 1}."
            canvas.drawText(serial, margin + 10f, rowY + 13f, cellPaint)

            val titleText = if (item.title.length > 35) item.title.take(33) + ".." else item.title
            canvas.drawText(titleText, margin + 34f, rowY + 13f, cellPaint)

            val subtitleText = if (item.subtitle.length > 28) item.subtitle.take(26) + ".." else item.subtitle
            canvas.drawText(subtitleText, margin + 248f, rowY + 13f, lightCellPaint)

            canvas.drawText(item.dateStr, pageW - margin - 90f, rowY + 13f, lightCellPaint)

            yPos += rowH
        }
        canvas.drawLine(margin, yPos, pageW - margin, yPos, lightLinePaint)
        yPos += 8f
        return true
    }

    // === BUILD PDF ===
    drawTopBar()
    drawHeader()

    drawSectionTitle("Ringkasan")
    drawSummary()

    drawSectionTitle("Data Laporan")
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
