package com.example.rentease

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var chatId: String
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: MaterialButton

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatMessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chatId = intent.getStringExtra("CHAT_ID") ?: ""
        val itemName = intent.getStringExtra("ITEM_NAME") ?: "Barang"
        val contactName = intent.getStringExtra("CONTACT_NAME") ?: "Pengguna"

        if (chatId.isEmpty()) {
            Toast.makeText(this, "Chat tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tv_chat_title).text = contactName
        findViewById<TextView>(R.id.tv_chat_subtitle).text = itemName

        rvMessages = findViewById(R.id.rv_messages)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)

        adapter = ChatMessageAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        loadMessages()

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadMessages() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    messages.clear()
                    for (doc in snapshots) {
                        val msg = ChatMessage(
                            id = doc.id,
                            chatId = chatId,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                        messages.add(msg)
                    }
                    adapter.updateData(messages)
                    if (messages.isNotEmpty()) {
                        rvMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val uid = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Pengguna"

        // Get user name from Firestore
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: userName

                val msg = hashMapOf(
                    "senderId" to uid,
                    "senderName" to name,
                    "message" to text,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("chats").document(chatId)
                    .collection("messages")
                    .add(msg)
                    .addOnSuccessListener {
                        // Update last message in chat
                        db.collection("chats").document(chatId)
                            .update(
                                mapOf(
                                    "lastMessage" to text,
                                    "lastMessageTime" to System.currentTimeMillis()
                                )
                            )
                            .addOnFailureListener {
                                // Silently handled — message was already sent
                            }
                        etMessage.setText("")
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengirim pesan", Toast.LENGTH_SHORT).show()
                    }
            }
    }
}
