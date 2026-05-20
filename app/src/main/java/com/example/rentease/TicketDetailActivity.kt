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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var ticketId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ticket_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ticket_detail_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ticketId = intent.getStringExtra("TICKET_ID") ?: ""
        if (ticketId.isEmpty()) {
            finish()
            return
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        loadTicketDetails()
        
        findViewById<MaterialButton>(R.id.btn_send_reply).setOnClickListener {
            submitReply()
        }
    }

    private fun loadTicketDetails() {
        db.collection("support_tickets").document(ticketId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val ticket = doc.toObject(SupportTicket::class.java)
                    ticket?.let {
                        findViewById<TextView>(R.id.tv_detail_user_name).text = it.userName
                        findViewById<TextView>(R.id.tv_detail_user_email).text = it.userEmail
                        findViewById<TextView>(R.id.tv_detail_subject).text = it.subject
                        findViewById<TextView>(R.id.tv_detail_message).text = it.message
                        
                        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                        findViewById<TextView>(R.id.tv_detail_date).text = "Dikirim pada ${sdf.format(Date(it.createdAt))}"
                        
                        if (it.status == SupportTicket.STATUS_RESOLVED) {
                            findViewById<EditText>(R.id.et_reply).setText(it.replyMessage)
                            findViewById<EditText>(R.id.et_reply).isEnabled = false
                            findViewById<MaterialButton>(R.id.btn_send_reply).isEnabled = false
                            findViewById<MaterialButton>(R.id.btn_send_reply).text = "Laporan Selesai"
                        }
                    }
                }
            }
    }

    private fun submitReply() {
        val reply = findViewById<EditText>(R.id.et_reply).text.toString().trim()
        if (reply.isEmpty()) {
            Toast.makeText(this, "Harap isi tanggapan", Toast.LENGTH_SHORT).show()
            return
        }

        val petugasId = auth.currentUser?.uid ?: return
        
        val updates = mapOf(
            "replyMessage" to reply,
            "repliedAt" to System.currentTimeMillis(),
            "repliedBy" to petugasId,
            "status" to SupportTicket.STATUS_RESOLVED
        )

        db.collection("support_tickets").document(ticketId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Tanggapan berhasil dikirim!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengirim tanggapan", Toast.LENGTH_SHORT).show()
            }
    }
}
