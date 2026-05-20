package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class RegisterActivity : AppCompatActivity() {
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerButton: MaterialButton
    private lateinit var backBtn: ImageButton
    private var isLoading = false
    private var loadingDialog: AlertDialog? = null
    private val registerTimeoutHandler = Handler(Looper.getMainLooper())
    private lateinit var registerTimeoutRunnable: Runnable
    private val REGISTER_TIMEOUT_MS = 30000L  // 30 seconds timeout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuthManager = FirebaseAuthManager()
        
        // Initialize the timeout runnable with an empty action to prevent crash on destroy if never used
        registerTimeoutRunnable = Runnable { }
        
        initializeViews()
        setupAnimations()
        setupListeners()
    }

    private fun initializeViews() {
        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.register_email_input)
        phoneInput = findViewById(R.id.phone_input)
        passwordInput = findViewById(R.id.register_password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        registerButton = findViewById(R.id.register_button)
        backBtn = findViewById(R.id.register_back_button)
    }

    private fun setupAnimations() {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        findViewById<android.widget.ImageView>(R.id.register_logo).startAnimation(fadeInAnimation)
    }

    private fun setupListeners() {
        val togglePasswordButton = findViewById<ImageButton>(R.id.register_toggle_password)
        val confirmTogglePasswordButton = findViewById<ImageButton>(R.id.confirm_toggle_password)
        val loginLink = findViewById<TextView>(R.id.login_link)

        // Back button click
        backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Real-time validation
        nameInput.addTextChangedListener(createTextWatcher { validateRegisterButton() })
        emailInput.addTextChangedListener(createTextWatcher { validateRegisterButton() })
        phoneInput.addTextChangedListener(createTextWatcher { validateRegisterButton() })
        passwordInput.addTextChangedListener(createTextWatcher { validateRegisterButton() })
        confirmPasswordInput.addTextChangedListener(createTextWatcher { validateRegisterButton() })

        // Toggle password visibility for password field
        togglePasswordButton.setOnClickListener {
            togglePasswordVisibility(passwordInput, togglePasswordButton)
        }

        // Toggle password visibility for confirm password field
        confirmTogglePasswordButton.setOnClickListener {
            togglePasswordVisibility(confirmPasswordInput, confirmTogglePasswordButton)
        }

        // Register button click
        registerButton.setOnClickListener {
            performRegistration()
        }

        // Login link click
        loginLink.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun createTextWatcher(action: () -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            action()
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun validateRegisterButton() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        registerButton.isEnabled = name.isNotEmpty() && email.isNotEmpty() && 
                                   phone.isNotEmpty() && password.isNotEmpty() && 
                                   confirmPassword.isNotEmpty()
    }

    private fun togglePasswordVisibility(passwordField: EditText, toggleButton: ImageButton) {
        if (passwordField == passwordInput) {
            isPasswordVisible = !isPasswordVisible
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }

        val isVisible = if (passwordField == passwordInput) isPasswordVisible else isConfirmPasswordVisible
        
        if (isVisible) {
            passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleButton.setImageResource(R.drawable.ic_password_hide)
        } else {
            passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleButton.setImageResource(R.drawable.ic_password_show)
        }
        passwordField.setSelection(passwordField.text.length)
    }

    private fun performRegistration() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        // Validation
        when {
            name.isEmpty() -> {
                Toast.makeText(this, "Silakan masukkan nama lengkap", Toast.LENGTH_SHORT).show()
                nameInput.requestFocus()
            }
            email.isEmpty() -> {
                Toast.makeText(this, "Silakan masukkan email", Toast.LENGTH_SHORT).show()
                emailInput.requestFocus()
            }
            !isValidEmail(email) -> {
                Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                emailInput.requestFocus()
            }
            phone.isEmpty() -> {
                Toast.makeText(this, "Silakan masukkan nomor telepon", Toast.LENGTH_SHORT).show()
                phoneInput.requestFocus()
            }
            !isValidPhoneNumber(phone) -> {
                Toast.makeText(this, "Nomor telepon harus minimal 10 digit", Toast.LENGTH_SHORT).show()
                phoneInput.requestFocus()
            }
            password.isEmpty() -> {
                Toast.makeText(this, "Silakan masukkan kata sandi", Toast.LENGTH_SHORT).show()
                passwordInput.requestFocus()
            }
            password.length < 6 -> {
                Toast.makeText(this, "Kata sandi harus minimal 6 karakter", Toast.LENGTH_SHORT).show()
                passwordInput.requestFocus()
            }
            confirmPassword != password -> {
                Toast.makeText(this, "Kata sandi tidak cocok", Toast.LENGTH_SHORT).show()
                confirmPasswordInput.requestFocus()
            }
            else -> {
                submitRegistration(email, password, name, phone)
            }
        }
    }

    private fun submitRegistration(email: String, password: String, name: String, phone: String) {
        // Prevent double-click
        if (isLoading) {
            Toast.makeText(this, "Proses pendaftaran sedang berlangsung, harap tunggu...", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        // Show loading dialog
        showLoadingDialog()

        // Set timeout for registration
        setupRegisterTimeout()

        // Attempt Firebase registration
        firebaseAuthManager.registerUser(email, password, name, phone,
            onSuccess = {
                cancelRegisterTimeout()
                if (isLoading) {  // Check if still loading (not timed out)
                    dismissLoadingDialog()
                    Toast.makeText(this, "Pendaftaran berhasil! Silakan login", Toast.LENGTH_SHORT).show()
                    // Sign out to force re-login
                    firebaseAuthManager.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            },
            onFailure = { errorMessage ->
                cancelRegisterTimeout()
                dismissLoadingDialog()
                isLoading = false
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                resetRegisterButton()
            }
        )
    }

    private fun showLoadingDialog() {
        if (loadingDialog?.isShowing == true) return

        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val content = layoutInflater.inflate(R.layout.dialog_loading, null)
        val tvMessage = content.findViewById<TextView>(R.id.tv_loading_text)
        if (tvMessage != null) {
            tvMessage.text = "Memproses pendaftaran...\nHarap tunggu..."
        }
        
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

    private fun setupRegisterTimeout() {
        registerTimeoutRunnable = Runnable {
            if (isLoading) {
                isLoading = false
                dismissLoadingDialog()
                resetRegisterButton()
                Toast.makeText(
                    this,
                    "Pendaftaran timeout. Koneksi internet mungkin lambat. Coba lagi.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        registerTimeoutHandler.postDelayed(registerTimeoutRunnable, REGISTER_TIMEOUT_MS)
    }

    private fun cancelRegisterTimeout() {
        registerTimeoutHandler.removeCallbacks(registerTimeoutRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelRegisterTimeout()
        dismissLoadingDialog()
    }

    private fun resetRegisterButton() {
        registerButton.isEnabled = true
        registerButton.text = "Daftar"
        nameInput.isEnabled = true
        emailInput.isEnabled = true
        phoneInput.isEnabled = true
        passwordInput.isEnabled = true
        confirmPasswordInput.isEnabled = true
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.length >= 10 && phone.all { it.isDigit() }
    }

    private fun navigateToLogin() {
        onBackPressedDispatcher.onBackPressed()
    }
}
