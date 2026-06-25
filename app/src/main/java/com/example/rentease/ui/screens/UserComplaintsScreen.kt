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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.Complaint
import com.google.firebase.auth.FirebaseAuth
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.components.StatItem
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

    val filteredComplaints = remember(allComplaints, searchQuery) {
        if (searchQuery.isBlank()) allComplaints
        else allComplaints.filter {
            it.subject.contains(searchQuery, ignoreCase = true) ||
            it.message.contains(searchQuery, ignoreCase = true) ||
            it.userName.contains(searchQuery, ignoreCase = true)
        }
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

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Keluhan Pengguna", onBackClick = onBack)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItem(value = openCount.toString(), label = "Terbuka", color = ErrorColor, modifier = Modifier.weight(1f))
                StatItem(value = progressCount.toString(), label = "Diproses", color = WarningColor, modifier = Modifier.weight(1f))
                StatItem(value = resolvedCount.toString(), label = "Selesai", color = SuccessColor, modifier = Modifier.weight(1f))
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

            Box(modifier = Modifier.weight(1f)) {
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
                        Text(
                            text = if (searchQuery.isBlank()) "Belum ada keluhan" else "Tidak ditemukan keluhan",
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
                                    selectedComplaint = complaint
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
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                GlowButton(
                                                    text = "Balas",
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

    if (showRespondDialog && selectedComplaint != null) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) { showRespondDialog = false; replyText = "" } },
            title = { Text("Balas Keluhan: ${selectedComplaint!!.subject}", color = TextDark) },
            text = {
                Column {
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
