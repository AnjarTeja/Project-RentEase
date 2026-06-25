package com.example.rentease.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.SuccessColor
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
fun CustomerServiceScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var userRole by remember { mutableStateOf("petugas") }

    LaunchedEffect(Unit) {
        authManager.getUserData(
            onSuccess = { data ->
                userRole = data["role"] as? String ?: "petugas"
            },
            onFailure = { userRole = "petugas" }
        )
    }

    DisposableEffect(Unit) {
        val listenerRegistration = db.collection("support_tickets")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    tickets = snapshots.documents.mapNotNull {
                        it.toObject(SupportTicket::class.java)?.copy(id = it.id)
                    }
                    loading = false
                }
            }
        onDispose {
            listenerRegistration.remove()
        }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            val roleTitle = if (userRole == "admin") "Admin - Layanan Pelanggan" else "Layanan Pelanggan"
            AppToolbar(title = roleTitle, onBackClick = onBack)

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextHint)
                }
            } else if (tickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada laporan masuk", color = TextHint)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tickets) { ticket ->
                        val statusColor = when (ticket.status) {
                            SupportTicket.STATUS_RESOLVED -> SuccessColor
                            SupportTicket.STATUS_IN_PROGRESS -> WarningColor
                            else -> Primary
                        }
                        GlowCard(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navController.navigate("ticket_detail/${ticket.id}")
                            },
                            radius = 12.dp,
                            borderColor = if (userRole == "admin") Primary else statusColor
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ticket.subject,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row {
                                        Text(ticket.userName, style = MaterialTheme.typography.labelSmall, color = TextLight)
                                        Text(" | ", style = MaterialTheme.typography.labelSmall, color = TextHint)
                                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                        Text(sdf.format(Date(ticket.createdAt)), style = MaterialTheme.typography.labelSmall, color = TextHint)
                                    }
                                }
                                GlassCard(modifier = Modifier, radius = 8.dp) {
                                    Text(
                                        ticket.status.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
