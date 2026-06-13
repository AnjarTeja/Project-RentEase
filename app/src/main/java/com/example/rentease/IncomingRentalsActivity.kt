package com.example.rentease

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Activity for item owners to see who wants to rent their items.
 * Shows rental requests where current user is the ownerId.
 */
class IncomingRentalsActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvRentals: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView
    private lateinit var tabPending: TextView
    private lateinit var tabApproved: TextView
    private lateinit var tabAll: TextView

    private val rentalList = mutableListOf<RentalRequest>()
    private lateinit var adapter: IncomingRentalAdapter

    private var currentFilter = "all" // "pending", "approved", "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_incoming_rentals)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.incoming_rentals_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupTabs()
        setupRecyclerView()
        loadIncomingRentals()
    }

    override fun onResume() {
        super.onResume()
        loadIncomingRentals()
    }

    private fun initViews() {
        rvRentals = findViewById(R.id.rv_incoming_rentals)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
        tabPending = findViewById(R.id.tab_pending)
        tabApproved = findViewById(R.id.tab_approved)
        tabAll = findViewById(R.id.tab_all)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
    }

    private fun setupTabs() {
        tabPending.setOnClickListener { selectTab("pending") }
        tabApproved.setOnClickListener { selectTab("approved") }
        tabAll.setOnClickListener { selectTab("all") }
        selectTab("pending")
    }

    private fun selectTab(filter: String) {
        currentFilter = filter
        val activeBg = R.drawable.bg_tab_selected
        val inactiveBg = R.drawable.bg_stat_card

        tabPending.setBackgroundResource(if (filter == "pending") activeBg else inactiveBg)
        tabPending.setTextColor(getColor(if (filter == "pending") R.color.white else R.color.text_hint))

        tabApproved.setBackgroundResource(if (filter == "approved") activeBg else inactiveBg)
        tabApproved.setTextColor(getColor(if (filter == "approved") R.color.white else R.color.text_hint))

        tabAll.setBackgroundResource(if (filter == "all") activeBg else inactiveBg)
        tabAll.setTextColor(getColor(if (filter == "all") R.color.white else R.color.text_hint))

        loadIncomingRentals()
    }

    private fun setupRecyclerView() {
        adapter = IncomingRentalAdapter(rentalList)
        rvRentals.layoutManager = LinearLayoutManager(this)
        rvRentals.adapter = adapter
    }

    private fun loadIncomingRentals() {
        val uid = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        rvRentals.visibility = View.GONE
        emptyState.visibility = View.GONE

        firestore.collection("rentals")
            .whereEqualTo("ownerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                rentalList.clear()

                for (doc in documents) {
                    try {
                        val req = doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                        // Filter by tab
                        if (currentFilter == "all" ||
                            (currentFilter == "pending" && req.status == RentalRequest.STATUS_PENDING) ||
                            (currentFilter == "approved" && req.status == RentalRequest.STATUS_APPROVED)
                        ) {
                            rentalList.add(req)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE

                if (rentalList.isEmpty()) {
                    showEmptyState()
                } else {
                    rvRentals.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                // Fallback: try without orderBy (no index yet)
                loadIncomingRentalsFallback(uid)
            }
    }

    private fun loadIncomingRentalsFallback(uid: String) {
        firestore.collection("rentals")
            .whereEqualTo("ownerId", uid)
            .get()
            .addOnSuccessListener { documents ->
                rentalList.clear()

                val sorted = documents.sortedByDescending { it.getLong("createdAt") ?: 0L }
                for (doc in sorted) {
                    try {
                        val req = doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                        if (currentFilter == "all" ||
                            (currentFilter == "pending" && req.status == RentalRequest.STATUS_PENDING) ||
                            (currentFilter == "approved" && req.status == RentalRequest.STATUS_APPROVED)
                        ) {
                            rentalList.add(req)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE

                if (rentalList.isEmpty()) showEmptyState()
                else rvRentals.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        tvEmptyMessage.text = when (currentFilter) {
            "pending" -> "Belum ada permintaan sewa pending"
            "approved" -> "Belum ada permintaan yang disetujui"
            else -> "Belum ada permintaan sewa untuk barang Anda"
        }
    }

    // ===== INNER ADAPTER =====
    inner class IncomingRentalAdapter(
        private val rentals: List<RentalRequest>
    ) : RecyclerView.Adapter<IncomingRentalAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
            val tvStatusBadge: TextView = view.findViewById(R.id.tv_status_badge)
            val tvDurationDate: TextView = view.findViewById(R.id.tv_duration_date)
            val btnReturn: android.widget.Button = view.findViewById(R.id.btn_return_item)
            val tvRating: TextView? = view.findViewById(R.id.tv_rating_display)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val rental = rentals[position]

            holder.tvItemName.text = rental.itemName
            holder.tvDurationDate.text = "Penyewa: ${rental.renterName}\nDurasi: ${rental.duration} hari • Mulai: ${rental.startDate}"
            holder.btnReturn.visibility = View.GONE
            holder.tvRating?.visibility = View.GONE

            when (rental.status) {
                RentalRequest.STATUS_PENDING -> {
                    holder.tvStatusBadge.text = "Menunggu"
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                }
                RentalRequest.STATUS_APPROVED -> {
                    holder.tvStatusBadge.text = "Disetujui"
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_approved)
                }
                RentalRequest.STATUS_RETURNED -> {
                    holder.tvStatusBadge.text = "Dikembalikan"
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_returned)
                }
                RentalRequest.STATUS_REJECTED -> {
                    holder.tvStatusBadge.text = "Ditolak"
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rejected)
                }
            }
        }

        override fun getItemCount() = rentals.size
    }
}
