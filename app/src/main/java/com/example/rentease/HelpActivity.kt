package com.example.rentease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import com.google.firebase.firestore.Query
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HelpActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val authManager = FirebaseAuthManager()
    
    private lateinit var adapter: UserSupportTicketAdapter
    private val myTickets = mutableListOf<SupportTicket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_help)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.help_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        setupContactButtons()
        setupReportsList()
        loadMyReports()
    }

    private fun setupReportsList() {
        val rvReports = findViewById<RecyclerView>(R.id.rv_my_reports)
        adapter = UserSupportTicketAdapter(myTickets)
        rvReports.layoutManager = LinearLayoutManager(this)
        rvReports.adapter = adapter
    }

    private fun loadMyReports() {
        val userId = auth.currentUser?.uid ?: return
        val tvNoReports = findViewById<TextView>(R.id.tv_no_reports)

        db.collection("support_tickets")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    myTickets.clear()
                    for (doc in snapshots) {
                        val ticket = doc.toObject(SupportTicket::class.java).copy(id = doc.id)
                        myTickets.add(ticket)
                    }
                    adapter.updateData(myTickets)
                    tvNoReports.visibility = if (myTickets.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    private fun setupContactButtons() {
        // WhatsApp Button
        findViewById<MaterialButton>(R.id.btn_whatsapp).setOnClickListener {
            openWhatsApp()
        }

        // Email Button
        findViewById<MaterialButton>(R.id.btn_email).setOnClickListener {
            openEmail()
        }

        // Send Ticket Button
        findViewById<MaterialButton>(R.id.btn_send_ticket).setOnClickListener {
            submitSupportTicket()
        }
    }

    private fun submitSupportTicket() {
        val subject = findViewById<EditText>(R.id.et_subject).text.toString().trim()
        val message = findViewById<EditText>(R.id.et_message).text.toString().trim()

        if (subject.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Harap isi subjek dan detail keluhan", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        
        findViewById<MaterialButton>(R.id.btn_send_ticket).isEnabled = false
        findViewById<MaterialButton>(R.id.btn_send_ticket).text = "Mengirim..."

        authManager.getUserData(
            onSuccess = { userData ->
                val userName = userData["name"] as? String ?: "User"
                val userEmail = userData["email"] as? String ?: auth.currentUser?.email ?: ""

                val ticket = SupportTicket(
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    subject = subject,
                    message = message,
                    status = SupportTicket.STATUS_OPEN,
                    createdAt = System.currentTimeMillis()
                )

                db.collection("support_tickets")
                    .add(ticket)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Laporan berhasil dikirim! Petugas akan segera menanggapi.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengirim laporan", Toast.LENGTH_SHORT).show()
                        findViewById<MaterialButton>(R.id.btn_send_ticket).isEnabled = true
                        findViewById<MaterialButton>(R.id.btn_send_ticket).text = "Kirim Laporan"
                    }
            },
            onFailure = {
                Toast.makeText(this, "Gagal mengambil data profil", Toast.LENGTH_SHORT).show()
                findViewById<MaterialButton>(R.id.btn_send_ticket).isEnabled = true
                findViewById<MaterialButton>(R.id.btn_send_ticket).text = "Kirim Laporan"
            }
        )
    }

    private fun openWhatsApp() {
        val phoneNumber = "6282316627926" // Nomor Admin User
        val message = "Halo Admin RentEase, saya butuh bantuan mengenai..."
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
        
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp tidak terpasang di perangkat Anda", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEmail() {
        val email = "jarztzy5@gmail.com"
        val subject = "Bantuan Aplikasi RentEase"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Aplikasi Email tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
}
