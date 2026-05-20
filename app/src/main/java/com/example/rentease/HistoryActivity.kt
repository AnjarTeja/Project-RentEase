package com.example.rentease

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: HistoryAdapter
    private val historyList = mutableListOf<RentalRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.history_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        setupRecyclerView()
        loadHistory()
    }

    private fun setupRecyclerView() {
        val rvHistory = findViewById<RecyclerView>(R.id.rv_history)
        adapter = HistoryAdapter(historyList)
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter
    }

    private fun loadHistory() {
        val uid = auth.currentUser?.uid ?: return
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val emptyState = findViewById<LinearLayout>(R.id.empty_state)

        progressBar.visibility = View.VISIBLE

        db.collection("rentals")
            .whereEqualTo("renterId", uid)
            .whereIn("status", listOf(RentalRequest.STATUS_RETURNED, RentalRequest.STATUS_REJECTED))
            .get()
            .addOnSuccessListener { documents ->
                historyList.clear()
                val pendingFallbacks = mutableListOf<RentalRequest>()

                for (doc in documents) {
                    val request = doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                    // Check if old data (missing price or image)
                    if (request.pricePerDay <= 0 || request.itemImageUrl.isEmpty()) {
                        pendingFallbacks.add(request)
                    } else {
                        historyList.add(request)
                    }
                }

                if (pendingFallbacks.isEmpty()) {
                    completeLoading(progressBar, emptyState)
                } else {
                    // Fetch missing details for old records
                    var completedCount = 0
                    for (req in pendingFallbacks) {
                        db.collection("items").document(req.itemId).get()
                            .addOnSuccessListener { itemDoc ->
                                val price = itemDoc.getDouble("price") ?: 0.0
                                val imageUrl = itemDoc.getString("imageUrl") ?: ""
                                
                                val updatedReq = req.copy(
                                    pricePerDay = price,
                                    itemImageUrl = imageUrl
                                )
                                historyList.add(updatedReq)
                                
                                completedCount++
                                if (completedCount == pendingFallbacks.size) {
                                    completeLoading(progressBar, emptyState)
                                }
                            }
                            .addOnFailureListener {
                                historyList.add(req) // Add anyway without details
                                completedCount++
                                if (completedCount == pendingFallbacks.size) {
                                    completeLoading(progressBar, emptyState)
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memuat riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun completeLoading(progressBar: ProgressBar, emptyState: LinearLayout) {
        // Sort manually in memory
        historyList.sortByDescending { it.updatedAt }
        adapter.updateData(historyList)
        progressBar.visibility = View.GONE
        emptyState.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
    }
}
