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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManageReturnsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var rentals by remember { mutableStateOf(listOf<RentalRequest>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var rentalToReturn by remember { mutableStateOf<RentalRequest?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var overdueCount by remember { mutableStateOf(0) }
    var totalFine by remember { mutableStateOf(0.0) }

    fun processRentals(raw: List<RentalRequest>): List<RentalRequest> {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val todayDate = sdf.parse(sdf.format(Date()))
        var overdue = 0
        var fine = 0.0
        val processed = raw.map { rental ->
            val r = rental.copy()
            try {
                val endDate = sdf.parse(r.endDate)
                if (endDate != null && todayDate != null && endDate.before(todayDate)) {
                    val diffMs = todayDate.time - endDate.time
                    val overdueDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                    val f = overdueDays * r.pricePerDay * 0.1
                    r.overdueDays = overdueDays
                    r.fineAmount = f
                    r.isOverdue = true
                    overdue++
                    fine += f
                }
            } catch (_: Exception) { }
            r
        }
        overdueCount = overdue
        totalFine = fine
        return processed
    }

    DisposableEffect(Unit) {
        val listener = db.collection("rentals")
            .whereEqualTo("status", "approved")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("FAILED_PRECONDITION") == true || error.message?.contains("index") == true) {
                        db.collection("rentals")
                            .whereEqualTo("status", "approved")
                            .get()
                            .addOnSuccessListener { documents ->
                                val raw = documents.mapNotNull { doc ->
                                    try { doc.toObject(RentalRequest::class.java).copy(id = doc.id) }
                                    catch (_: Exception) { null }
                                }.sortedByDescending { it.createdAt }
                                rentals = processRentals(raw)
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
                    val raw = snapshot.documents.mapNotNull { doc ->
                        try { doc.toObject(RentalRequest::class.java)?.copy(id = doc.id) }
                        catch (_: Exception) { null }
                    }
                    rentals = processRentals(raw)
                    isLoading = false
                    errorMessage = null
                }
            }
        onDispose { listener.remove() }
    }

    fun processReturn() {
        val rental = rentalToReturn ?: return
        isProcessing = true

        val batch = db.batch()
        val rentalRef = db.collection("rentals").document(rental.id)
        val updates = hashMapOf<String, Any>(
            "status" to "returned",
            "actualReturnDate" to dateFormat.format(Date()),
            "isOverdue" to rental.isOverdue
        )
        if (rental.isOverdue) {
            updates["overdueDays"] = rental.overdueDays
            updates["fineAmount"] = rental.fineAmount
        }
        batch.update(rentalRef, updates)

        if (rental.itemId.isNotEmpty()) {
            val itemRef = db.collection("items").document(rental.itemId)
            batch.update(itemRef, "status", Item.STATUS_AVAILABLE)
            batch.update(itemRef, "stock", com.google.firebase.firestore.FieldValue.increment(1))
        }

        batch.commit()
            .addOnSuccessListener {
                isProcessing = false
                showReturnDialog = false
                rentalToReturn = null
                NotificationHelper.showRentalStatusNotification(context, rental.itemName, "returned")
            }
            .addOnFailureListener {
                isProcessing = false
            }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Pengembalian Barang", onBackClick = onBack)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "$overdueCount",
                            style = MaterialTheme.typography.headlineSmall,
                            color = WarningColor
                        )
                        Text("Rental Terlambat", style = MaterialTheme.typography.labelSmall, color = TextLight)
                    }
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Rp ${String.format("%,.0f", totalFine)}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ErrorColor
                        )
                        Text("Total Denda", style = MaterialTheme.typography.labelSmall, color = TextLight)
                    }
                }
            }

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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada data pengembalian",
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
                            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                            if (rental.isOverdue) {
                                                OverdueBadge(rental.overdueDays)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

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

                                        Spacer(modifier = Modifier.height(2.dp))

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

                                        if (rental.isOverdue && rental.fineAmount > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = ErrorColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Denda: Rp ${String.format("%,.0f", rental.fineAmount)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ErrorColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        GlowButton(
                                            text = "Tandai Kembali",
                                            onClick = {
                                                rentalToReturn = rental
                                                showReturnDialog = true
                                            },
                                            backgroundColor = SuccessColor
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

    if (showReturnDialog && rentalToReturn != null) {
        val rental = rentalToReturn!!
        val message = buildString {
            append("Apakah barang '${rental.itemName}' sudah dikembalikan dengan kondisi baik oleh ${rental.renterName}?")
            if (rental.isOverdue && rental.fineAmount > 0) {
                append("\n\nRental ini terlambat ${rental.overdueDays} hari!\nDenda: Rp ${String.format("%,.0f", rental.fineAmount)}")
            }
        }
        AlertDialog(
            onDismissRequest = { if (!isProcessing) { showReturnDialog = false; rentalToReturn = null } },
            title = { Text("Konfirmasi Pengembalian", color = TextDark) },
            text = { Text(message, color = TextLight) },
            confirmButton = {
                TextButton(onClick = { processReturn() }, enabled = !isProcessing) {
                    Text(if (isProcessing) "Memproses..." else "Sudah Kembali", color = SuccessColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false; rentalToReturn = null }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = TechCardBg
        )
    }
}

@Composable
private fun OverdueBadge(days: Int) {
    Box(
        modifier = Modifier
            .background(ErrorColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Terlambat $days hari",
                color = ErrorColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
