package com.example.rentease

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageUsersActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var rvUsers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView
    private lateinit var tvUserCount: TextView
    private val userList = mutableListOf<Map<String, Any>>()
    private var filterStaffOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_users)

        filterStaffOnly = intent.getBooleanExtra("FILTER_STAFF", false)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manage_users_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        setupListeners()

        if (filterStaffOnly) {
            findViewById<TextView>(R.id.tv_empty_message).text = "Belum ada data staff"
        }

        loadUsers()
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    private fun initializeViews() {
        rvUsers = findViewById(R.id.rv_users)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
        tvUserCount = findViewById(R.id.tv_user_count)
    }

    private fun setupRecyclerView() {
        rvUsers.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
    }

    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        rvUsers.visibility = View.GONE
        emptyState.visibility = View.GONE

        firestore.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                userList.clear()
                for (doc in snapshot) {
                    val userData = doc.data.toMutableMap()
                    val role = userData["role"] as? String ?: "user"

                    // Filter staff only if requested
                    if (filterStaffOnly && role == "user") continue

                    userData["docId"] = doc.id
                    userList.add(userData)
                }

                // Sort: staff first, then users, alphabetically
                userList.sortWith(compareByDescending<Map<String, Any>> {
                    val role = it["role"] as? String ?: "user"
                    if (role != "user") 1 else 0
                }.thenBy { (it["name"] as? String ?: "").lowercase() })

                tvUserCount.text = "${userList.size} pengguna"

                val adapter = UserManageAdapter(userList) { user ->
                    showUserRoleDialog(user)
                }
                rvUsers.adapter = adapter

                progressBar.visibility = View.GONE
                if (userList.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                } else {
                    rvUsers.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
                tvEmptyMessage.text = "Gagal memuat data pengguna"
            }
    }

    private fun showUserRoleDialog(user: Map<String, Any>) {
        val userName = user["name"] as? String ?: "Pengguna"
        val userEmail = user["email"] as? String ?: ""
        val currentRole = user["role"] as? String ?: "user"
        val docId = user["docId"] as? String ?: return

        val roles = arrayOf("user", "petugas", "admin")
        val roleLabels = arrayOf("Pengguna (User)", "Petugas", "Administrator")
        val currentIndex = roles.indexOf(currentRole).coerceAtLeast(0)

        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.dialog_title).text = "Ubah Role"
        dialogView.findViewById<TextView>(R.id.dialog_message).text = "$userName\n$userEmail\n\nRole saat ini: ${currentRole.uppercase()}"

        val btnNo = dialogView.findViewById<MaterialButton>(R.id.btn_no)
        val btnYes = dialogView.findViewById<MaterialButton>(R.id.btn_yes)

        btnNo.text = "Batal"
        btnYes.text = "Ubah Role"

        btnNo.setOnClickListener { dialog.dismiss() }

        // Show role selection dialog
        btnYes.setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(this)
                .setTitle("Pilih Role untuk $userName")
                .setItems(roleLabels) { _, which ->
                    val newRole = roles[which]
                    if (newRole != currentRole) {
                        changeUserRole(docId, newRole, userName)
                    }
                }
                .show()
        }

        dialogView.findViewById<View>(R.id.dialog_icon_bg).setBackgroundResource(R.drawable.bg_icon_circle_purple)
        dialog.show()
    }

    private fun changeUserRole(docId: String, newRole: String, userName: String) {
        firestore.collection("users").document(docId)
            .update("role", newRole)
            .addOnSuccessListener {
                Toast.makeText(this, "Role $userName berhasil diubah menjadi $newRole", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengubah role", Toast.LENGTH_SHORT).show()
            }
    }

    // Inner adapter class
    private class UserManageAdapter(
        private val users: List<Map<String, Any>>,
        private val onClick: (Map<String, Any>) -> Unit
    ) : RecyclerView.Adapter<UserManageAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_user_name)
            val tvEmail: TextView = view.findViewById(R.id.tv_user_email)
            val tvRole: TextView = view.findViewById(R.id.tv_user_role)
            val tvDate: TextView = view.findViewById(R.id.tv_user_date)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_manage, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.tvName.text = user["name"] as? String ?: "Tanpa Nama"
            holder.tvEmail.text = user["email"] as? String ?: "-"

            val role = user["role"] as? String ?: "user"
            holder.tvRole.text = role.uppercase()

            val createdAt = user["createdAt"]
            if (createdAt is Long) {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                holder.tvDate.text = sdf.format(Date(createdAt))
            } else {
                holder.tvDate.text = "-"
            }

            holder.itemView.setOnClickListener { onClick(user) }
        }

        override fun getItemCount() = users.size
    }
}
