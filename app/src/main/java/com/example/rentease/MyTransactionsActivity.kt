package com.example.rentease

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyTransactionsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tabPending: TextView
    private lateinit var tabActive: TextView
    private lateinit var tabHistory: TextView
    
    private lateinit var rvTransactions: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView

    private lateinit var adapter: UserTransactionAdapter
    private val transactionsList = mutableListOf<RentalRequest>()
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var currentTab = "pending" // pending, active, history

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_transactions)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.my_transactions_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
        setupRecyclerView()
        
        loadTransactions()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tabPending = findViewById(R.id.tab_pending)
        tabActive = findViewById(R.id.tab_active)
        tabHistory = findViewById(R.id.tab_history)
        
        rvTransactions = findViewById(R.id.rv_transactions)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        tabPending.setOnClickListener { switchTab("pending") }
        tabActive.setOnClickListener { switchTab("active") }
        tabHistory.setOnClickListener { switchTab("history") }
    }

    private fun setupRecyclerView() {
        adapter = UserTransactionAdapter(transactionsList) { rental ->
            showReturnDialog(rental)
        }
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        
        // Reset styles
        val activeBg = R.drawable.bg_tab_selected
        val inactiveBg = R.drawable.bg_stat_card
        val activeColor = ContextCompat.getColor(this, R.color.white)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_hint)

        tabPending.setBackgroundResource(if (tab == "pending") activeBg else inactiveBg)
        tabPending.setTextColor(if (tab == "pending") activeColor else inactiveColor)

        tabActive.setBackgroundResource(if (tab == "active") activeBg else inactiveBg)
        tabActive.setTextColor(if (tab == "active") activeColor else inactiveColor)

        tabHistory.setBackgroundResource(if (tab == "history") activeBg else inactiveBg)
        tabHistory.setTextColor(if (tab == "history") activeColor else inactiveColor)

        loadTransactions()
    }

    private fun loadTransactions() {
        val uid = auth.currentUser?.uid ?: return
        
        progressBar.visibility = View.VISIBLE
        rvTransactions.visibility = View.GONE
        emptyState.visibility = View.GONE
        
        val statusList = when (currentTab) {
            "pending" -> listOf(RentalRequest.STATUS_PENDING)
            "active" -> listOf(RentalRequest.STATUS_APPROVED, "return_pending")
            "history" -> listOf(RentalRequest.STATUS_RETURNED, RentalRequest.STATUS_REJECTED)
            else -> listOf(RentalRequest.STATUS_PENDING)
        }

        firestore.collection("rentals")
            .whereEqualTo("renterId", uid)
            .whereIn("status", statusList)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                transactionsList.clear()
                for (doc in documents) {
                    try {
                        val req = doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                        transactionsList.add(req)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                adapter.updateData(transactionsList)
                progressBar.visibility = View.GONE
                
                if (transactionsList.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    tvEmptyMessage.text = when(currentTab) {
                        "pending" -> "Belum ada transaksi menunggu"
                        "active" -> "Tidak ada barang yang sedang disewa"
                        else -> "Belum ada riwayat transaksi"
                    }
                } else {
                    rvTransactions.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                if (e.message?.contains("index") == true) {
                    loadTransactionsFallback(uid, statusList)
                } else {
                    Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadTransactionsFallback(uid: String, statusList: List<String>) {
        firestore.collection("rentals")
            .whereEqualTo("renterId", uid)
            .get()
            .addOnSuccessListener { documents ->
                transactionsList.clear()
                
                val filtered = documents.mapNotNull { 
                    try {
                        it.toObject(RentalRequest::class.java).copy(id = it.id)
                    } catch(e: Exception) { null }
                }.filter { it.status in statusList }
                 .sortedByDescending { it.createdAt }

                transactionsList.addAll(filtered)
                adapter.updateData(transactionsList)
                
                if (transactionsList.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    tvEmptyMessage.text = when(currentTab) {
                        "pending" -> "Belum ada transaksi menunggu"
                        "active" -> "Tidak ada barang yang sedang disewa"
                        else -> "Belum ada riwayat transaksi"
                    }
                } else {
                    rvTransactions.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReturnDialog(rental: RentalRequest) {
        AlertDialog.Builder(this)
            .setTitle("Kembalikan Barang")
            .setMessage("Silakan temui petugas dan serahkan barangnya. Petugas yang akan menyelesaikan transaksi ini.")
            .setPositiveButton("Mengerti") { _, _ -> }
            .show()
    }
}
