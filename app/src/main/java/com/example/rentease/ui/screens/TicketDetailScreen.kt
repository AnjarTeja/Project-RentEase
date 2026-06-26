package com.example.rentease.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.SupportTicket
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.InfoRow
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TicketDetailScreen(
    ticketId: String,
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    var ticket by remember { mutableStateOf<SupportTicket?>(null) }
    var loading by remember { mutableStateOf(true) }
    var replyText by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var petugasName by remember { mutableStateOf("Petugas") }
    var isStaff by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role") ?: ""
                    isStaff = role == "petugas" || role == "admin"
                    petugasName = doc.getString("name") ?: "Petugas"
                }
        }
    }

    DisposableEffect(ticketId) {
        val listener = db.collection("support_tickets").document(ticketId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Gagal memuat tiket"
                    loading = false
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    ticket = snapshot.toObject(SupportTicket::class.java)?.copy(id = snapshot.id)
                    loading = false
                    ticket?.let {
                        if (it.status == SupportTicket.STATUS_RESOLVED) {
                            replyText = it.replyMessage
                        }
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    errorMessage = "Tiket tidak ditemukan"
                    loading = false
                }
            }
        onDispose { listener.remove() }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Detail Tiket", onBackClick = onBack)

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else if (ticket == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tiket tidak ditemukan", color = ErrorColor)
                }
            } else {
                val t = ticket!!
                val isResolved = t.status == SupportTicket.STATUS_RESOLVED
                val statusColor = when (t.status) {
                    SupportTicket.STATUS_RESOLVED -> SuccessColor
                    SupportTicket.STATUS_IN_PROGRESS -> WarningColor
                    else -> Primary
                }
                val statusLabel = when (t.status) {
                    SupportTicket.STATUS_RESOLVED -> "Selesai"
                    SupportTicket.STATUS_IN_PROGRESS -> "Diproses"
                    else -> "Terbuka"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlowCard(modifier = Modifier.fillMaxWidth(), radius = 12.dp) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    t.subject,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        statusLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            InfoRow(icon = Icons.Default.Person, label = "Nama", value = t.userName)
                            InfoRow(icon = Icons.Default.Email, label = "Email", value = t.userEmail)
                            InfoRow(icon = Icons.Default.CalendarMonth, label = "Tanggal", value = sdf.format(Date(t.createdAt)))
                        }
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth(), radius = 12.dp) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Pesan",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextDark,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(t.message, style = MaterialTheme.typography.bodyMedium, color = TextLight)
                        }
                    }

                    if (isResolved && t.replyMessage.isNotEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), radius = 12.dp) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Tanggapan Petugas",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SuccessColor,
                                    fontWeight = FontWeight.Medium
                                )
                                if (t.repliedByName.isNotEmpty()) {
                                    Text(
                                        "Oleh: ${t.repliedByName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextHint
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(t.replyMessage, style = MaterialTheme.typography.bodyMedium, color = TextLight)
                            }
                        }
                    }

                    if (isStaff && !isResolved) {
                        Text(
                            "Berikan Tanggapan",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextDark,
                            fontWeight = FontWeight.Medium
                        )
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            label = { Text("Tulis tanggapan...") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = TextHint,
                                focusedLabelColor = Primary,
                                unfocusedLabelColor = TextHint,
                                cursorColor = Primary,
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark
                            )
                        )

                        GlowButton(
                            text = if (submitting) "Mengirim..." else "Kirim Tanggapan",
                            onClick = {
                                if (replyText.isEmpty()) return@GlowButton
                                val petugasId = auth.currentUser?.uid ?: return@GlowButton
                                submitting = true
                                val updates = mapOf(
                                    "replyMessage" to replyText,
                                    "repliedAt" to System.currentTimeMillis(),
                                    "repliedBy" to petugasId,
                                    "repliedByName" to petugasName,
                                    "status" to SupportTicket.STATUS_RESOLVED
                                )
                                db.collection("support_tickets").document(ticketId)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        submitting = false
                                        onBack()
                                    }
                                    .addOnFailureListener { submitting = false }
                            },
                            enabled = !submitting
                        )
                    }

                    if (isResolved) {
                        GlowButton(
                            text = "Laporan Selesai",
                            onClick = {},
                            enabled = false,
                            backgroundColor = SuccessColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
