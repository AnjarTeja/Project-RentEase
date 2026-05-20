package com.example.rentease

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val appNameImageView = findViewById<ImageView>(R.id.app_name_text)

        // Load animations
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Apply animations
        appNameImageView.startAnimation(fadeInAnimation)

        // Navigate immediately to avoid delay
        val firebaseAuthManager = FirebaseAuthManager()
        val isLoggedIn = firebaseAuthManager.isUserLoggedIn()

        if (isLoggedIn) {
            // Get user role and navigate to appropriate dashboard
            firebaseAuthManager.getUserRole(
                onSuccess = { role ->
                    navigateToDashboard(firebaseAuthManager, role)
                },
                onFailure = { error ->
                    // Default to user dashboard if role fetch fails
                    navigateToDashboard(firebaseAuthManager, FirebaseAuthManager.ROLE_USER)
                }
            )
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun navigateToDashboard(firebaseAuthManager: FirebaseAuthManager, role: String) {
        val intent = when (role) {
            FirebaseAuthManager.ROLE_ADMIN -> Intent(this, DashboardAdminActivity::class.java)
            FirebaseAuthManager.ROLE_PETUGAS -> Intent(this, DashboardPetugasActivity::class.java)
            else -> Intent(this, DashboardActivity::class.java)  // Default user
        }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
