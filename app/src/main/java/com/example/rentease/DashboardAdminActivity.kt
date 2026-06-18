package com.example.rentease

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class DashboardAdminActivity : AppCompatActivity() {
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_admin)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard_admin_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuthManager = FirebaseAuthManager()

        setupProfileHeader()
        setupMenuButtons()
        loadStats()
        setupBackPressHandler()
    }

    override fun onResume() {
        super.onResume()
        // Reload profile data and stats whenever activity comes back to focus
        refreshProfileHeader()
        loadStats()
    }

    private fun refreshProfileHeader() {
        val userNameDisplay = findViewById<TextView>(R.id.user_name_display_admin)
        
        firebaseAuthManager.getUserData(
            onSuccess = { userData ->
                val userName = userData["name"] as? String ?: "Administrator"
                userNameDisplay.text = userName
                
                // Update profile image if exists
                val profileImageUrl = userData["profileImageUrl"] as? String
                if (!profileImageUrl.isNullOrEmpty()) {
                    val ivAvatar = findViewById<ImageView>(R.id.iv_dashboard_avatar)
                    val ivPlaceholder = findViewById<ImageView>(R.id.iv_dashboard_avatar_placeholder)
                    try {
                        ivAvatar.setImageURI(android.net.Uri.parse(profileImageUrl))
                        ivPlaceholder.visibility = View.GONE
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onFailure = { 
                userNameDisplay.text = "Administrator"
            }
        )
    }

    private fun setupProfileHeader() {
        val userNameDisplay = findViewById<TextView>(R.id.user_name_display_admin)
        
        firebaseAuthManager.getUserData(
            onSuccess = { userData ->
                val userName = userData["name"] as? String ?: "Administrator"
                userNameDisplay.text = userName
                
                // Handle profile image if exists
                val profileImageUrl = userData["profileImageUrl"] as? String
                if (!profileImageUrl.isNullOrEmpty()) {
                    val ivAvatar = findViewById<ImageView>(R.id.iv_dashboard_avatar)
                    val ivPlaceholder = findViewById<ImageView>(R.id.iv_dashboard_avatar_placeholder)
                    try {
                        ivAvatar.setImageURI(android.net.Uri.parse(profileImageUrl))
                        ivPlaceholder.visibility = View.GONE
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onFailure = { 
                userNameDisplay.text = "Administrator"
            }
        )

        findViewById<LinearLayout>(R.id.profile_section_btn).setOnClickListener {
            val intent = Intent(this, ProfileAdminActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupMenuButtons() {
        // Manage Users - navigate to ManageUsersActivity
        findViewById<LinearLayout>(R.id.manage_users_btn).setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Manage Staff - navigate to ManageUsersActivity (shows all users, staff management)
        findViewById<LinearLayout>(R.id.manage_staff_btn).setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            intent.putExtra("FILTER_STAFF", true)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        findViewById<LinearLayout>(R.id.view_reports_btn).setOnClickListener {
            val intent = Intent(this, ViewReportsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // System Statistics - show summary dialog
        findViewById<LinearLayout>(R.id.system_stats_btn).setOnClickListener {
            showSystemStatsDialog()
        }

        // System Settings - show settings info
        findViewById<LinearLayout>(R.id.system_settings_btn).setOnClickListener {
            showSystemSettingsDialog()
        }

        findViewById<LinearLayout>(R.id.customer_service_btn).setOnClickListener {
            val intent = Intent(this, CustomerServiceActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun showSystemStatsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_stats, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvUsers = dialogView.findViewById<TextView>(R.id.tv_stat_users_chart)
        val tvItems = dialogView.findViewById<TextView>(R.id.tv_stat_items_chart)
        val tvRentals = dialogView.findViewById<TextView>(R.id.tv_stat_rentals_chart)
        val pieChart = dialogView.findViewById<PieChart>(R.id.pie_chart)
        val tvLegend = dialogView.findViewById<TextView>(R.id.tv_chart_legend)

        dialog.show()

        var totalUsers = 0
        var totalItems = 0
        var pendingRentals = 0
        var approvedRentals = 0
        var returnedRentals = 0
        var openTickets = 0
        var loaded = 0
        val totalQueries = 3

        fun updateStats() {
            loaded++
            if (loaded >= totalQueries) {
                tvUsers.text = totalUsers.toString()
                tvItems.text = totalItems.toString()
                tvRentals.text = (pendingRentals + approvedRentals + returnedRentals).toString()

                // Setup Pie Chart
                val entries = mutableListOf<PieEntry>()
                if (pendingRentals > 0) entries.add(PieEntry(pendingRentals.toFloat(), "Pending"))
                if (approvedRentals > 0) entries.add(PieEntry(approvedRentals.toFloat(), "Disetujui"))
                if (returnedRentals > 0) entries.add(PieEntry(returnedRentals.toFloat(), "Dikembalikan"))

                if (entries.isNotEmpty()) {
                    val dataSet = PieDataSet(entries, "").apply {
                        colors = listOf(
                            Color.rgb(255, 170, 0),   // Orange for pending
                            Color.rgb(0, 255, 136),   // Green for approved
                            Color.rgb(0, 217, 255)    // Cyan for returned
                        )
                        valueTextColor = Color.WHITE
                        valueTextSize = 12f
                    }

                    pieChart.data = PieData(dataSet)
                    pieChart.description.isEnabled = false
                    pieChart.setDrawEntryLabels(true)
                    pieChart.setEntryLabelColor(Color.WHITE)
                    pieChart.setEntryLabelTextSize(11f)
                    pieChart.setUsePercentValues(true)
                    pieChart.holeRadius = 40f
                    pieChart.setHoleColor(Color.TRANSPARENT)
                    pieChart.transparentCircleRadius = 45f
                    pieChart.legend.isEnabled = false
                    pieChart.invalidate()
                } else {
                    pieChart.visibility = View.GONE
                }

                tvLegend.text = buildString {
                    append("• Pengguna: $totalUsers\n")
                    append("• Barang: $totalItems\n")
                    append("• Tiket Terbuka: $openTickets\n")
                    append("• Pending: $pendingRentals | Disetujui: $approvedRentals | Kembali: $returnedRentals")
                }
            }
        }

        db.collection("users").get().addOnSuccessListener {
            totalUsers = it.size(); updateStats()
        }.addOnFailureListener { updateStats() }

        db.collection("items").get().addOnSuccessListener {
            totalItems = it.size(); updateStats()
        }.addOnFailureListener { updateStats() }

        db.collection("rentals").get().addOnSuccessListener {
            for (doc in it) {
                when (doc.getString("status")) {
                    "pending" -> pendingRentals++
                    "approved" -> approvedRentals++
                    "returned" -> returnedRentals++
                }
            }
            updateStats()
        }.addOnFailureListener { updateStats() }

        db.collection("support_tickets").whereEqualTo("status", "open").get()
            .addOnSuccessListener { openTickets = it.size(); updateStats() }
            .addOnFailureListener { updateStats() }
    }

    private fun showSystemSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Pengaturan Aplikasi")
            .setMessage("RentEase v1.0\n\n• Notifikasi: Aktif\n• Mode Offline: Aktif\n• Tema: Dark Tech\n\nPengaturan lebih lanjut akan tersedia di versi berikutnya.")
            .setPositiveButton("Tutup") { d, _ -> d.dismiss() }
            .show()
    }

    private fun loadStats() {
        // Total Users
        db.collection("users").get().addOnSuccessListener { snapshot ->
            findViewById<TextView>(R.id.stat_total_users).text = snapshot.size().toString()
        }

        // Total Transactions (Rentals)
        db.collection("rentals").get().addOnSuccessListener { snapshot ->
            findViewById<TextView>(R.id.stat_total_transactions).text = snapshot.size().toString()
        }

        // Total Items
        db.collection("items").get().addOnSuccessListener { snapshot ->
            findViewById<TextView>(R.id.stat_total_items).text = snapshot.size().toString()
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

    private fun logout() {
        firebaseAuthManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }
}
