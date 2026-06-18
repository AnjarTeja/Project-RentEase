package com.example.rentease

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

class ComplaintAdapter(
    private var complaints: List<Complaint>
) : BaseAdapter() {

    private var selectedPosition = -1

    override fun getCount(): Int = complaints.size
    override fun getItem(position: Int): Any = complaints[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_complaint, parent, false)

        val complaint = complaints[position]
        val card = view.findViewById<MaterialCardView>(R.id.card_root)
        val tvSubject = view.findViewById<TextView>(R.id.tv_subject)
        val tvUserEmail = view.findViewById<TextView>(R.id.tv_user_email)
        val tvStatus = view.findViewById<TextView>(R.id.tv_status)
        val tvMessagePreview = view.findViewById<TextView>(R.id.tv_message_preview)
        val tvDate = view.findViewById<TextView>(R.id.tv_date)

        tvSubject.text = complaint.subject
        tvUserEmail.text = complaint.userName
        tvMessagePreview.text = complaint.message

        val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID"))
        tvDate.text = sdf.format(java.util.Date(complaint.createdAt))

        when (complaint.status) {
            Complaint.STATUS_OPEN -> {
                tvStatus.text = "Baru"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_rejected)
            }
            Complaint.STATUS_IN_PROGRESS -> {
                tvStatus.text = "Diproses"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            }
            Complaint.STATUS_RESOLVED -> {
                tvStatus.text = "Selesai"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_approved)
            }
        }

        card.isChecked = position == selectedPosition
        card.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
        }

        return view
    }

    fun getSelectedComplaint(): Complaint? {
        return if (selectedPosition >= 0 && selectedPosition < complaints.size) {
            complaints[selectedPosition]
        } else null
    }

    fun updateData(newComplaints: List<Complaint>) {
        complaints = newComplaints
        selectedPosition = -1
        notifyDataSetChanged()
    }
}
