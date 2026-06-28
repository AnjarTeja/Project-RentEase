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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ItemReport
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ReportItemScreen(
    itemId: String = "",
    itemName: String = "Barang",
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val reasons = remember {
        listOf(
            "Tidak Pantas",
            "Spam",
            "Barang Palsu",
            "Kategori Salah",
            "Harga Tidak Wajar",
            "Barang Rusak",
            "Tidak Sesuai Deskripsi",
            "Penipuan",
            "Lainnya"
        )
    }
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Laporkan Barang", onBackClick = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth(), radius = 12.dp) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Melaporkan: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLight
                        )
                        Text(
                            itemName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text("Alasan Pelaporan", style = MaterialTheme.typography.titleMedium, color = TextDark)

                Column(modifier = Modifier.selectableGroup()) {
                    reasons.forEach { reason ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .selectable(
                                    selected = selectedReason == reason,
                                    onClick = { selectedReason = reason },
                                    role = Role.RadioButton
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedReason == reason) Primary.copy(alpha = 0.1f) else TechCardBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedReason == reason,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Primary,
                                        unselectedColor = TextHint
                                    )
                                )
                                Text(
                                    reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextDark,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Text("Deskripsi (opsional)", style = MaterialTheme.typography.titleSmall, color = TextDark)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Tambahkan detail pelaporan...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = TextHint,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextHint,
                        cursorColor = Primary,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark
                    )
                )

                GlowButton(
                    text = if (submitting) "Mengirim..." else "Kirim Laporan",
                    onClick = {
                        if (selectedReason.isEmpty()) return@GlowButton
                        val uid = auth.currentUser?.uid ?: return@GlowButton
                        submitting = true
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val reporterName = doc.getString("name") ?: "Pengguna"
                                val report = hashMapOf(
                                    "itemId" to itemId,
                                    "itemName" to itemName,
                                    "reporterId" to uid,
                                    "reporterName" to reporterName,
                                    "reason" to selectedReason,
                                    "description" to description,
                                    "status" to ItemReport.STATUS_PENDING,
                                    "createdAt" to System.currentTimeMillis()
                                )
                                db.collection("item_reports").add(report)
                                    .addOnSuccessListener {
                                        submitting = false
                                        onBack()
                                    }
                                    .addOnFailureListener { submitting = false }
                            }
                            .addOnFailureListener { submitting = false }
                    },
                    enabled = !submitting && selectedReason.isNotEmpty()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
