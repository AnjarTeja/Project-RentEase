package com.example.rentease.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.ChatMessage
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    navController: NavHostController,
    onBack: () -> Unit = {},
    chatId: String
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isParticipant by remember { mutableStateOf(false) }
    var myName by remember { mutableStateOf("") }
    var nameLoaded by remember { mutableStateOf(false) }
    var accessError by remember { mutableStateOf<String?>(null) }

    val listenerReg = remember { mutableStateOf<ListenerRegistration?>(null) }

    // Get current user's name and check role/access
    LaunchedEffect(chatId) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                myName = userDoc.getString("name") ?: "Pengguna"
                nameLoaded = true
                val role = userDoc.getString("role") ?: ""
                val isStaff = role == "admin" || role == "petugas"

                db.collection("chats").document(chatId).get()
                    .addOnSuccessListener { chatDoc ->
                        if (!chatDoc.exists()) {
                            accessError = "Chat tidak ditemukan"
                            return@addOnSuccessListener
                        }
                        val renterId = chatDoc.getString("renterId") ?: ""
                        val ownerId = chatDoc.getString("ownerId") ?: ""
                        val hasAccess = isStaff || uid == renterId || uid == ownerId
                        if (!hasAccess) {
                            accessError = "Anda tidak memiliki akses ke chat ini"
                            return@addOnSuccessListener
                        }
                        isParticipant = true

                        val reg = db.collection("chats").document(chatId)
                            .collection("messages")
                            .orderBy("timestamp", Query.Direction.ASCENDING)
                            .addSnapshotListener { snapshots, e ->
                                if (e != null || snapshots == null) return@addSnapshotListener
                                val msgs = snapshots.mapNotNull { doc ->
                                    try {
                                        ChatMessage(
                                            id = doc.id,
                                            chatId = chatId,
                                            senderId = doc.getString("senderId") ?: "",
                                            senderName = doc.getString("senderName") ?: "",
                                            message = doc.getString("message") ?: "",
                                            timestamp = doc.getLong("timestamp") ?: 0L
                                        )
                                    } catch (e: Exception) { null }
                                }
                                messages.clear()
                                messages.addAll(msgs)
                            }
                        listenerReg.value = reg
                    }
                    .addOnFailureListener {
                        accessError = "Gagal memuat chat"
                    }
            }
            .addOnFailureListener {
                nameLoaded = true
                accessError = "Gagal memuat data user"
            }
    }

    // Auto-scroll to latest message when new message arrives
    var isNearBottom by remember { mutableStateOf(true) }
    LaunchedEffect(messages.size) {
        if (isNearBottom && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    // Track if user is near bottom
    LaunchedEffect(listState.firstVisibleItemIndex) {
        isNearBottom = listState.firstVisibleItemIndex >= messages.size - 3 || messages.isEmpty()
    }

    // Cleanup listener
    DisposableEffect(chatId) {
        onDispose {
            listenerReg.value?.remove()
        }
    }

    fun sendMessage() {
        val text = messageText.trim()
        if (text.isEmpty() || !isParticipant || !nameLoaded) return
        val uid = auth.currentUser?.uid ?: return

        val msg = hashMapOf(
            "senderId" to uid,
            "senderName" to myName,
            "message" to text,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("chats").document(chatId)
            .collection("messages")
            .add(msg)
            .addOnSuccessListener {
                db.collection("chats").document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to text,
                            "lastMessageTime" to System.currentTimeMillis()
                        )
                    )
                messageText = ""
            }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Box(modifier = Modifier.fillMaxSize().imePadding()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppToolbar(title = "Chat", onBackClick = onBack)

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (accessError != null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = accessError!!,
                                color = ErrorColor,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(messages) { msg ->
                                val isMe = msg.senderId == auth.currentUser?.uid
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                ) {
                                    GlassCard(
                                        modifier = Modifier.width(280.dp),
                                        radius = 12.dp
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = msg.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextDark
                                            )
                                            Text(
                                                text = if (msg.timestamp > 0) {
                                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                                                } else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextHint,
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isParticipant) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Ketik pesan...", color = TextHint) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                                cursorColor = Primary,
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { sendMessage() },
                            enabled = messageText.trim().isNotEmpty()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Kirim",
                                tint = if (messageText.trim().isNotEmpty()) Primary else TextHint
                            )
                        }
                    }
                }
            }
        }
    }
}
