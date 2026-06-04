package com.example.rentease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileAdminActivity : AppCompatActivity() {

    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedImageUri: Uri? = null
    private var dialogAvatarView: ImageView? = null
    private var dialogAvatarPlaceholder: ImageView? = null

    // System-wide stats
    private lateinit var tvStatUsers: TextView
    private lateinit var tvStatTransactions: TextView
    private lateinit var tvStatItems: TextView

    // Profile views
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfilePhone: TextView
    private lateinit var tvProfileJoined: TextView
    private lateinit var ivProfileAvatar: ImageView
    private lateinit var ivAvatarPlaceholder: ImageView
    private lateinit var progressBar: ProgressBar

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedImageUri = uri
                dialogAvatarView?.setImageURI(uri)
                dialogAvatarPlaceholder?.visibility = View.GONE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_admin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_admin_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuthManager = FirebaseAuthManager()

        initViews()
        setupBackButton()
        setupEditProfileButton()
        setupChangePasswordButton()
        setupLogout()
        loadProfileData()
        loadSystemStats()
    }

    override fun onResume() {
        super.onResume()
        loadSystemStats()
    }

    private fun initViews() {
        tvProfileName = findViewById(R.id.profile_user_name)
        tvProfileEmail = findViewById(R.id.profile_email)
        tvProfilePhone = findViewById(R.id.profile_phone)
        tvProfileJoined = findViewById(R.id.profile_joined)
        ivProfileAvatar = findViewById(R.id.iv_profile_avatar)
        ivAvatarPlaceholder = findViewById(R.id.iv_avatar_placeholder)
        progressBar = findViewById(R.id.progress_bar)

        tvStatUsers = findViewById(R.id.stat_total_users)
        tvStatTransactions = findViewById(R.id.stat_total_transactions)
        tvStatItems = findViewById(R.id.stat_total_items)
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupEditProfileButton() {
        findViewById<ImageButton>(R.id.btn_edit_profile).setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun setupChangePasswordButton() {
        findViewById<LinearLayout>(R.id.btn_change_password).setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun loadProfileData() {
        progressBar.visibility = View.VISIBLE

        firebaseAuthManager.getUserData(
            onSuccess = { userData ->
                progressBar.visibility = View.GONE

                tvProfileName.text = userData["name"] as? String ?: "Administrator"
                tvProfileEmail.text = userData["email"] as? String
                    ?: firebaseAuthManager.getCurrentUserEmail() ?: "-"
                tvProfilePhone.text = (userData["phone"] as? String)
                    ?.takeIf { it.isNotEmpty() } ?: "-"

                val createdAt = userData["createdAt"]
                if (createdAt is Long) {
                    val sdf =
                        java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault())
                    tvProfileJoined.text = sdf.format(java.util.Date(createdAt))
                } else {
                    tvProfileJoined.text = createdAt?.toString() ?: "-"
                }

                val profileImageUrl = userData["profileImageUrl"] as? String
                if (!profileImageUrl.isNullOrEmpty()) {
                    try {
                        ivProfileAvatar.setImageURI(Uri.parse(profileImageUrl))
                        ivAvatarPlaceholder.visibility = View.GONE
                    } catch (e: Exception) {
                        ivProfileAvatar.setImageDrawable(null)
                        ivAvatarPlaceholder.visibility = View.VISIBLE
                    }
                } else {
                    ivProfileAvatar.setImageDrawable(null)
                    ivAvatarPlaceholder.visibility = View.VISIBLE
                }
            },
            onFailure = {
                progressBar.visibility = View.GONE
                tvProfileName.text = "Administrator"
                tvProfileEmail.text = firebaseAuthManager.getCurrentUserEmail() ?: "-"
                tvProfilePhone.text = "-"
                tvProfileJoined.text = "-"
            }
        )
    }

    private fun loadSystemStats() {
        tvStatUsers.text = "-"
        tvStatTransactions.text = "-"
        tvStatItems.text = "-"

        db.collection("users").get().addOnSuccessListener {
            tvStatUsers.text = it.size().toString()
        }
        db.collection("rentals").get().addOnSuccessListener {
            tvStatTransactions.text = it.size().toString()
        }
        db.collection("items").get().addOnSuccessListener {
            tvStatItems.text = it.size().toString()
        }
    }

    private fun showEditProfileDialog() {
        val currentName = tvProfileName.text.toString()
        val currentPhone =
            tvProfilePhone.text.toString().let { if (it == "-") "" else it }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_edit_name)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_edit_phone)
        val btnPickAvatar = dialogView.findViewById<TextView>(R.id.btn_pick_avatar)

        dialogAvatarView = dialogView.findViewById(R.id.iv_edit_avatar)
        dialogAvatarPlaceholder = dialogView.findViewById(R.id.iv_edit_placeholder)
        selectedImageUri = null

        etName.setText(if (currentName == "Administrator") "" else currentName)
        etPhone.setText(currentPhone)

        if (ivProfileAvatar.drawable != null) {
            dialogAvatarView?.setImageDrawable(ivProfileAvatar.drawable)
            dialogAvatarPlaceholder?.visibility = View.GONE
        }

        btnPickAvatar.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        // Hide NIK and Address – not needed for Admin
        dialogView.findViewById<View>(R.id.container_nik).visibility = View.GONE
        dialogView.findViewById<View>(R.id.container_address).visibility = View.GONE

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel_edit)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save_edit)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            updateProfileData(newName, newPhone)
        }

        dialog.show()
    }

    private fun updateProfileData(newName: String, newPhone: String) {
        val uid = firebaseAuthManager.getCurrentUserUID() ?: return

        val updates = hashMapOf<String, Any>(
            "name" to newName,
            "phone" to newPhone
        )
        if (selectedImageUri != null) {
            updates["profileImageUrl"] = selectedImageUri.toString()
        }

        db.collection("users").document(uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                loadProfileData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangePasswordDialog() {
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)

        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.et_current_password)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.et_new_password)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.et_confirm_password)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<MaterialButton>(R.id.btn_cancel_password).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_save_password).setOnClickListener {
            val currentPwd = etCurrentPassword.text.toString()
            val newPwd = etNewPassword.text.toString()
            val confirmPwd = etConfirmPassword.text.toString()

            when {
                currentPwd.isEmpty() -> {
                    Toast.makeText(this, "Masukkan kata sandi saat ini", Toast.LENGTH_SHORT).show()
                }
                newPwd.length < 6 -> {
                    Toast.makeText(
                        this,
                        "Kata sandi baru minimal 6 karakter",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                newPwd != confirmPwd -> {
                    Toast.makeText(
                        this,
                        "Konfirmasi kata sandi tidak cocok",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    dialog.dismiss()
                    changePassword(currentPwd, newPwd)
                }
            }
        }

        dialog.show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Kata sandi berhasil diubah",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Gagal mengubah kata sandi: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Kata sandi saat ini salah",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setupLogout() {
        findViewById<LinearLayout>(R.id.logout_btn).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        DialogUtils.showConfirmationDialog(
            activity = this,
            title = "Keluar Akun Admin",
            message = "Apakah Anda yakin ingin keluar dari panel Administrator?",
            positiveButtonText = "Ya, Keluar"
        ) {
            firebaseAuthManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
