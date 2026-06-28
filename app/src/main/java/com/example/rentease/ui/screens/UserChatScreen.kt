package com.example.rentease.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.RentalRequest
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.InfoRow
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore

private const val WA_NUMBER = "6282316627926"

@Composable
fun UserChatScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val context = LocalContext.current
    val rentals = remember { mutableStateListOf<RentalRequest>() }
    var isLoading by remember { mutableStateOf(true) }

    fun loadRentals() {
        val uid = authManager.getCurrentUserUID() ?: return
        isLoading = true
        db.collection("rentals")
            .whereEqualTo("renterId", uid)
            .get()
            .addOnSuccessListener { docs ->
                val result = docs.mapNotNull { doc ->
                    try {
                        doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.createdAt }
                rentals.clear()
                rentals.addAll(result)
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    fun openWhatsApp(itemName: String) {
        val text = "Halo, saya ingin bertanya tentang sewa $itemName"
        val uri = Uri.parse("https://wa.me/$WA_NUMBER?text=${Uri.encode(text)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    LaunchedEffect(Unit) { loadRentals() }

    fun statusBadge(status: String): @Composable () -> Unit = {
        val (label, color) = when (status) {
            RentalRequest.STATUS_PENDING -> "Menunggu" to WarningColor
            RentalRequest.STATUS_APPROVED -> "Disetujui" to SuccessColor
            RentalRequest.STATUS_REJECTED -> "Ditolak" to ErrorColor
            RentalRequest.STATUS_RETURNED -> "Selesai" to Primary
            RentalRequest.STATUS_RETURN_PENDING -> "Menunggu Verifikasi" to WarningColor
            else -> status to TextLight
        }
        RoleBadge(role = label, textColor = color)
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Hubungi Pemilik", onBackClick = onBack)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextLight)
                }
            } else if (rentals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada barang yang disewa",
                        color = TextLight
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rentals) { rental ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            radius = 12.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openWhatsApp(rental.itemName) }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rental.itemName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextDark,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = rental.renterName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextHint,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
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
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tanya via WhatsApp",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Primary,
                                        fontWeight = FontWeight.Medium
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
