package com.example.rentease

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserComplaintsActivity : AppCompatActivity() {
    private lateinit var backButton: ImageButton
    private lateinit var searchView: SearchView
    private lateinit var complaintsListView: ListView
    private lateinit var respondButton: Button
    private lateinit var resolveButton: Button
    private lateinit var tvStatOpen: TextView
    private lateinit var tvStatProgress: TextView
    private lateinit var tvStatResolved: TextView

    private val db = FirebaseFirestore.getInstance()
    private val allComplaints = mutableListOf<Complaint>()
    private var filteredComplaints = mutableListOf<Complaint>()
    private lateinit var adapter: ComplaintAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_complaints)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_complaints_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupListeners()
        loadComplaints()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.back_button)
        searchView = findViewById(R.id.search_view)
        complaintsListView = findViewById(R.id.complaints_list_view)
        respondButton = findViewById(R.id.respond_button)
        resolveButton = findViewById(R.id.resolve_button)
        tvStatOpen = findViewById(R.id.stat_open_count)
        tvStatProgress = findViewById(R.id.stat_progress_count)
        tvStatResolved = findViewById(R.id.stat_resolved_count)

        adapter = ComplaintAdapter(filteredComplaints)
        complaintsListView.adapter = adapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchComplaints(query)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                searchComplaints(newText)
                return true
            }
        })

        respondButton.setOnClickListener { respondToComplaint() }
        resolveButton.setOnClickListener { resolveComplaint() }
    }

    private fun loadComplaints() {
        db.collection("complaints")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    loadComplaintsFallback()
                    return@addSnapshotListener
                }
                if (snapshots != null) processComplaints(snapshots.documents)
            }
    }

    private fun loadComplaintsFallback() {
        db.collection("complaints")
            .get()
            .addOnSuccessListener { snapshots ->
                val sorted = snapshots.sortedByDescending { it.getLong("createdAt") ?: 0L }
                processComplaints(sorted)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat keluhan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processComplaints(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        allComplaints.clear()
        for (doc in documents) {
            try {
                val complaint = Complaint(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "",
                    userEmail = doc.getString("userEmail") ?: "",
                    subject = doc.getString("subject") ?: "",
                    message = doc.getString("message") ?: "",
                    status = doc.getString("status") ?: Complaint.STATUS_OPEN,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    replyMessage = doc.getString("replyMessage") ?: "",
                    repliedAt = doc.getLong("repliedAt") ?: 0L,
                    repliedBy = doc.getString("repliedBy") ?: ""
                )
                allComplaints.add(complaint)
            } catch (e: Exception) { /* skip */ }
        }
        applyFilter()
    }

    private fun searchComplaints(query: String?) {
        if (query.isNullOrEmpty()) {
            applyFilter()
        } else {
            filteredComplaints = allComplaints.filter {
                it.subject.contains(query, ignoreCase = true) ||
                it.message.contains(query, ignoreCase = true) ||
                it.userName.contains(query, ignoreCase = true)
            }.toMutableList()
            updateUI()
        }
    }

    private fun applyFilter() {
        filteredComplaints = allComplaints.toMutableList()
        updateUI()
    }

    private fun updateUI() {
        adapter.updateData(filteredComplaints)

        val openCount = allComplaints.count { it.status == Complaint.STATUS_OPEN }
        val progressCount = allComplaints.count { it.status == Complaint.STATUS_IN_PROGRESS }
        val resolvedCount = allComplaints.count { it.status == Complaint.STATUS_RESOLVED }

        tvStatOpen.text = openCount.toString()
        tvStatProgress.text = progressCount.toString()
        tvStatResolved.text = resolvedCount.toString()
    }

    private fun respondToComplaint() {
        val complaint = adapter.getSelectedComplaint()
        if (complaint == null) {
            Toast.makeText(this, "Pilih keluhan terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Balas Keluhan: ${complaint.subject}")

        val input = EditText(this)
        input.hint = "Tulis balasan..."
        input.setText(complaint.replyMessage)
        builder.setView(input)

        builder.setPositiveButton("Kirim") { _, _ ->
            val reply = input.text.toString().trim()
            if (reply.isEmpty()) {
                Toast.makeText(this, "Balasan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            sendReply(complaint, reply)
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun sendReply(complaint: Complaint, reply: String) {
        val updates = mapOf<String, Any>(
            "replyMessage" to reply,
            "repliedAt" to System.currentTimeMillis(),
            "repliedBy" to (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""),
            "status" to Complaint.STATUS_IN_PROGRESS
        )

        db.collection("complaints").document(complaint.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Balasan berhasil dikirim", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengirim balasan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resolveComplaint() {
        val complaint = adapter.getSelectedComplaint()
        if (complaint == null) {
            Toast.makeText(this, "Pilih keluhan terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Selesaikan Keluhan")
            .setMessage("Tandai keluhan \"${complaint.subject}\" sebagai selesai?")
            .setPositiveButton("Ya, Selesai") { _, _ ->
                db.collection("complaints").document(complaint.id)
                    .update("status", Complaint.STATUS_RESOLVED)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Keluhan telah ditandai selesai", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menyelesaikan keluhan", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

}
