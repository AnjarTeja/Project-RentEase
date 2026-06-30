package com.example.rentease.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.ListenerRegistration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ImageUploadHelper
import com.example.rentease.RentalRequest
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.InfoRow
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
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

    DisposableEffect(Unit) {
        val uid = authManager.getCurrentUserUID()
        val reg = if (uid != null) {
            db.collection("rentals")
                .whereEqualTo("renterId", uid)
                .addSnapshotListener { snapshots, e ->
                    if (e != null || snapshots == null) return@addSnapshotListener
                    val result = snapshots.mapNotNull { doc ->
                        try { doc.toObject(RentalRequest::class.java).copy(id = doc.id) }
                        catch (_: Exception) { null }
                    }.filter { it.status in listOf(RentalRequest.STATUS_RETURNED, RentalRequest.STATUS_REJECTED) }
                        .sortedByDescending { it.updatedAt }
                    historyItems.clear()
                    historyItems.addAll(result)
                    isLoading = false
                }
        } else null
        onDispose { reg?.remove() }
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
                        GlowCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(Screen.ItemDetail.createRoute(rental.itemId)) }
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (rental.itemImageUrl.isNotEmpty()) {
                                        val imageModel = remember(rental.itemImageUrl) { ImageUploadHelper.imageModelFromUrl(rental.itemImageUrl) }
                                        AsyncImage(
                                            model = imageModel,
                                            contentDescription = rental.itemName,
                                            modifier = Modifier
                                                .width(60.dp).height(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(TechCardBg),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .width(60.dp).height(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Inventory,
                                                contentDescription = null,
                                                tint = TextHint.copy(alpha = 0.4f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
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
                                if (rental.pricePerDay > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    InfoRow(
                                        icon = Icons.Default.AttachMoney,
                                        label = "Total",
                                        value = "Rp ${String.format("%,.0f", rental.pricePerDay * rental.duration)}",
                                        iconTint = Primary
                                    )
                                }
                                if (rental.isOverdue && rental.fineAmount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Terlambat ${rental.overdueDays} hari | Denda: Rp ${String.format("%,.0f", rental.fineAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ErrorColor,
                                        modifier = Modifier.padding(start = 4.dp)
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
