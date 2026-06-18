package com.example.rentease.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.Item
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
    var items by remember { mutableStateOf(listOf<Item>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var itemToAct by remember { mutableStateOf<Item?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    fun loadPendingItems() {
        isLoading = true
        db.collection("items")
            .whereEqualTo("approvalStatus", Item.APPROVAL_PENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                items = documents.mapNotNull { doc ->
                    try { doc.toObject(Item::class.java).copy(id = doc.id) }
                    catch (_: Exception) { null }
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("FAILED_PRECONDITION") == true || e.message?.contains("index") == true) {
                    db.collection("items")
                        .whereEqualTo("approvalStatus", Item.APPROVAL_PENDING)
                        .get()
                        .addOnSuccessListener { documents ->
                            items = documents.mapNotNull { doc ->
                                try { doc.toObject(Item::class.java).copy(id = doc.id) }
                                catch (_: Exception) { null }
                            }.sortedByDescending { it.createdAt }
                            isLoading = false
                        }
                        .addOnFailureListener {
                            errorMessage = "Gagal memuat data pengajuan"
                            isLoading = false
                        }
                } else {
                    errorMessage = "Gagal memuat data pengajuan"
                    isLoading = false
                }
            }
    }

    LaunchedEffect(Unit) { loadPendingItems() }

    fun approveItem() {
        val item = itemToAct ?: return
        isProcessing = true
        db.collection("items").document(item.id)
            .update("approvalStatus", Item.APPROVAL_APPROVED)
            .addOnSuccessListener {
                isProcessing = false
                showApproveDialog = false
                itemToAct = null
                items = items.filter { it.id != item.id }
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
                items = items.filter { it.id != item.id }
            }
            .addOnFailureListener { isProcessing = false }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Verifikasi Barang User", onBackClick = onBack)

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
                    items.isEmpty() -> {
                        Text(
                            text = "Tidak ada pengajuan barang pending",
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
                            items(items, key = { it.id }) { item ->
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                        Text(text = item.name, style = MaterialTheme.typography.titleSmall, color = TextDark)
                                        Text(
                                            text = "Rp ${String.format("%,.0f", item.price)} | Stok: ${item.stock}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextLight
                                        )
                                        Text(
                                            text = dateFormat.format(Date(item.createdAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextHint
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
