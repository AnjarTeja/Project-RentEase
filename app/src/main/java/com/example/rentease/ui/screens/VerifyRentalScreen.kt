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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.example.rentease.ui.components.InfoRow
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
fun VerifyRentalScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    var rentals by remember { mutableStateOf(listOf<RentalRequest>()) }
    var currentTab by remember { mutableStateOf(RentalRequest.STATUS_PENDING) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf("") }
    var rentalToAct by remember { mutableStateOf<RentalRequest?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    fun loadRentals() {
        isLoading = true
        errorMessage = null
        db.collection("rentals")
            .whereEqualTo("status", currentTab)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                rentals = documents.mapNotNull { doc ->
                    try { doc.toObject(RentalRequest::class.java).copy(id = doc.id) }
                    catch (_: Exception) { null }
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("index") == true) {
                    db.collection("rentals").get()
                        .addOnSuccessListener { documents ->
                            rentals = documents
                                .filter { it.getString("status") == currentTab }
                                .sortedByDescending { it.getLong("createdAt") ?: 0L }
                                .mapNotNull { doc ->
                                    try { doc.toObject(RentalRequest::class.java).copy(id = doc.id) }
                                    catch (_: Exception) { null }
                                }
                            isLoading = false
                        }
                        .addOnFailureListener {
                            errorMessage = "Gagal memuat data"
                            isLoading = false
                        }
                } else {
                    errorMessage = "Gagal memuat data: ${e.localizedMessage}"
                    isLoading = false
                }
            }
    }

    LaunchedEffect(currentTab) { loadRentals() }

    fun showConfirm(rental: RentalRequest, newStatus: String) {
        rentalToAct = rental
        confirmAction = newStatus
        showConfirmDialog = true
    }

    fun executeAction() {
        val rental = rentalToAct ?: return
        val newStatus = confirmAction
        isProcessing = true

        if (newStatus == RentalRequest.STATUS_APPROVED && rental.itemId.isNotEmpty()) {
            db.collection("items").document(rental.itemId).get()
                .addOnSuccessListener { doc ->
                    val currentStock = doc.getLong("stock")?.toInt() ?: 1
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
                            loadRentals()
                        }
                        .addOnFailureListener { isProcessing = false; showConfirmDialog = false }
                }
                .addOnFailureListener { isProcessing = false; showConfirmDialog = false }
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
                    loadRentals()
                }
                .addOnFailureListener { isProcessing = false; showConfirmDialog = false }
        }
    }

    val tabs = listOf("Pending", "Disetujui", "Ditolak")
    val tabValues = listOf(RentalRequest.STATUS_PENDING, RentalRequest.STATUS_APPROVED, RentalRequest.STATUS_REJECTED)

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Verifikasi Rental", onBackClick = onBack)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    val selected = currentTab == tabValues[index]
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) TechCardBg else TextLight,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Primary else TechCardBg)
                            .clickable { currentTab = tabValues[index] }
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

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
                    rentals.isEmpty() -> {
                        val emptyMsg = when (currentTab) {
                            RentalRequest.STATUS_PENDING -> "Tidak ada pengajuan pending"
                            RentalRequest.STATUS_APPROVED -> "Belum ada pengajuan yang disetujui"
                            RentalRequest.STATUS_REJECTED -> "Belum ada pengajuan yang ditolak"
                            else -> "Data kosong"
                        }
                        Text(
                            text = emptyMsg,
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
                            items(rentals, key = { it.id }) { rental ->
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                        Text(text = rental.itemName, style = MaterialTheme.typography.titleSmall, color = TextDark)
                                        Text(text = "Penyewa: ${rental.renterName}", style = MaterialTheme.typography.bodySmall, color = TextLight)
                                        Text(text = "${rental.startDate} - ${rental.endDate}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                                        if (rental.note.isNotBlank()) {
                                            Text(text = "Catatan: ${rental.note}", style = MaterialTheme.typography.labelSmall, color = TextLight)
                                        }

                                        if (currentTab == RentalRequest.STATUS_PENDING) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
