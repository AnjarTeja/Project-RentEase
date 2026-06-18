package com.example.rentease.ui.screens

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.RentalRequest
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.InfoRow
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val historyItems = remember { mutableStateListOf<RentalRequest>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = authManager.getCurrentUserUID() ?: return@LaunchedEffect

        db.collection("rentals")
            .whereEqualTo("renterId", uid)
            .whereIn("status", listOf(RentalRequest.STATUS_RETURNED, RentalRequest.STATUS_REJECTED))
            .get()
            .addOnSuccessListener { docs ->
                val result = docs.mapNotNull { doc ->
                    try {
                        doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.updatedAt }
                historyItems.clear()
                historyItems.addAll(result)
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    fun statusBadge(status: String): @Composable () -> Unit = {
        val (label, color) = when (status) {
            RentalRequest.STATUS_RETURNED -> "Selesai" to Primary
            RentalRequest.STATUS_REJECTED -> "Ditolak" to ErrorColor
            else -> status to WarningColor
        }
        RoleBadge(role = label, textColor = color)
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Riwayat", onBackClick = onBack)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextLight)
                }
            } else if (historyItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada riwayat", color = TextLight)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyItems) { rental ->
                        GlowCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (rental.itemImageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = rental.itemImageUrl,
                                            contentDescription = rental.itemName,
                                            modifier = Modifier.width(60.dp).height(60.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Text(
                                        text = rental.itemName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    statusBadge(rental.status)()
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoRow(
                                    icon = Icons.Default.CalendarMonth,
                                    label = "Tanggal",
                                    value = "${rental.startDate} - ${rental.endDate}",
                                    iconTint = Primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                InfoRow(
                                    icon = Icons.Default.ShoppingCart,
                                    label = "Durasi",
                                    value = "${rental.duration} hari",
                                    iconTint = Primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
