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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.rentease.Item
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
fun VerifyUserItemsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var allItems by remember { mutableStateOf(listOf<Item>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var itemToAct by remember { mutableStateOf<Item?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var accessError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val authManager = FirebaseAuthManager()
        authManager.getUserRole(
            onSuccess = { role ->
                if (role != "admin" && role != "petugas") {
                    accessError = "Anda tidak memiliki akses ke halaman ini"
                }
            },
            onFailure = { accessError = "Gagal memverifikasi akses" }
        )
    }

    val pendingItems = allItems.filter { it.approvalStatus == Item.APPROVAL_PENDING }

    DisposableEffect(Unit) {
        val listener = db.collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("index") == true) {
                        db.collection("items").get()
                            .addOnSuccessListener { documents ->
                                allItems = documents.mapNotNull { doc ->
                                    try { doc.toObject(Item::class.java).copy(id = doc.id) }
                                    catch (_: Exception) { null }
                                }.sortedByDescending { it.createdAt }
                                isLoading = false
                                errorMessage = null
                            }
                            .addOnFailureListener {
                                errorMessage = "Gagal memuat data pengajuan"
                                isLoading = false
                            }
                    } else {
                        errorMessage = "Gagal memuat data: ${error.localizedMessage}"
                        isLoading = false
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    allItems = snapshot.documents.mapNotNull { doc ->
                        try { doc.toObject(Item::class.java)?.copy(id = doc.id) }
                        catch (_: Exception) { null }
                    }
                    isLoading = false
                    errorMessage = null
                }
            }
        onDispose { listener.remove() }
    }

    fun approveItem() {
        val item = itemToAct ?: return
        isProcessing = true
        db.collection("items").document(item.id)
            .update("approvalStatus", Item.APPROVAL_APPROVED)
            .addOnSuccessListener {
                isProcessing = false
                showApproveDialog = false
                itemToAct = null
            }
            .addOnFailureListener { isProcessing = false }
    }

    fun rejectItem() {
        val item = itemToAct ?: return
        isProcessing = true
        db.collection("items").document(item.id)
            .delete()
            .addOnSuccessListener {
                isProcessing = false
                showRejectDialog = false
                itemToAct = null
            }
            .addOnFailureListener { isProcessing = false }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        if (accessError != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppToolbar(title = "Akses Ditolak", onBackClick = onBack)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = accessError!!,
                        color = ErrorColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AppToolbar(title = "Verifikasi Barang User", onBackClick = onBack)

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
                    pendingItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada pengajuan barang pending",
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
                            items(pendingItems, key = { it.id }) { item ->
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextDark,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            PendingBadge()
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
                                                text = item.ownerId.take(8) + "..." ,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextLight
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Category,
                                                contentDescription = null,
                                                tint = TextHint,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.category,
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
                                                text = dateFormat.format(Date(item.createdAt)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextHint
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Sell,
                                                contentDescription = null,
                                                tint = SuccessColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Rp ${String.format("%,.0f", item.price)} | Stok: ${item.stock}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SuccessColor
                                            )
                                        }

                                        if (item.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextLight,
                                                maxLines = 2
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            GlowButton(
                                                text = "Setujui",
                                                onClick = {
                                                    itemToAct = item
                                                    showApproveDialog = true
                                                },
                                                backgroundColor = SuccessColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                            GlowButton(
                                                text = "Tolak",
                                                onClick = {
                                                    itemToAct = item
                                                    showRejectDialog = true
                                                },
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

    if (showApproveDialog && itemToAct != null) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) { showApproveDialog = false; itemToAct = null } },
            title = { Text("Setujui Barang", color = TextDark) },
            text = {
                Text(
                    "Barang '${itemToAct!!.name}' akan ditambahkan ke katalog publik dan bisa dilihat oleh semua penyewa. Lanjutkan?",
                    color = TextLight
                )
            },
            confirmButton = {
                TextButton(onClick = { approveItem() }, enabled = !isProcessing) {
                    Text(if (isProcessing) "Memproses..." else "Setujui", color = SuccessColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false; itemToAct = null }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = TechCardBg
        )
    }

    if (showRejectDialog && itemToAct != null) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) { showRejectDialog = false; itemToAct = null } },
            title = { Text("Tolak Barang", color = TextDark) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menolak pengajuan barang '${itemToAct!!.name}'? Data barang akan dihapus dari sistem pengajuan.",
                    color = TextLight
                )
            },
            confirmButton = {
                TextButton(onClick = { rejectItem() }, enabled = !isProcessing) {
                    Text(if (isProcessing) "Memproses..." else "Tolak", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false; itemToAct = null }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = TechCardBg
        )
    }
}

@Composable
private fun PendingBadge() {
    Box(
        modifier = Modifier
            .background(WarningColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "Pending",
            color = WarningColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
