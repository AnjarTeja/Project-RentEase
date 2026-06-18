package com.example.rentease

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DashboardActivity : AppCompatActivity() {
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "DashboardUser"

    // Views
    private lateinit var userNameDisplay: TextView
    private lateinit var statActiveCount: TextView
    private lateinit var statPendingCount: TextView
    private lateinit var statCompletedCount: TextView
    private lateinit var rvItems: RecyclerView
    private lateinit var progressItems: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView
    private lateinit var tvItemCount: TextView

    private lateinit var adapter: BrowseItemAdapter
    private val itemList = mutableListOf<Item>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard_user_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuthManager = FirebaseAuthManager()

        initViews()
        setupTopBar()
        setupMenuListeners()
        setupItemsGrid()
        animateMenuIcons()
        setupBackPressHandler()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        loadUserStats()
        loadAvailableItems()
    }

    private fun initViews() {
        userNameDisplay = findViewById(R.id.user_name_display)
        statActiveCount = findViewById(R.id.stat_active_count)
        statPendingCount = findViewById(R.id.stat_pending_count)
        statCompletedCount = findViewById(R.id.stat_completed_count)
        rvItems = findViewById(R.id.rv_items)
        progressItems = findViewById(R.id.progress_items)
        emptyState = findViewById(R.id.empty_state)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
        tvItemCount = findViewById(R.id.tv_item_count)
    }

    // ===== TOP BAR =====
    private fun setupTopBar() {
        findViewById<CardView>(R.id.btn_profile).setOnClickListener {
            openActivity(ProfileUserActivity::class.java)
        }
    }

    private fun animateCountUp(textView: TextView, target: Int) {
        val anim = ValueAnimator.ofInt(0, target)
        anim.duration = 600
        anim.interpolator = DecelerateInterpolator()
        anim.addUpdateListener { textView.text = it.animatedValue.toString() }
        anim.start()
    }

    // ===== LOAD USER DATA =====
    private fun loadUserData() {
        firebaseAuthManager.getUserData(
            onSuccess = { userData ->
                val name = userData["name"] as? String ?: "Pengguna"
                userNameDisplay.text = name

                // Load avatar
                val profileUrl = userData["profileImageUrl"] as? String
                val ivAvatar = findViewById<ImageView>(R.id.iv_topbar_avatar)
                val ivPlaceholder = findViewById<ImageView>(R.id.iv_topbar_avatar_placeholder)

                if (!profileUrl.isNullOrEmpty()) {
                    try {
                        ivAvatar.setImageURI(Uri.parse(profileUrl))
                        ivPlaceholder.visibility = View.GONE
                    } catch (e: Exception) {
                        ivAvatar.setImageDrawable(null)
                        ivPlaceholder.visibility = View.VISIBLE
                    }
                } else {
                    ivAvatar.setImageDrawable(null)
                    ivPlaceholder.visibility = View.VISIBLE
                }
            },
            onFailure = {
                userNameDisplay.text = "Pengguna"
            }
        )
    }

    // ===== LOAD USER STATS =====
    private fun loadUserStats() {
        val uid = firebaseAuthManager.getCurrentUserUID() ?: return

        // Reset
        statActiveCount.text = "-"
        statPendingCount.text = "-"
        statCompletedCount.text = "-"

        // Active rentals (approved)
        firestore.collection("rentals")
            .whereEqualTo("renterId", uid)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { animateCountUp(statActiveCount, it.size()) }
            .addOnFailureListener { animateCountUp(statActiveCount, 0) }

        // Pending rentals
        firestore.collection("rentals")
            .whereEqualTo("renterId", uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { animateCountUp(statPendingCount, it.size()) }
            .addOnFailureListener { animateCountUp(statPendingCount, 0) }

        // Completed rentals
        firestore.collection("rentals")
            .whereEqualTo("renterId", uid)
            .whereEqualTo("status", "returned")
            .get()
            .addOnSuccessListener { animateCountUp(statCompletedCount, it.size()) }
            .addOnFailureListener { animateCountUp(statCompletedCount, 0) }
    }

    private fun openActivity(activity: Class<*>, extras: ((Intent) -> Unit)? = null) {
        val intent = Intent(this, activity)
        extras?.invoke(intent)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_up_in, R.anim.scale_fade_out)
    }

    // ===== MENU LISTENERS =====
    private fun setupMenuListeners() {
        findViewById<LinearLayout>(R.id.menu_browse).setOnClickListener {
            openActivity(BrowseItemsActivity::class.java)
        }

        findViewById<LinearLayout>(R.id.menu_add_item).setOnClickListener {
            openActivity(AddEditItemActivity::class.java) { it.putExtra("IS_USER", true) }
        }

        findViewById<LinearLayout>(R.id.menu_my_transactions).setOnClickListener {
            openActivity(MyTransactionsActivity::class.java)
        }

        findViewById<LinearLayout>(R.id.menu_my_items).setOnClickListener {
            openActivity(MyItemsActivity::class.java)
        }

        findViewById<LinearLayout>(R.id.menu_incoming_rentals).setOnClickListener {
            openActivity(IncomingRentalsActivity::class.java)
        }

        findViewById<LinearLayout>(R.id.menu_history).setOnClickListener {
            openActivity(HistoryActivity::class.java)
        }

        findViewById<LinearLayout>(R.id.menu_favorites).setOnClickListener {
            openActivity(FavoritesActivity::class.java)
        }

        findViewById<LinearLayout>(R.id.menu_chat).setOnClickListener {
            openActivity(ChatListActivity::class.java)
        }

        findViewById<LinearLayout>(R.id.menu_help).setOnClickListener {
            openActivity(HelpActivity::class.java)
        }
    }

    // ===== ITEMS GRID =====
    private fun setupItemsGrid() {
        adapter = BrowseItemAdapter(itemList) { item ->
            openActivity(ItemDetailActivity::class.java) { it.putExtra("ITEM_ID", item.id) }
        }

        rvItems.layoutManager = GridLayoutManager(this, 2)
        rvItems.adapter = adapter
    }

    private fun loadAvailableItems() {
        progressItems.visibility = View.VISIBLE
        rvItems.visibility = View.GONE
        emptyState.visibility = View.GONE

        firestore.collection("items")
            .whereEqualTo("status", Item.STATUS_AVAILABLE)
            .get()
            .addOnSuccessListener { documents ->
                itemList.clear()

                for (doc in documents) {
                    try {
                        val approval = doc.getString("approvalStatus")
                        
                        // Show if approved OR if the field is missing (for legacy data)
                        if (approval == null || approval == Item.APPROVAL_APPROVED) {
                            val item = Item(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                ownerId = doc.getString("ownerId") ?: "",
                                status = doc.getString("status") ?: Item.STATUS_AVAILABLE,
                                imageUrl = doc.getString("imageUrl") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                approvalStatus = approval ?: Item.APPROVAL_APPROVED,
                                rentCount = (doc.getLong("rentCount") ?: 0L).toInt(),
                                stock = (doc.getLong("stock") ?: 1L).toInt()
                            )
                            itemList.add(item)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing item: ${e.message}")
                    }
                }

                // Sort by most popular first (highest rentCount)
                itemList.sortByDescending { it.rentCount }

                adapter.updateData(itemList)
                progressItems.visibility = View.GONE
                tvItemCount.text = "${itemList.size} barang"

                if (itemList.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvItems.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rvItems.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading items: ${e.message}")
                progressItems.visibility = View.GONE

                // Fallback: load without filter
                loadItemsFallback()
            }
    }

    private fun loadItemsFallback() {
        firestore.collection("items")
            .get()
            .addOnSuccessListener { documents ->
                itemList.clear()

                for (doc in documents) {
                    try {
                        val status = doc.getString("status") ?: Item.STATUS_AVAILABLE
                        val approval = doc.getString("approvalStatus") ?: Item.APPROVAL_APPROVED

                        if (status == Item.STATUS_AVAILABLE && approval == Item.APPROVAL_APPROVED) {
                            val item = Item(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                ownerId = doc.getString("ownerId") ?: "",
                                status = status,
                                imageUrl = doc.getString("imageUrl") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                approvalStatus = approval,
                                rentCount = (doc.getLong("rentCount") ?: 0L).toInt(),
                                stock = (doc.getLong("stock") ?: 1L).toInt()
                            )
                            itemList.add(item)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing item: ${e.message}")
                    }
                }

                itemList.sortByDescending { it.createdAt }
                adapter.updateData(itemList)
                tvItemCount.text = "${itemList.size} barang"

                if (itemList.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvItems.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rvItems.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                emptyState.visibility = View.VISIBLE
                tvEmptyMessage.text = "Gagal memuat barang"
                rvItems.visibility = View.GONE
            }
    }

    // ===== ANIMATIONS =====
    private fun animateMenuIcons() {
        val menus = listOf(
            R.id.menu_browse, R.id.menu_add_item, R.id.menu_my_transactions,
            R.id.menu_my_items, R.id.menu_incoming_rentals, R.id.menu_favorites,
            R.id.menu_chat, R.id.menu_history, R.id.menu_help
        )

        menus.forEachIndexed { index, id ->
            val view = findViewById<View>(id)
            view.alpha = 0f
            view.translationY = 30f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setStartDelay((index * 60).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    // ===== EXIT DIALOG =====
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }

    private fun showExitDialog() {
        DialogUtils.showConfirmationDialog(
            activity = this,
            title = "Keluar Aplikasi",
            message = "Apakah Anda yakin ingin keluar dari aplikasi RentEase?",
            positiveButtonText = "Ya, Keluar"
        ) {
            finishAffinity()
        }
    }
}
