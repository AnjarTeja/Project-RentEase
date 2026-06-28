package com.example.rentease.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.Complaint
import com.example.rentease.FirebaseAuthManager
import com.google.firebase.auth.FirebaseAuth
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserComplaintsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var adminName by remember { mutableStateOf("Admin") }
    var allComplaints by remember { mutableStateOf(listOf<Complaint>()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedComplaint by remember { mutableStateOf<Complaint?>(null) }
    var showRespondDialog by remember { mutableStateOf(false) }
    var showResolveDialog by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var accessError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val authManager = FirebaseAuthManager()
        authManager.getUserRole(
            onSuccess = { role ->
                if (role != "admin") {
                    accessError = "Anda tidak memiliki akses ke halaman ini"
                }
            },
            onFailure = { accessError = "Gagal memverifikasi akses" }
        )
    }

    val filteredComplaints = remember(allComplaints, searchQuery, statusFilter) {
        val bySearch = if (searchQuery.isBlank()) allComplaints
        else allComplaints.filter {
            it.subject.contains(searchQuery, ignoreCase = true) ||
            it.message.contains(searchQuery, ignoreCase = true) ||
            it.userName.contains(searchQuery, ignoreCase = true)
        }
        if (statusFilter == null) bySearch
        else bySearch.filter { it.status == statusFilter }
    }

    val openCount = remember(allComplaints) { allComplaints.count { it.status == Complaint.STATUS_OPEN } }
    val progressCount = remember(allComplaints) { allComplaints.count { it.status == Complaint.STATUS_IN_PROGRESS } }
    val resolvedCount = remember(allComplaints) { allComplaints.count { it.status == Complaint.STATUS_RESOLVED } }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    adminName = doc.getString("name") ?: "Admin"
                }
        }
    }

    DisposableEffect(Unit) {
        val listener = db.collection("complaints")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    db.collection("complaints").get()
                        .addOnSuccessListener { snap ->
                            val sorted = snap.sortedByDescending { it.getLong("createdAt") ?: 0L }
                            processComplaints(sorted) { allComplaints = it; isLoading = false }
                        }
                        .addOnFailureListener {
                            errorMessage = "Gagal memuat keluhan"
                            isLoading = false
                        }
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    processComplaints(snapshots.documents) { allComplaints = it; isLoading = false }
                }
            }
        onDispose { listener.remove() }
    }

    fun sendReply() {
        val complaint = selectedComplaint ?: return
        if (replyText.isBlank()) return
        isProcessing = true
        val updates = mapOf<String, Any>(
            "replyMessage" to replyText.trim(),
            "repliedAt" to System.currentTimeMillis(),
            "repliedBy" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
            "repliedByName" to adminName,
            "status" to Complaint.STATUS_IN_PROGRESS
        )
        db.collection("complaints").document(complaint.id)
            .update(updates)
            .addOnSuccessListener {
                isProcessing = false
                showRespondDialog = false
                selectedComplaint = null
                replyText = ""
            }
            .addOnFailureListener { isProcessing = false }
    }

    fun resolveComplaint() {
        val complaint = selectedComplaint ?: return
        isProcessing = true
        db.collection("complaints").document(complaint.id)
            .update("status", Complaint.STATUS_RESOLVED)
            .addOnSuccessListener {
                isProcessing = false
                showResolveDialog = false
                selectedComplaint = null
            }
            .addOnFailureListener { isProcessing = false }
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
                AppToolbar(title = "Keluhan Pengguna", onBackClick = onBack)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterStatItem(
                    value = openCount.toString(),
                    label = "Terbuka",
                    color = ErrorColor,
                    isActive = statusFilter == Complaint.STATUS_OPEN,
                    onClick = {
                        statusFilter = if (statusFilter == Complaint.STATUS_OPEN) null else Complaint.STATUS_OPEN
                        selectedComplaint = null
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterStatItem(
                    value = progressCount.toString(),
                    label = "Diproses",
                    color = WarningColor,
                    isActive = statusFilter == Complaint.STATUS_IN_PROGRESS,
                    onClick = {
                        statusFilter = if (statusFilter == Complaint.STATUS_IN_PROGRESS) null else Complaint.STATUS_IN_PROGRESS
                        selectedComplaint = null
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterStatItem(
                    value = resolvedCount.toString(),
                    label = "Selesai",
                    color = SuccessColor,
                    isActive = statusFilter == Complaint.STATUS_RESOLVED,
                    onClick = {
                        statusFilter = if (statusFilter == Complaint.STATUS_RESOLVED) null else Complaint.STATUS_RESOLVED
                        selectedComplaint = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari keluhan...", color = TextHint) },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null, tint = TextHint) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                    cursorColor = Primary,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                )
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            color = ErrorColor,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    filteredComplaints.isEmpty() -> {
                        val emptyMsg = when {
                            searchQuery.isNotBlank() -> "Tidak ditemukan keluhan"
                            statusFilter != null -> "Tidak ada keluhan dengan status ini"
                            else -> "Belum ada keluhan"
                        }
                        Text(
                            text = emptyMsg,
                            color = TextHint,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(filteredComplaints, key = { it.id }) { complaint ->
                                GlassCard(modifier = Modifier.fillMaxWidth().clickable {
                                    selectedComplaint = if (selectedComplaint?.id == complaint.id) null else complaint
                                }) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = complaint.subject,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextDark,
                                                modifier = Modifier.weight(1f)
                                            )
                                            val statusColor = when (complaint.status) {
                                                Complaint.STATUS_OPEN -> ErrorColor
                                                Complaint.STATUS_IN_PROGRESS -> WarningColor
                                                else -> SuccessColor
                                            }
                                            val statusLabel = when (complaint.status) {
                                                Complaint.STATUS_OPEN -> "Terbuka"
                                                Complaint.STATUS_IN_PROGRESS -> "Diproses"
                                                else -> "Selesai"
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(statusColor.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        Text(text = complaint.userName, style = MaterialTheme.typography.bodySmall, color = TextLight)
                                        Text(
                                            text = dateFormat.format(Date(complaint.createdAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextHint
                                        )

                                        if (selectedComplaint?.id == complaint.id) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = Primary.copy(alpha = 0.1f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(text = "Email: ${complaint.userEmail}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = complaint.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextLight
                                            )

                                            if (complaint.replyMessage.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                HorizontalDivider(color = Primary.copy(alpha = 0.1f))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(text = "Balasan Admin:", style = MaterialTheme.typography.labelSmall, color = Primary)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = complaint.replyMessage,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextLight
                                                )
                                                if (complaint.repliedByName.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "— ${complaint.repliedByName}, ${dateFormat.format(Date(complaint.repliedAt))}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextHint
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                GlowButton(
                                                    text = if (complaint.replyMessage.isBlank()) "Balas" else "Edit Balasan",
                                                    onClick = {
                                                        selectedComplaint = complaint
                                                        replyText = complaint.replyMessage
                                                        showRespondDialog = true
                                                    },
                                                    backgroundColor = Primary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (complaint.status != Complaint.STATUS_RESOLVED) {
                                                    GlowButton(
                                                        text = "Selesaikan",
                                                        onClick = {
                                                            selectedComplaint = complaint
                                                            showResolveDialog = true
                                                        },
                                                        backgroundColor = SuccessColor,
                                                        modifier = Modifier.weight(1f)
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
        }
    }
    }

    if (showRespondDialog && selectedComplaint != null) {
        val c = selectedComplaint!!
        val isEdit = c.replyMessage.isNotBlank()
        AlertDialog(
            onDismissRequest = { if (!isProcessing) { showRespondDialog = false; replyText = "" } },
            title = { Text("${if (isEdit) "Edit" else "Balas"} Keluhan", color = TextDark) },
            text = {
                Column {
                    Text(
                        text = c.subject,
                        style = MaterialTheme.typography.titleSmall,
                        color = Primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dari: ${c.userName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = c.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Primary.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEdit) "Edit balasan Anda:" else "Tulis balasan Anda:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Tulis balasan...", color = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { sendReply() }, enabled = !isProcessing) {
                    Text(if (isProcessing) "Mengirim..." else "Kirim", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRespondDialog = false; replyText = "" }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = TechCardBg
        )
    }

    if (showResolveDialog && selectedComplaint != null) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) { showResolveDialog = false } },
            title = { Text("Selesaikan Keluhan", color = TextDark) },
            text = {
                Text("Tandai keluhan \"${selectedComplaint!!.subject}\" sebagai selesai?", color = TextLight)
            },
            confirmButton = {
                TextButton(onClick = { resolveComplaint() }, enabled = !isProcessing) {
                    Text(if (isProcessing) "Memproses..." else "Ya, Selesai", color = SuccessColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = TechCardBg
        )
    }
}

@Composable
private fun FilterStatItem(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) color else color.copy(alpha = 0f)
    GlassCard(
        modifier = modifier.clickable { onClick() },
        radius = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isActive) Modifier.background(color.copy(alpha = 0.08f))
                    else Modifier
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isActive) color else color.copy(alpha = 0.6f),
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) TextLight else TextHint
                )
            }
        }
    }
}

private fun processComplaints(
    documents: List<com.google.firebase.firestore.DocumentSnapshot>,
    onResult: (List<Complaint>) -> Unit
) {
    val list = documents.mapNotNull { doc ->
        try {
            Complaint(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                userName = doc.getString("userName") ?: "",
                userEmail = doc.getString("userEmail") ?: "",
                subject = doc.getString("subject") ?: "",
                message = doc.getString("message") ?: "",
                status = doc.getString("status") ?: Complaint.STATUS_OPEN,
                createdAt = doc.getLong("createdAt") ?: 0L,
                replyMessage = doc.getString("replyMessage") ?: "",
                repliedAt = doc.getLong("repliedAt") ?: 0L,
                repliedBy = doc.getString("repliedBy") ?: "",
                repliedByName = doc.getString("repliedByName") ?: ""
            )
        } catch (_: Exception) { null }
    }
    onResult(list)
}
