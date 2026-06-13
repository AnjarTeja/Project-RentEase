package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashScreenActivity : AppCompatActivity() {

    private var hasNavigated = false

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
        val taglineView = findViewById<TextView>(R.id.tv_tagline)
        val progressBar = findViewById<ProgressBar>(R.id.splash_progress)

        // Load animations
        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Apply zoom to logo
        appNameImageView.startAnimation(zoomInAnimation)

        // Delayed fade-in for tagline and progress bar
        Handler(Looper.getMainLooper()).postDelayed({
            taglineView.animate().alpha(1f).setDuration(800).start()
        }, 500)

        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.animate().alpha(1f).setDuration(400).start()
        }, 1000)

        // Navigate after 2.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (!hasNavigated) {
                hasNavigated = true
                navigateToNextScreen()
            }
        }, 2500)
    }

    private fun navigateToNextScreen() {
        val firebaseAuthManager = FirebaseAuthManager()
        val isLoggedIn = firebaseAuthManager.isUserLoggedIn()

        if (isLoggedIn) {
            firebaseAuthManager.getUserRole(
                onSuccess = { role ->
                    navigateToDashboard(firebaseAuthManager, role)
                },
                onFailure = {
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
            else -> Intent(this, DashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
