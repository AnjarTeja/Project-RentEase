package com.example.rentease

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RentalRequestAdapter(
    private val context: Context,
    private val rentalList: List<RentalRequest>,
    private val onApproveClick: (RentalRequest) -> Unit,
    private val onRejectClick: (RentalRequest) -> Unit
) : RecyclerView.Adapter<RentalRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
        val tvStatusBadge: TextView = view.findViewById(R.id.tv_status_badge)
        val tvRenterName: TextView = view.findViewById(R.id.tv_renter_name)
        val tvRentalDate: TextView = view.findViewById(R.id.tv_rental_date)
        val actionContainer: LinearLayout = view.findViewById(R.id.action_buttons_container)
        val btnApprove: TextView = view.findViewById(R.id.btn_approve)
        val btnReject: TextView = view.findViewById(R.id.btn_reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rental_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rental = rentalList[position]

        holder.tvItemName.text = rental.itemName.ifEmpty { "Barang Tidak Diketahui" }
        holder.tvRenterName.text = rental.renterName.ifEmpty { "Penyewa Anonim" }

        // Date format
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateString = try {
            if (rental.startDate.isNotEmpty()) {
                rental.startDate
            } else {
                dateFormat.format(Date(rental.createdAt))
            }
        } catch (e: Exception) {
            "Tanggal tidak valid"
        }
        
        holder.tvRentalDate.text = "$dateString — ${rental.duration} hari"

        // Status Styling
        when (rental.status) {
            RentalRequest.STATUS_PENDING -> {
                holder.tvStatusBadge.text = "Pending"
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_color))
                holder.actionContainer.visibility = View.VISIBLE
            }
            RentalRequest.STATUS_APPROVED -> {
                holder.tvStatusBadge.text = "Disetujui"
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success_color))
                holder.actionContainer.visibility = View.GONE
            }
            RentalRequest.STATUS_REJECTED -> {
                holder.tvStatusBadge.text = "Ditolak"
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error_color))
                holder.actionContainer.visibility = View.GONE
            }
            else -> {
                holder.tvStatusBadge.text = rental.status
                holder.tvStatusBadge.setTextColor(Color.WHITE)
                holder.actionContainer.visibility = View.GONE
            }
        }

        // Click listeners
        holder.btnApprove.setOnClickListener { onApproveClick(rental) }
        holder.btnReject.setOnClickListener { onRejectClick(rental) }
    }

    override fun getItemCount(): Int = rentalList.size
}
