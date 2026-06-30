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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.navigation.NavHostController
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.RentalRequest
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.InfoRow
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.example.rentease.Item
import com.example.rentease.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Locale

@Composable
fun IncomingRentalsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val context = LocalContext.current
    val tabTitles = listOf("Semua", "Pending", "Disetujui")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val rentals = remember { mutableStateListOf<RentalRequest>() }
    var isLoading by remember { mutableStateOf(true) }
    val listenerReg = remember { mutableStateOf<ListenerRegistration?>(null) }
    val rentalsFull = remember { mutableStateListOf<RentalRequest>() }

    fun applyFilter() {
        val filterStatuses: Set<String> = when (selectedTabIndex) {
            0 -> emptySet()
            1 -> setOf(RentalRequest.STATUS_PENDING)
            2 -> setOf(RentalRequest.STATUS_APPROVED, RentalRequest.STATUS_RETURN_PENDING)
            else -> emptySet()
        }
        isLoading = true
        val filtered = if (filterStatuses.isEmpty()) rentalsFull.toList()
            else rentalsFull.filter { it.status in filterStatuses }
        rentals.clear()
        rentals.addAll(filtered.sortedByDescending { it.createdAt })
        isLoading = false
    }

    DisposableEffect(Unit) {
        val uid = authManager.getCurrentUserUID()
        if (uid != null) {
            val reg = db.collection("rentals")
                .whereEqualTo("ownerId", uid)
                .addSnapshotListener { snapshots, e ->
                    if (e != null || snapshots == null) return@addSnapshotListener
                    val result = snapshots.mapNotNull { doc ->
                        try {
                            doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                        } catch (e: Exception) { null }
                    }
                    rentalsFull.clear()
                    rentalsFull.addAll(result.sortedByDescending { it.createdAt })
                    applyFilter()
                }
            listenerReg.value = reg
        }
        onDispose { listenerReg.value?.remove() }
    }

    LaunchedEffect(selectedTabIndex) {
        applyFilter()
    }

    fun updateStatus(rental: RentalRequest, newStatus: String) {
        if (newStatus == RentalRequest.STATUS_APPROVED && rental.itemId.isNotEmpty()) {
            db.collection("items").document(rental.itemId).get()
                .addOnSuccessListener { doc ->
                    val currentStock = doc.getLong("stock")?.toInt() ?: 1
                    if (currentStock <= 0) {
                        Toast.makeText(context, "Stok barang habis, tidak dapat menyetujui", Toast.LENGTH_SHORT).show()
                        applyFilter()
                        return@addOnSuccessListener
                    }
                    val newStock = currentStock - 1
                    val batch = db.batch()
                    val rentalRef = db.collection("rentals").document(rental.id)
                    batch.update(rentalRef, "status", newStatus)
                    batch.update(rentalRef, "updatedAt", System.currentTimeMillis())

                    val itemRef = db.collection("items").document(rental.itemId)
                    batch.update(itemRef, "stock", newStock)
                    batch.update(itemRef, "rentCount", com.google.firebase.firestore.FieldValue.increment(1))
                    if (newStock <= 0) batch.update(itemRef, "status", Item.STATUS_RENTED)

                    batch.commit().addOnSuccessListener {
                        NotificationHelper.showRentalStatusNotification(context, rental.itemName, newStatus)
                        applyFilter()
                    }
                }
        } else {
            val batch = db.batch()
            val rentalRef = db.collection("rentals").document(rental.id)
            batch.update(rentalRef, "status", newStatus)
            batch.update(rentalRef, "updatedAt", System.currentTimeMillis())
            batch.commit().addOnSuccessListener {
                NotificationHelper.showRentalStatusNotification(context, rental.itemName, newStatus)
                applyFilter()
            }
        }
    }

    fun statusBadge(status: String): @Composable () -> Unit = {
        val (label, color) = when (status) {
            RentalRequest.STATUS_PENDING -> "Pending" to WarningColor
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
            AppToolbar(title = "Penyewaan Masuk", onBackClick = onBack)

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = com.example.rentease.ui.theme.TechCardBg,
                contentColor = Primary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Primary
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, color = if (selectedTabIndex == index) Primary else TextLight) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextLight)
                }
            } else if (rentals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        when (selectedTabIndex) {
                            0 -> "Belum ada permintaan sewa"
                            1 -> "Belum ada permintaan pending"
                            2 -> "Belum ada permintaan disetujui"
                            else -> ""
                        },
                        color = TextLight
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rentals) { rental ->
                        GlowCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                    icon = Icons.Default.Person,
                                    label = "Penyewa",
                                    value = rental.renterName,
                                    iconTint = Primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                InfoRow(
                                    icon = Icons.Default.CalendarMonth,
                                    label = "Durasi",
                                    value = "${rental.duration} hari (${rental.startDate} - ${rental.endDate})",
                                    iconTint = Primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                InfoRow(
                                    icon = Icons.Default.ShoppingCart,
                                    label = "Total",
                                    value = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(rental.pricePerDay * rental.duration),
                                    iconTint = Primary
                                )
                                if (rental.status == RentalRequest.STATUS_PENDING) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        GlowButton(
                                            text = "Setujui",
                                            onClick = { updateStatus(rental, RentalRequest.STATUS_APPROVED) },
                                            modifier = Modifier.weight(1f),
                                            backgroundColor = SuccessColor
                                        )
                                        GlowButton(
                                            text = "Tolak",
                                            onClick = { updateStatus(rental, RentalRequest.STATUS_REJECTED) },
                                            modifier = Modifier.weight(1f),
                                            backgroundColor = ErrorColor
                                        )
                                    }
                                }
                                if (rental.status == RentalRequest.STATUS_RETURN_PENDING) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        GlowButton(
                                            text = "Konfirmasi Kembali",
                                            onClick = { updateStatus(rental, RentalRequest.STATUS_RETURNED) },
                                            modifier = Modifier.weight(1f),
                                            backgroundColor = SuccessColor
                                        )
                                        GlowButton(
                                            text = "Laporkan",
                                            onClick = { Toast.makeText(context, "Hubungi admin untuk bantuan", Toast.LENGTH_SHORT).show() },
                                            modifier = Modifier.weight(1f),
                                            backgroundColor = ErrorColor
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
