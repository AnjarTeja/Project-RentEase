package com.example.rentease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class DashboardPetugasActivity : AppCompatActivity() {
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "DashboardPetugas"

    // Stat card TextViews
    private lateinit var pendingCountView: TextView
    private lateinit var approvedCountView: TextView
    private lateinit var itemsCountView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_petugas)

        // Apply window insets to root scroll view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard_petugas_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuthManager = FirebaseAuthManager()

        // Initialize stat card views
        pendingCountView = findViewById(R.id.stat_pending_count)
        approvedCountView = findViewById(R.id.stat_approved_count)
        itemsCountView = findViewById(R.id.stat_items_count)

        setupUserInfo()
        setupProfileNavigation()
        setupMenuListeners()
        loadStats()
        animateMenuCards()
        setupBackPressHandler()
    }

    /**
     * Refresh stats every time user returns to this screen
     */
    override fun onResume() {
        super.onResume()
        loadStats()
        setupUserInfo()
    }

    /**
     * Fetch user data from Firestore and display welcome name
     */
    private fun setupUserInfo() {
        val userNameDisplay = findViewById<TextView>(R.id.user_name_display_petugas)

        firebaseAuthManager.getUserData(
            onSuccess = { userData ->
                val name = userData["name"] as? String ?: "Petugas"
                userNameDisplay.text = name
                
                val profileImageUrl = userData["profileImageUrl"] as? String
                val ivDashboardAvatar = findViewById<ImageView>(R.id.iv_dashboard_avatar)
                val ivDashboardPlaceholder = findViewById<ImageView>(R.id.iv_dashboard_avatar_placeholder)
                
                if (!profileImageUrl.isNullOrEmpty()) {
                    try {
                        ivDashboardAvatar.setImageURI(Uri.parse(profileImageUrl))
                        ivDashboardPlaceholder.visibility = View.GONE
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ivDashboardAvatar.setImageDrawable(null)
                        ivDashboardPlaceholder.visibility = View.VISIBLE
                    }
                } else {
                    ivDashboardAvatar.setImageDrawable(null)
                    ivDashboardPlaceholder.visibility = View.VISIBLE
                }
            },
            onFailure = { _ ->
                userNameDisplay.text = "Petugas"
            }
        )
    }

    /**
     * Navigate to profile page when tapping avatar/name area
     */
    private fun setupProfileNavigation() {
        findViewById<LinearLayout>(R.id.profile_section_btn).setOnClickListener {
            val intent = Intent(this, ProfilePetugasActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    /**
     * Set up click listeners for all menu cards
     */
    private fun setupMenuListeners() {
        // Verify Rental Button
        findViewById<LinearLayout>(R.id.verify_rental_btn).setOnClickListener {
            val intent = Intent(this, VerifyRentalActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Manage Items Button
        findViewById<LinearLayout>(R.id.manage_items_btn).setOnClickListener {
            val intent = Intent(this, ManageItemsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Verify User Items Button
        findViewById<LinearLayout>(R.id.verify_user_items_btn).setOnClickListener {
            val intent = Intent(this, VerifyUserItemsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Manage Returns Button
        findViewById<LinearLayout>(R.id.manage_returns_btn).setOnClickListener {
            val intent = Intent(this, ManageReturnsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // View Reports Button
        findViewById<LinearLayout>(R.id.view_reports_btn).setOnClickListener {
            val intent = Intent(this, ViewReportsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Customer Service Button
        findViewById<LinearLayout>(R.id.customer_service_btn).setOnClickListener {
            val intent = Intent(this, CustomerServiceActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    /**
     * Load real-time stats from Firebase Firestore:
     * - Pending: count of documents in "rentals" where status == "pending"
     * - Disetujui: count of documents in "rentals" where status == "approved"
     * - Barang: total count of documents in "items" collection
     */
    private fun loadStats() {
        // Show loading indicator
        pendingCountView.text = "-"
        approvedCountView.text = "-"
        itemsCountView.text = "-"

        // Query pending rentals
        firestore.collection("rentals")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                pendingCountView.text = snapshot.size().toString()
                Log.d(TAG, "Pending rentals: ${snapshot.size()}")
            }
            .addOnFailureListener { e ->
                pendingCountView.text = "0"
                Log.e(TAG, "Error loading pending count: ${e.message}")
            }

        // Query approved rentals
        firestore.collection("rentals")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { snapshot ->
                approvedCountView.text = snapshot.size().toString()
                Log.d(TAG, "Approved rentals: ${snapshot.size()}")
            }
            .addOnFailureListener { e ->
                approvedCountView.text = "0"
                Log.e(TAG, "Error loading approved count: ${e.message}")
            }

        // Query total items
        firestore.collection("items")
            .get()
            .addOnSuccessListener { snapshot ->
                itemsCountView.text = snapshot.size().toString()
                Log.d(TAG, "Total items: ${snapshot.size()}")
            }
            .addOnFailureListener { e ->
                itemsCountView.text = "0"
                Log.e(TAG, "Error loading items count: ${e.message}")
            }

        // Query open support tickets
        val ticketBadge = findViewById<TextView>(R.id.tv_ticket_count_badge)
        firestore.collection("support_tickets")
            .whereEqualTo("status", "open")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.size() > 0) {
                    ticketBadge.text = snapshot.size().toString()
                    ticketBadge.visibility = View.VISIBLE
                } else {
                    ticketBadge.visibility = View.GONE
                }
            }

        // Check overdue rentals
        checkOverdueRentals()
    }

    /**
     * Check for rentals that have passed their endDate.
     * Shows a red warning banner if overdue rentals found.
     */
    private fun checkOverdueRentals() {
        val overdueBanner = findViewById<LinearLayout>(R.id.overdue_banner)
        val overdueTitle = findViewById<TextView>(R.id.tv_overdue_title)

        firestore.collection("rentals")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { snapshot ->
                val today = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
                    .format(java.util.Date())
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
                val todayDate = sdf.parse(today)

                var overdueCount = 0
                for (doc in snapshot) {
                    val endDateStr = doc.getString("endDate") ?: continue
                    try {
                        val endDate = sdf.parse(endDateStr)
                        if (endDate != null && todayDate != null && endDate.before(todayDate)) {
                            overdueCount++
                        }
                    } catch (e: Exception) {
                        // Skip parsing errors
                    }
                }

                if (overdueCount > 0) {
                    overdueTitle.text = "$overdueCount Rental Terlewat Batas!"
                    overdueBanner.visibility = View.VISIBLE
                    Log.d(TAG, "Overdue rentals: $overdueCount")
                } else {
                    overdueBanner.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                overdueBanner.visibility = View.GONE
                Log.e(TAG, "Error checking overdue: ${e.message}")
            }
    }

    /**
     * Animate menu cards with a staggered slide-in effect
     */
    private fun animateMenuCards() {
        val menuCards = listOf(
            findViewById<LinearLayout>(R.id.verify_rental_btn),
            findViewById<LinearLayout>(R.id.manage_items_btn),
            findViewById<LinearLayout>(R.id.verify_user_items_btn),
            findViewById<LinearLayout>(R.id.manage_returns_btn),
            findViewById<LinearLayout>(R.id.view_reports_btn),
            findViewById<LinearLayout>(R.id.customer_service_btn)
        )

        menuCards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 60f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay((index * 100).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun showExitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set content for Exit
        dialogView.findViewById<TextView>(R.id.dialog_title).text = "Keluar Aplikasi"
        dialogView.findViewById<TextView>(R.id.dialog_message).text = "Apakah Anda yakin ingin keluar dari aplikasi RentEase?"
        
        val btnNo = dialogView.findViewById<MaterialButton>(R.id.btn_no)
        val btnYes = dialogView.findViewById<MaterialButton>(R.id.btn_yes)
        
        btnNo.text = "Batal"
        btnYes.text = "Ya, Keluar"

        btnNo.setOnClickListener { dialog.dismiss() }
        btnYes.setOnClickListener {
            dialog.dismiss()
            finishAffinity()
        }

        // Set Warning color for exit
        btnYes.setBackgroundColor(getColor(R.color.warning_color))
        dialogView.findViewById<View>(R.id.dialog_icon_bg).setBackgroundResource(R.drawable.bg_icon_circle_orange)
        
        dialog.show()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }
}
