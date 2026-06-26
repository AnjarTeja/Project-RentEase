package com.example.rentease.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.Item
import com.example.rentease.NotificationHelper
import com.example.rentease.RentalRequest
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


@Composable
fun VerifyRentalScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    var allRentals by remember { mutableStateOf(listOf<RentalRequest>()) }
    var currentTab by remember { mutableStateOf(RentalRequest.STATUS_PENDING) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf("") }
    var rentalToAct by remember { mutableStateOf<RentalRequest?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val rentals by remember(allRentals, currentTab) {
        derivedStateOf { allRentals.filter { it.status == currentTab } }
    }

    DisposableEffect(Unit) {
        val listener = db.collection("rentals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("index") == true) {
                        db.collection("rentals").get()
                            .addOnSuccessListener { docs ->
                                allRentals = docs.mapNotNull { doc ->
                                    try { doc.toObject(RentalRequest::class.java)?.copy(id = doc.id) }
                                    catch (_: Exception) { null }
                                }
                                isLoading = false
                                errorMessage = null
                            }
                            .addOnFailureListener {
                                errorMessage = "Gagal memuat data"
                                isLoading = false
                            }
                    } else {
                        errorMessage = "Gagal memuat data: ${error.localizedMessage}"
                        isLoading = false
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    allRentals = snapshot.documents.mapNotNull { doc ->
                        try { doc.toObject(RentalRequest::class.java)?.copy(id = doc.id) }
                        catch (_: Exception) { null }
                    }
                    isLoading = false
                    errorMessage = null
                }
            }
        onDispose { listener.remove() }
    }

    fun showConfirm(rental: RentalRequest, newStatus: String) {
        rentalToAct = rental
        confirmAction = newStatus
        showConfirmDialog = true
    }

    fun executeAction() {
        val rental = rentalToAct ?: return
        val newStatus = confirmAction
        isProcessing = true

        if (newStatus == RentalRequest.STATUS_APPROVED) {
            if (rental.itemId.isEmpty()) {
                isProcessing = false
                showConfirmDialog = false
                rentalToAct = null
                Toast.makeText(context, "ID barang tidak valid", Toast.LENGTH_SHORT).show()
                return
            }
            db.collection("items").document(rental.itemId).get()
                .addOnSuccessListener { doc ->
                    if (!doc.exists()) {
                        isProcessing = false
                        showConfirmDialog = false
                        rentalToAct = null
                        Toast.makeText(context, "Barang tidak ditemukan", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    val currentStock = doc.getLong("stock")?.toInt() ?: 0
                    if (currentStock <= 0) {
                        isProcessing = false
                        showConfirmDialog = false
                        rentalToAct = null
                        Toast.makeText(context, "Stok barang habis, tidak dapat menyetujui", Toast.LENGTH_SHORT).show()
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

                    batch.commit()
                        .addOnSuccessListener {
                            isProcessing = false
                            showConfirmDialog = false
                            rentalToAct = null
                            NotificationHelper.showRentalStatusNotification(context, rental.itemName, newStatus)
                        }
                        .addOnFailureListener {
                            isProcessing = false
                            showConfirmDialog = false
                            Toast.makeText(context, "Gagal memperbarui status", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    isProcessing = false
                    showConfirmDialog = false
                    Toast.makeText(context, "Gagal memeriksa stok barang", Toast.LENGTH_SHORT).show()
                }
        } else {
            val batch = db.batch()
            val rentalRef = db.collection("rentals").document(rental.id)
            batch.update(rentalRef, "status", newStatus)
            batch.update(rentalRef, "updatedAt", System.currentTimeMillis())
            batch.commit()
                .addOnSuccessListener {
                    isProcessing = false
                    showConfirmDialog = false
                    rentalToAct = null
                    NotificationHelper.showRentalStatusNotification(context, rental.itemName, newStatus)
                }
                .addOnFailureListener {
                    isProcessing = false
                    showConfirmDialog = false
                    Toast.makeText(context, "Gagal memperbarui status", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val tabs = listOf("Pending", "Disetujui", "Ditolak")
    val tabValues = listOf(RentalRequest.STATUS_PENDING, RentalRequest.STATUS_APPROVED, RentalRequest.STATUS_REJECTED)

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(
                title = "Verifikasi Rental",
                onBackClick = onBack
            )

            TabRow(
                selectedTabIndex = tabValues.indexOf(currentTab).coerceAtLeast(0),
                containerColor = TechCardBg,
                contentColor = Primary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabValues.indexOf(currentTab).coerceAtLeast(0)]),
                        height = 3.dp,
                        color = Primary
                    )
                },
                divider = { Spacer(modifier = Modifier.height(0.dp)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = currentTab == tabValues[index],
                        onClick = { currentTab = tabValues[index] },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentTab == tabValues[index]) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTab == tabValues[index]) Primary else TextLight
                            )
                        },
                        selectedContentColor = Primary,
                        unselectedContentColor = TextLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
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
                    rentals.isEmpty() -> {
                        val emptyMsg = when (currentTab) {
                            RentalRequest.STATUS_PENDING -> "Tidak ada pengajuan pending"
                            RentalRequest.STATUS_APPROVED -> "Belum ada pengajuan yang disetujui"
                            RentalRequest.STATUS_REJECTED -> "Belum ada pengajuan yang ditolak"
                            else -> "Data kosong"
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
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(rentals, key = { it.id }) { rental ->
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = rental.itemName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextDark,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            StatusBadge(rental.status)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = TextHint,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = rental.renterName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextLight
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = TextHint,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${rental.startDate} - ${rental.endDate}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextHint
                                            )
                                        }

                                        if (rental.duration > 0) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    tint = TextHint,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${rental.duration} hari",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextHint
                                                )
                                            }
                                        }

                                        if (rental.pricePerDay > 0) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.AttachMoney,
                                                    contentDescription = null,
                                                    tint = SuccessColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Rp ${String.format("%,.0f", rental.pricePerDay)}/hari",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SuccessColor
                                                )
                                            }
                                        }

                                        if (rental.note.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Catatan: ${rental.note}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextLight,
                                                maxLines = 2
                                            )
                                        }

                                        if (currentTab == RentalRequest.STATUS_PENDING) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                GlowButton(
                                                    text = "Setujui",
                                                    onClick = { showConfirm(rental, RentalRequest.STATUS_APPROVED) },
                                                    backgroundColor = SuccessColor,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                GlowButton(
                                                    text = "Tolak",
                                                    onClick = { showConfirm(rental, RentalRequest.STATUS_REJECTED) },
                                                    backgroundColor = ErrorColor,
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

    if (showConfirmDialog && rentalToAct != null) {
        val rental = rentalToAct!!
        val isApprove = confirmAction == RentalRequest.STATUS_APPROVED
        AlertDialog(
            onDismissRequest = { if (!isProcessing) { showConfirmDialog = false; rentalToAct = null } },
            title = { Text(if (isApprove) "Setujui Penyewaan" else "Tolak Penyewaan", color = TextDark) },
            text = {
                Text(
                    "Apakah Anda yakin ingin ${if (isApprove) "menyetujui" else "menolak"} penyewaan ${rental.itemName} oleh ${rental.renterName}?",
                    color = TextLight
                )
            },
            confirmButton = {
                TextButton(onClick = { executeAction() }, enabled = !isProcessing) {
                    Text(
                        if (isProcessing) "Memproses..." else if (isApprove) "Setujui" else "Tolak",
                        color = if (isApprove) SuccessColor else ErrorColor
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false; rentalToAct = null }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = TechCardBg
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, bgColor, textColor) = when (status) {
        RentalRequest.STATUS_PENDING -> Triple("Pending", WarningColor.copy(alpha = 0.15f), WarningColor)
        RentalRequest.STATUS_APPROVED -> Triple("Disetujui", SuccessColor.copy(alpha = 0.15f), SuccessColor)
        RentalRequest.STATUS_REJECTED -> Triple("Ditolak", ErrorColor.copy(alpha = 0.15f), ErrorColor)
        else -> Triple(status, TechCardBg, TextLight)
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
