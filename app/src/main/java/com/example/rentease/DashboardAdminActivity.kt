package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
            showLogoutDialog()
        }
    }

    private fun setupMenuButtons() {
        findViewById<LinearLayout>(R.id.manage_users_btn).setOnClickListener {
            Toast.makeText(this, "Panel Kelola User", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.manage_staff_btn).setOnClickListener {
            Toast.makeText(this, "Panel Kelola Petugas", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.view_reports_btn).setOnClickListener {
            val intent = Intent(this, ViewReportsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        findViewById<LinearLayout>(R.id.system_stats_btn).setOnClickListener {
            Toast.makeText(this, "Statistik Global", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.system_settings_btn).setOnClickListener {
            Toast.makeText(this, "Pengaturan Aplikasi", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.customer_service_btn).setOnClickListener {
            val intent = Intent(this, CustomerServiceActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
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

    private fun showLogoutDialog() {
        DialogUtils.showConfirmationDialog(
            activity = this,
            title = "Logout Admin",
            message = "Apakah Anda yakin ingin keluar dari panel Administrator?",
            positiveButtonText = "Ya, Keluar"
        ) {
            logout()
        }
    }

    private fun logout() {
        firebaseAuthManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        showLogoutDialog()
    }
}
