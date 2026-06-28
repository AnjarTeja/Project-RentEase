package com.example.rentease.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.Chat
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val chatList = remember { mutableStateListOf<Chat>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf("") }
    var roleLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: run { isLoading = false; return@LaunchedEffect }
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userRole = doc.getString("role") ?: ""
                roleLoaded = true
            }
            .addOnFailureListener {
                userRole = ""
                roleLoaded = true
            }
    }

    DisposableEffect(roleLoaded) {
        if (!roleLoaded) return@DisposableEffect onDispose {}
        val uid = auth.currentUser?.uid ?: return@DisposableEffect onDispose {}
        val isStaff = userRole == "admin" || userRole == "petugas"
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

        fun addChatFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot) {
            val chat = Chat(
                id = doc.id,
                rentalId = doc.getString("rentalId") ?: "",
                itemId = doc.getString("itemId") ?: "",
                itemName = doc.getString("itemName") ?: "",
                renterId = doc.getString("renterId") ?: "",
                renterName = doc.getString("renterName") ?: "",
                ownerId = doc.getString("ownerId") ?: "",
                ownerName = doc.getString("ownerName") ?: "",
                lastMessage = doc.getString("lastMessage") ?: "",
                lastMessageTime = doc.getLong("lastMessageTime") ?: 0L,
                createdAt = doc.getLong("createdAt") ?: 0L
            )
            val existingIndex = chatList.indexOfFirst { it.id == chat.id }
            if (existingIndex >= 0) {
                chatList[existingIndex] = chat
            } else {
                chatList.add(chat)
            }
        }

        if (isStaff) {
            val reg = db.collection("chats")
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        db.collection("chats")
                            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener { snap ->
                                chatList.clear()
                                for (doc in snap.documents) {
                                    addChatFromDoc(doc)
                                }
                                chatList.sortByDescending { it.lastMessageTime }
                                isLoading = false
                            }
                            .addOnFailureListener {
                                errorMessage = "Gagal memuat chat"
                                isLoading = false
                            }
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        chatList.clear()
                        for (doc in snapshots.documents) {
                            addChatFromDoc(doc)
                        }
                        chatList.sortByDescending { it.lastMessageTime }
                        isLoading = false
                    }
                }
            listeners.add(reg)
        } else {
            fun listenField(field: String) {
                val reg = db.collection("chats")
                    .whereEqualTo(field, uid)
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            db.collection("chats")
                                .whereEqualTo(field, uid)
                                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                                .get()
                                .addOnSuccessListener { snap ->
                                    for (doc in snap.documents) {
                                        addChatFromDoc(doc)
                                    }
                                    chatList.sortByDescending { it.lastMessageTime }
                                    isLoading = false
                                }
                                .addOnFailureListener {
                                    if (!isLoading) return@addOnFailureListener
                                    errorMessage = "Gagal memuat chat"
                                    isLoading = false
                                }
                            return@addSnapshotListener
                        }
                        if (snapshots != null) {
                            for (doc in snapshots.documents) {
                                addChatFromDoc(doc)
                            }
                            chatList.sortByDescending { it.lastMessageTime }
                            isLoading = false
                        }
                    }
                listeners.add(reg)
            }
            listenField("renterId")
            listenField("ownerId")
        }

        onDispose {
            for (reg in listeners) reg.remove()
        }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Chat", onBackClick = onBack)

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                    }
                    errorMessage != null -> {
                        Text(
                            errorMessage!!,
                            color = ErrorColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    chatList.isEmpty() -> {
                        Text("Belum ada chat", color = TextLight)
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatList, key = { it.id }) { chat ->
                                val uid = auth.currentUser?.uid ?: ""
                                val isStaff = userRole == "admin" || userRole == "petugas"
                                val contactName = when {
                                    isStaff -> "${chat.renterName} & ${chat.ownerName}"
                                    chat.renterId == uid -> chat.ownerName
                                    else -> chat.renterName
                                }

                                GlowCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate(Screen.Chat.createRoute(chat.id))
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(TechCardBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Chat,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = contactName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = TextDark,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (chat.lastMessageTime > 0) {
                                                    Text(
                                                        text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(chat.lastMessageTime)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextHint
                                                    )
                                                }
                                            }
                                            Text(
                                                text = chat.itemName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Primary
                                            )
                                            if (chat.lastMessage.isNotEmpty()) {
                                                Text(
                                                    text = chat.lastMessage,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextLight,
                                                    maxLines = 1
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
}
