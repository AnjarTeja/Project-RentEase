package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CustomerServiceActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: SupportTicketAdapter
    private val ticketList = mutableListOf<SupportTicket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_service)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.customer_service_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        setupRecyclerView()
        loadTickets()
        setupRoleTheme()
    }

    private fun setupRoleTheme() {
        val topBar = findViewById<LinearLayout>(R.id.top_bar)
        val firebaseAuthManager = FirebaseAuthManager()
        
        firebaseAuthManager.getUserData(
            onSuccess = { userData ->
                val role = userData["role"] as? String ?: "petugas"
                if (role == "admin") {
                    topBar.setBackgroundResource(R.drawable.bg_header_admin)
                } else {
                    topBar.setBackgroundResource(R.drawable.bg_header_petugas)
                }
            },
            onFailure = {
                topBar.setBackgroundResource(R.drawable.bg_header_petugas)
            }
        )
    }

    private fun setupRecyclerView() {
        val rvTickets = findViewById<RecyclerView>(R.id.rv_tickets)
        adapter = SupportTicketAdapter(ticketList) { ticket ->
            val intent = Intent(this, TicketDetailActivity::class.java)
            intent.putExtra("TICKET_ID", ticket.id)
            startActivity(intent)
        }
        rvTickets.layoutManager = LinearLayoutManager(this)
        rvTickets.adapter = adapter
    }

    private fun loadTickets() {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val emptyState = findViewById<LinearLayout>(R.id.empty_state)

        progressBar.visibility = View.VISIBLE

        db.collection("support_tickets")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                progressBar.visibility = View.GONE
                if (e != null) {
                    Toast.makeText(this, "Gagal memuat laporan", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    ticketList.clear()
                    for (doc in snapshots) {
                        val ticket = doc.toObject(SupportTicket::class.java).copy(id = doc.id)
                        ticketList.add(ticket)
                    }
                    adapter.updateData(ticketList)
                    emptyState.visibility = if (ticketList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }
}
