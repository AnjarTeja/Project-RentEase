package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {
    private var isPasswordVisible = false
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: MaterialButton
    private lateinit var togglePasswordButton: ImageButton
    private var isLoading = false
    private var loadingDialog: AlertDialog? = null
    private val loginTimeoutHandler = Handler(Looper.getMainLooper())
    private lateinit var loginTimeoutRunnable: Runnable
    private val LOGIN_TIMEOUT_MS = 30000L  // 30 seconds timeout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuthManager = FirebaseAuthManager()

        // Check if user is already logged in
        if (firebaseAuthManager.isUserLoggedIn()) {
            // Fetch user role and navigate to appropriate dashboard
            firebaseAuthManager.getUserRole(
                onSuccess = { role ->
                    navigateToDashboard(role)
                },
                onFailure = { error ->
                    // Default to user dashboard if role fetch fails
                    navigateToDashboard(FirebaseAuthManager.ROLE_USER)
                }
            )
            return
        }

        setupBackPressHandler()
        initializeViews()
        setupAnimations()
        setupListeners()
    }

    private fun initializeViews() {
        emailEditText = findViewById(R.id.email_input)
        passwordEditText = findViewById(R.id.password_input)
        togglePasswordButton = findViewById(R.id.toggle_password)
        loginButton = findViewById(R.id.login_button)
        val signupButton = findViewById<TextView>(R.id.signup_button)
    }

    private fun setupAnimations() {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        findViewById<android.widget.ImageView>(R.id.login_logo).startAnimation(fadeInAnimation)
    }

    private fun setupListeners() {
        // Real-time input validation
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateLoginButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateLoginButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Toggle password visibility
        togglePasswordButton.setOnClickListener {
            togglePasswordVisibility()
        }

        // Login button click
        loginButton.setOnClickListener {
            performLogin()
        }

        // Signup button click
        val signupButton = findViewById<TextView>(R.id.signup_button)
        signupButton.setOnClickListener {
            navigateToRegister()
        }

        // Forgot password click
        val forgotPasswordButton = findViewById<TextView>(R.id.forgot_password_button)
        forgotPasswordButton.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val etEmail = dialogView.findViewById<EditText>(R.id.et_reset_email)
        
        // Pre-fill with current email if available
        val currentEmail = emailEditText.text.toString().trim()
        if (currentEmail.isNotEmpty() && isValidEmail(currentEmail)) {
            etEmail.setText(currentEmail)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel_reset)
        val btnSend = dialogView.findViewById<MaterialButton>(R.id.btn_send_reset)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Masukkan email Anda", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isValidEmail(email)) {
                Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            btnSend.text = "Mengirim..."

            firebaseAuthManager.sendPasswordReset(
                email = email,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(this, "Email reset kata sandi telah dikirim ke $email", Toast.LENGTH_LONG).show()
                },
                onFailure = { errorMessage ->
                    btnSend.isEnabled = true
                    btnSend.text = "Kirim Reset"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun validateLoginButton() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        loginButton.isEnabled = email.isNotEmpty() && password.isNotEmpty()
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            togglePasswordButton.setImageResource(R.drawable.ic_password_hide)
        } else {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            togglePasswordButton.setImageResource(R.drawable.ic_password_show)
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        when {
            email.isEmpty() || password.isEmpty() -> {
                Toast.makeText(this, "Silakan isi semua field", Toast.LENGTH_SHORT).show()
            }
            !isValidEmail(email) -> {
                Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                emailEditText.requestFocus()
            }
            password.length < 6 -> {
                Toast.makeText(this, "Kata sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                passwordEditText.requestFocus()
            }
            else -> {
                submitLogin(email, password)
            }
        }
    }

    private fun submitLogin(email: String, password: String) {
        // Check network connectivity
        if (!NetworkUtils.checkAndNotify(this)) return

        // Prevent double-click
        if (isLoading) {
            Toast.makeText(this, "Proses login sedang berlangsung, harap tunggu...", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        // Show loading dialog
        showLoadingDialog()

        // Set timeout for login
        setupLoginTimeout()

        // Attempt Firebase login with role detection
        firebaseAuthManager.loginUser(email, password,
            onSuccess = { role ->
                cancelLoginTimeout()
                if (isLoading) {  // Check if still loading (not timed out)
                    dismissLoadingDialog()
                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                    navigateToDashboard(role)
                }
            },
            onFailure = { errorMessage ->
                cancelLoginTimeout()
                dismissLoadingDialog()
                isLoading = false
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                resetLoginButton()
            }
        )
    }

    private fun showLoadingDialog() {
        if (loadingDialog?.isShowing == true) return

        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val content = layoutInflater.inflate(R.layout.dialog_loading, null)
        builder.setView(content)
        
        loadingDialog = builder.create()
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        try {
            loadingDialog?.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupLoginTimeout() {
        loginTimeoutRunnable = Runnable {
            if (isLoading) {
                isLoading = false
                dismissLoadingDialog()
                resetLoginButton()
                Toast.makeText(
                    this,
                    "Login timeout. Koneksi internet mungkin lambat. Coba lagi.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        loginTimeoutHandler.postDelayed(loginTimeoutRunnable, LOGIN_TIMEOUT_MS)
    }

    private fun cancelLoginTimeout() {
        loginTimeoutHandler.removeCallbacks(loginTimeoutRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelLoginTimeout()
        dismissLoadingDialog()
    }

    private fun resetLoginButton() {
        loginButton.isEnabled = true
        loginButton.text = "Masuk"
        emailEditText.isEnabled = true
        passwordEditText.isEnabled = true
        togglePasswordButton.isEnabled = true
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun navigateToDashboard(role: String) {
        val intent = when (role) {
            FirebaseAuthManager.ROLE_ADMIN -> Intent(this, DashboardAdminActivity::class.java)
            FirebaseAuthManager.ROLE_PETUGAS -> Intent(this, DashboardPetugasActivity::class.java)
            else -> Intent(this, DashboardActivity::class.java)  // Default user
        }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun showExitConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set content
        dialogView.findViewById<TextView>(R.id.dialog_title).text = "Keluar Aplikasi"
        dialogView.findViewById<TextView>(R.id.dialog_message).text = "Apakah Anda yakin ingin keluar dari aplikasi RentEase?"
        
        val btnNo = dialogView.findViewById<MaterialButton>(R.id.btn_no)
        val btnYes = dialogView.findViewById<MaterialButton>(R.id.btn_yes)

        btnNo.setOnClickListener { dialog.dismiss() }
        btnYes.setOnClickListener {
            dialog.dismiss()
            finishAffinity()
        }

        // Apply Warning theme for exit
        btnYes.setBackgroundColor(getColor(R.color.warning_color))
        dialogView.findViewById<View>(R.id.dialog_icon_bg).setBackgroundResource(R.drawable.bg_icon_circle_orange)

        dialog.show()
    }
}
