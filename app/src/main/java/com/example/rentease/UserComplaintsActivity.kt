package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class UserComplaintsActivity : AppCompatActivity() {
    private lateinit var backButton: ImageButton
    private lateinit var searchView: SearchView
    private lateinit var complaintsListView: ListView
    private lateinit var respondButton: Button
    private lateinit var resolveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_complaints)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_complaints_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupListeners()
        loadComplaints()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.back_button)
        searchView = findViewById(R.id.search_view)
        complaintsListView = findViewById(R.id.complaints_list_view)
        respondButton = findViewById(R.id.respond_button)
        resolveButton = findViewById(R.id.resolve_button)
    }

    private fun setupListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchComplaints(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchComplaints(newText)
                return true
            }
        })

        // Respond button
        respondButton.setOnClickListener {
            respondToComplaint()
        }

        // Resolve button
        resolveButton.setOnClickListener {
            resolveComplaint()
        }
    }

    private fun loadComplaints() {
        // TODO: Fetch complaints from Firestore
        Toast.makeText(this, "Memuat daftar keluhan pengguna...", Toast.LENGTH_SHORT).show()
    }

    private fun searchComplaints(query: String?) {
        // TODO: Filter complaints based on search query
        if (query.isNullOrEmpty()) {
            loadComplaints()
        } else {
            Toast.makeText(this, "Mencari keluhan: $query", Toast.LENGTH_SHORT).show()
        }
    }

    private fun respondToComplaint() {
        // TODO: Get selected complaint and open response dialog/activity
        Toast.makeText(this, "Membuka form balasan keluhan...", Toast.LENGTH_SHORT).show()
    }

    private fun resolveComplaint() {
        // TODO: Mark complaint as resolved in Firestore
        Toast.makeText(this, "Keluhan telah ditandai selesai", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        finish()
    }
}
