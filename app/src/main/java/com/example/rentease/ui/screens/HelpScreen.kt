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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.FaqItem
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.SupportTicket
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HelpScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val authManager = remember { FirebaseAuthManager() }
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var loadingTickets by remember { mutableStateOf(true) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var expandedFaqIndex by remember { mutableStateOf(-1) }

    val faqItems = remember {
        listOf(
            FaqItem("Bagaimana cara menyewa barang?", "Pilih barang di katalog, tentukan durasi sewa, lalu klik 'Ajukan Sewa'. Tunggu petugas menyetujui laporan Anda."),
            FaqItem("Metode pembayaran apa saja yang tersedia?", "Saat ini kami mendukung pembayaran tunai saat pengambilan barang (COD) atau transfer bank langsung ke pemilik barang."),
            FaqItem("Apakah saya bisa menyewakan barang milik saya?", "Tentu! Gunakan menu 'Tambah Barang' di dashboard. Barang Anda akan diverifikasi oleh petugas sebelum tampil di katalog."),
            FaqItem("Berapa lama proses verifikasi barang?", "Petugas kami akan memproses verifikasi dalam waktu maksimal 1x24 jam kerja."),
            FaqItem("Apa yang terjadi jika saya terlambat mengembalikan?", "Anda akan dikenakan denda keterlambatan sebesar 10% dari harga sewa harian untuk setiap hari keterlambatan."),
            FaqItem("Bagaimana jika barang yang saya sewa rusak?", "Laporkan segera melalui formulir bantuan di bawah ini. Biaya perbaikan akan didiskusikan antara penyewa dan pemilik barang."),
            FaqItem("Apakah data pribadi saya aman?", "RentEase sangat menjaga privasi Anda. Data hanya digunakan untuk keperluan verifikasi dan keamanan transaksi sewa."),
            FaqItem("Bisakah saya membatalkan pengajuan sewa?", "Bisa, selama status laporan masih 'PENDING'. Jika sudah disetujui, harap hubungi admin via WhatsApp."),
            FaqItem("Siapa yang menanggung biaya pengiriman?", "Biaya pengiriman atau pengambilan barang ditanggung sepenuhnya oleh penyewa, kecuali ada kesepakatan lain."),
            FaqItem("Mengapa barang saya ditolak petugas?", "Alasan penolakan biasanya karena foto tidak jelas, deskripsi kurang lengkap, atau kategori tidak sesuai.")
        )
    }

    val ticketsListener = remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: run { loadingTickets = false; return@LaunchedEffect }
        val reg = db.collection("support_tickets")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    db.collection("support_tickets")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { docs ->
                            tickets = docs.documents.mapNotNull {
                                it.toObject(SupportTicket::class.java)?.copy(id = it.id)
                            }.sortedByDescending { it.createdAt }
                            loadingTickets = false
                        }
                        .addOnFailureListener { loadingTickets = false }
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    tickets = snapshots.documents.mapNotNull {
                        it.toObject(SupportTicket::class.java)?.copy(id = it.id)
                    }
                    loadingTickets = false
                }
            }
        ticketsListener.value = reg
    }

    DisposableEffect(Unit) {
        onDispose {
            ticketsListener.value?.remove()
        }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppToolbar(title = "Bantuan", onBackClick = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Pusat Bantuan", style = MaterialTheme.typography.titleLarge, color = TextDark)
                Text("Temukan jawaban atau hubungi kami", style = MaterialTheme.typography.bodyMedium, color = TextLight)

                Text("FAQ", style = MaterialTheme.typography.titleMedium, color = TextDark)
                faqItems.forEachIndexed { index, faq ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), radius = 12.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.QuestionAnswer,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = faq.question,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextDark,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (expandedFaqIndex == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextHint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (expandedFaqIndex == index) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = faq.answer,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextLight
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Hubungi Kami", style = MaterialTheme.typography.titleMedium, color = TextDark)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlowButton(
                        text = "WhatsApp",
                        onClick = {
                            val url = "https://api.whatsapp.com/send?phone=6282316627926&text=${Uri.encode("Halo Admin RentEase, saya butuh bantuan mengenai...")}"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = SuccessColor,
                        icon = Icons.AutoMirrored.Filled.Chat
                    )
                    GlowButton(
                        text = "Email",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("jarztzy5@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Bantuan Aplikasi RentEase")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = Primary,
                        icon = Icons.Default.Email
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Kirim Tiket Bantuan", style = MaterialTheme.typography.titleMedium, color = TextDark)

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subjek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Detail Keluhan") },
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
                        if (subject.isEmpty() || message.isEmpty()) return@GlowButton
                        val uid = auth.currentUser?.uid ?: return@GlowButton
                        submitting = true
                        authManager.getUserData(
                            onSuccess = { userData ->
                                val userName = userData["name"] as? String ?: "User"
                                val userEmail = userData["email"] as? String ?: auth.currentUser?.email ?: ""
                                val ticket = SupportTicket(
                                    userId = uid,
                                    userName = userName,
                                    userEmail = userEmail,
                                    subject = subject,
                                    message = message,
                                    status = SupportTicket.STATUS_OPEN,
                                    createdAt = System.currentTimeMillis()
                                )
                                db.collection("support_tickets").add(ticket)
                                    .addOnSuccessListener {
                                        subject = ""
                                        message = ""
                                        submitting = false
                                    }
                                    .addOnFailureListener { submitting = false }
                            },
                            onFailure = { submitting = false }
                        )
                    },
                    enabled = !submitting
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Tiket Saya", style = MaterialTheme.typography.titleMedium, color = TextDark)

                if (loadingTickets) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Memuat...", color = TextHint)
                    }
                } else if (tickets.isEmpty()) {
                    Text("Belum ada tiket", color = TextHint)
                } else {
                    tickets.forEach { ticket ->
                        GlowCard(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navController.navigate(Screen.TicketDetail.createRoute(ticket.id))
                            },
                            radius = 12.dp
                        ) {
                            Column(modifier = Modifier.padding(0.dp)) {
                                Text(ticket.subject, style = MaterialTheme.typography.bodyMedium, color = TextDark, fontWeight = FontWeight.Medium)
                                Text(ticket.message, style = MaterialTheme.typography.bodySmall, color = TextLight, maxLines = 2)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    Text(sdf.format(Date(ticket.createdAt)), style = MaterialTheme.typography.labelSmall, color = TextHint)
                                    val statusColor = when (ticket.status) {
                                        SupportTicket.STATUS_RESOLVED -> SuccessColor
                                        SupportTicket.STATUS_IN_PROGRESS -> WarningColor
                                        else -> Primary
                                    }
                                    Text(ticket.status.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = statusColor)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
