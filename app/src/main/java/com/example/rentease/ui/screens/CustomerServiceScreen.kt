package com.example.rentease.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextAlign
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
fun CustomerServiceScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    var allTickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf("user") }
    var currentTab by remember { mutableStateOf(0) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    val tabs = listOf("Terbuka", "Diproses", "Selesai")
    val tabStatuses = listOf(SupportTicket.STATUS_OPEN, SupportTicket.STATUS_IN_PROGRESS, SupportTicket.STATUS_RESOLVED)

    val filteredTickets = allTickets.filter { it.status == tabStatuses[currentTab] }

    LaunchedEffect(Unit) {
        authManager.getUserData(
            onSuccess = { data ->
                userRole = data["role"] as? String ?: "user"
            },
            onFailure = { userRole = "user" }
        )
    }

    DisposableEffect(Unit) {
        val listenerRegistration = db.collection("support_tickets")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    errorMessage = "Gagal memuat data: ${error.localizedMessage}"
                    loading = false
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    allTickets = snapshots.documents.mapNotNull {
                        it.toObject(SupportTicket::class.java)?.copy(id = it.id)
                    }
                    loading = false
                    errorMessage = null
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

            TabRow(
                selectedTabIndex = currentTab,
                containerColor = TechCardBg,
                contentColor = Primary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                        height = 3.dp,
                        color = Primary
                    )
                },
                divider = { Spacer(modifier = Modifier.height(0.dp)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTab == index) Primary else TextLight
                            )
                        },
                        selectedContentColor = Primary,
                        unselectedContentColor = TextLight
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    loading -> {
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
                    filteredTickets.isEmpty() -> {
                        val emptyMsg = when (currentTab) {
                            0 -> "Belum ada laporan masuk"
                            1 -> "Belum ada laporan yang diproses"
                            else -> "Belum ada laporan selesai"
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emptyMsg,
                                color = TextHint,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredTickets, key = { it.id }) { ticket ->
                                val statusColor = when (ticket.status) {
                                    SupportTicket.STATUS_RESOLVED -> SuccessColor
                                    SupportTicket.STATUS_IN_PROGRESS -> WarningColor
                                    else -> Primary
                                }
                                val statusLabel = when (ticket.status) {
                                    SupportTicket.STATUS_RESOLVED -> "Selesai"
                                    SupportTicket.STATUS_IN_PROGRESS -> "Diproses"
                                    else -> "Terbuka"
                                }
                                GlowCard(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        navController.navigate("ticket_detail/${ticket.id}")
                                    },
                                    radius = 12.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                ticket.subject,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextDark,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "${ticket.userName} — ${dateFormat.format(Date(ticket.createdAt))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextHint
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                statusLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold
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
