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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class ProfileUserActivity : AppCompatActivity() {
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    private var selectedImageUri: Uri? = null
    private var dialogAvatarView: ImageView? = null
    private var dialogAvatarPlaceholder: ImageView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
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
        setContentView(R.layout.activity_profile_user)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_user_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuthManager = FirebaseAuthManager()

        setupBackButton()
        setupEditProfileButton()
        loadProfileData()
        setupLogout()
    }

    /**
     * Navigate back to dashboard
     */
    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    /**
     * Load user profile data from Firestore
     */
    private fun loadProfileData() {
        val userName = findViewById<TextView>(R.id.profile_user_name)
        val userEmail = findViewById<TextView>(R.id.profile_email)
        val userPhone = findViewById<TextView>(R.id.profile_phone)
        val userNik = findViewById<TextView>(R.id.profile_nik)
        val userAddress = findViewById<TextView>(R.id.profile_address)
        val userJoined = findViewById<TextView>(R.id.profile_joined)

        firebaseAuthManager.getUserData(
            onSuccess = { userData ->
                userName.text = userData["name"] as? String ?: "Pengguna"
                userEmail.text = userData["email"] as? String ?: firebaseAuthManager.getCurrentUserEmail() ?: "-"
                userPhone.text = userData["phone"] as? String ?: "-"
                userNik.text = userData["nik"] as? String ?: "-"
                userAddress.text = userData["address"] as? String ?: "-"

                val createdAt = userData["createdAt"]
                if (createdAt is Long) {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    userJoined.text = sdf.format(java.util.Date(createdAt))
                } else {
                    userJoined.text = createdAt?.toString() ?: "-"
                }

                val profileImageUrl = userData["profileImageUrl"] as? String
                val ivProfileAvatar = findViewById<ImageView>(R.id.iv_profile_avatar)
                val ivAvatarPlaceholder = findViewById<ImageView>(R.id.iv_avatar_placeholder)

                if (!profileImageUrl.isNullOrEmpty()) {
                    try {
                        ivProfileAvatar.setImageURI(Uri.parse(profileImageUrl))
                        ivAvatarPlaceholder.visibility = View.GONE
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ivProfileAvatar.setImageDrawable(null)
                        ivAvatarPlaceholder.visibility = View.VISIBLE
                    }
                } else {
                    ivProfileAvatar.setImageDrawable(null)
                    ivAvatarPlaceholder.visibility = View.VISIBLE
                }
            },
            onFailure = { _ ->
                userName.text = "Pengguna"
                userEmail.text = "-"
                userPhone.text = "-"
                userNik.text = "-"
                userAddress.text = "-"
                userJoined.text = "-"
            }
        )
    }

    /**
     * Edit profile logic
     */
    private fun setupEditProfileButton() {
        findViewById<ImageButton>(R.id.btn_edit_profile).setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun showEditProfileDialog() {
        val currentName = findViewById<TextView>(R.id.profile_user_name).text.toString()
        val currentPhone = findViewById<TextView>(R.id.profile_phone).text.toString().replace("-", "")
        val currentNik = findViewById<TextView>(R.id.profile_nik).text.toString().replace("-", "")
        val currentAddress = findViewById<TextView>(R.id.profile_address).text.toString().replace("-", "")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_edit_name)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_edit_phone)
        val etNik = dialogView.findViewById<EditText>(R.id.et_edit_nik)
        val etAddress = dialogView.findViewById<EditText>(R.id.et_edit_address)
        val btnPickAvatar = dialogView.findViewById<TextView>(R.id.btn_pick_avatar)

        dialogAvatarView = dialogView.findViewById(R.id.iv_edit_avatar)
        dialogAvatarPlaceholder = dialogView.findViewById(R.id.iv_edit_placeholder)
        selectedImageUri = null // Reset

        etName.setText(if (currentName == "Pengguna") "" else currentName)
        etPhone.setText(if (currentPhone == "Belum diatur" || currentPhone.isEmpty()) "" else currentPhone)
        etNik.setText(if (currentNik.isEmpty()) "" else currentNik)
        etAddress.setText(if (currentAddress.isEmpty()) "" else currentAddress)

        // Pre-load existing avatar if available
        val ivProfileAvatar = findViewById<ImageView>(R.id.iv_profile_avatar)
        if (ivProfileAvatar.drawable != null) {
            dialogAvatarView?.setImageDrawable(ivProfileAvatar.drawable)
            dialogAvatarPlaceholder?.visibility = View.GONE
        }

        btnPickAvatar.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel_edit)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save_edit)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()
            val newNik = etNik.text.toString().trim()
            val newAddress = etAddress.text.toString().trim()

            if (newName.isNotEmpty()) {
                dialog.dismiss()
                updateProfileData(newName, newPhone, newNik, newAddress)
            } else {
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updateProfileData(newName: String, newPhone: String, newNik: String, newAddress: String) {
        val userId = firebaseAuthManager.getCurrentUserUID() ?: return
        val db = FirebaseFirestore.getInstance()

        val updates = hashMapOf<String, Any>(
            "name" to newName,
            "phone" to newPhone,
            "nik" to newNik,
            "address" to newAddress
        )

        if (selectedImageUri != null) {
            updates["profileImageUrl"] = selectedImageUri.toString()
        }

        db.collection("users").document(userId)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                loadProfileData() // Reload UI
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Setup logout button
     */
    private fun setupLogout() {
        findViewById<LinearLayout>(R.id.logout_btn).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        DialogUtils.showConfirmationDialog(
            activity = this,
            title = "Keluar Akun",
            message = "Apakah Anda yakin ingin keluar dari akun Anda?",
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
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
