package com.example.rentease.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.ListenerRegistration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.RentalRequest
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.InfoRow
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MyTransactionsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val context = LocalContext.current
    val tabTitles = listOf("Pending", "Aktif", "Riwayat")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val transactions = remember { mutableStateListOf<RentalRequest>() }
    val allRentals = remember { mutableStateListOf<RentalRequest>() }
    var isLoading by remember { mutableStateOf(true) }
    var cancelTarget by remember { mutableStateOf<RentalRequest?>(null) }
    val listenerReg = remember { mutableStateOf<ListenerRegistration?>(null) }

    fun applyFilter() {
        val statusList = when (selectedTabIndex) {
            0 -> listOf(RentalRequest.STATUS_PENDING)
            1 -> listOf(RentalRequest.STATUS_APPROVED, RentalRequest.STATUS_RETURN_PENDING)
            2 -> listOf(RentalRequest.STATUS_RETURNED, RentalRequest.STATUS_REJECTED)
            else -> listOf(RentalRequest.STATUS_PENDING)
        }
        val filtered = allRentals.filter { it.status in statusList }
            .sortedByDescending { it.createdAt }
        transactions.clear()
        transactions.addAll(filtered)
    }

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
                    }
                    allRentals.clear()
                    allRentals.addAll(result)
                    applyFilter()
                    isLoading = false
                }
        } else null
        listenerReg.value = reg
        onDispose { reg?.remove() }
    }

    LaunchedEffect(selectedTabIndex) {
        applyFilter()
    }

    fun cancelRental(rental: RentalRequest) {
        db.collection("rentals").document(rental.id)
            .update("status", RentalRequest.STATUS_REJECTED, "updatedAt", System.currentTimeMillis())
            .addOnSuccessListener {
                cancelTarget = null
                Toast.makeText(context, "Permintaan sewa dibatalkan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                cancelTarget = null
                Toast.makeText(context, "Gagal membatalkan", Toast.LENGTH_SHORT).show()
            }
    }

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

    if (cancelTarget != null) {
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Batalkan Sewa") },
            text = { Text("Yakin ingin membatalkan sewa ${cancelTarget!!.itemName}?") },
            confirmButton = {
                TextButton(onClick = { cancelRental(cancelTarget!!) }) {
                    Text("Ya, Batalkan", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("Tidak")
                }
            }
        )
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Transaksi Saya", onBackClick = onBack)

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
            } else if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        when (selectedTabIndex) {
                            0 -> "Belum ada transaksi menunggu"
                            1 -> "Tidak ada barang yang sedang disewa"
                            else -> "Belum ada riwayat transaksi"
                        },
                        color = TextLight
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions) { rental ->
                        GlowCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (rental.itemId.isNotEmpty()) {
                                        navController.navigate(Screen.ItemDetail.createRoute(rental.itemId))
                                    }
                                }
                        ) {
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
                                if (rental.status == RentalRequest.STATUS_PENDING) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    GlowButton(
                                        text = "Batalkan",
                                        onClick = { cancelTarget = rental },
                                        modifier = Modifier.fillMaxWidth(),
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
