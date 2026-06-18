package com.example.rentease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageReturnAdapter(
    private var rentalList: List<RentalRequest>,
    private val onReturnClick: (RentalRequest) -> Unit
) : RecyclerView.Adapter<ManageReturnAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRoot: MaterialCardView = view.findViewById(R.id.card_root)
        val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
        val tvRenterName: TextView = view.findViewById(R.id.tv_renter_name)
        val tvRentDate: TextView = view.findViewById(R.id.tv_rent_date)
        val tvOverdueBadge: TextView = view.findViewById(R.id.tv_overdue_badge)
        val tvFineInfo: TextView = view.findViewById(R.id.tv_fine_info)
        val btnReturn: TextView = view.findViewById(R.id.btn_return)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_return_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rental = rentalList[position]

        holder.tvItemName.text = rental.itemName
        holder.tvRenterName.text = "Penyewa: ${rental.renterName}"

        val dateStr = if (rental.createdAt > 0) {
            dateFormat.format(Date(rental.createdAt))
        } else {
            "Tidak diketahui"
        }
        holder.tvRentDate.text = "Tanggal Pinjam: $dateStr"

        if (rental.isOverdue) {
            holder.tvOverdueBadge.visibility = View.VISIBLE
            holder.tvOverdueBadge.text = "Terlambat ${rental.overdueDays} hari"
            holder.tvFineInfo.visibility = View.VISIBLE
            holder.tvFineInfo.text = "Denda: Rp ${String.format("%,.0f", rental.fineAmount)}"
            holder.cardRoot.strokeColor = holder.itemView.context.getColor(R.color.warning_color)
            holder.cardRoot.strokeWidth = 2
        } else {
            holder.tvOverdueBadge.visibility = View.GONE
            holder.tvFineInfo.visibility = View.GONE
            holder.cardRoot.strokeWidth = 0
        }

        holder.btnReturn.setOnClickListener { onReturnClick(rental) }
    }

    override fun getItemCount(): Int = rentalList.size

    fun updateData(newList: List<RentalRequest>) {
        rentalList = newList
        notifyDataSetChanged()
    }
}
