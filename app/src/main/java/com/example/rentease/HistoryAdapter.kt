package com.example.rentease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var historyList: List<RentalRequest>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
        val tvHistoryDate: TextView = view.findViewById(R.id.tv_history_date)
        val tvDuration: TextView = view.findViewById(R.id.tv_duration)
        val tvStatusText: TextView = view.findViewById(R.id.tv_status_text)
        val tvTotalPrice: TextView = view.findViewById(R.id.tv_total_price)
        val ivItemImage: ImageView = view.findViewById(R.id.iv_item_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = historyList[position]
        val context = holder.itemView.context

        holder.tvItemName.text = request.itemName
        holder.tvDuration.text = "${request.duration} Hari"

        // Format Price
        val totalPrice = request.pricePerDay * request.duration
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0
        holder.tvTotalPrice.text = formatter.format(totalPrice)

        // Load Image
        if (request.itemImageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(request.itemImageUrl)
                .placeholder(R.drawable.bg_card_dark)
                .into(holder.ivItemImage)
        } else {
            holder.ivItemImage.setImageResource(R.drawable.bg_card_dark)
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        val dateStr = sdf.format(Date(request.updatedAt))
        
        when (request.status) {
            RentalRequest.STATUS_RETURNED -> {
                holder.tvStatusText.text = "Selesai"
                holder.tvStatusText.setBackgroundResource(R.drawable.bg_badge_returned)
                holder.tvHistoryDate.text = "Dikembalikan pada $dateStr"
            }
            RentalRequest.STATUS_REJECTED -> {
                holder.tvStatusText.text = "Ditolak"
                holder.tvStatusText.setBackgroundResource(R.drawable.bg_badge_rejected)
                holder.tvHistoryDate.text = "Ditolak pada $dateStr"
            }
        }
    }

    override fun getItemCount() = historyList.size

    fun updateData(newList: List<RentalRequest>) {
        historyList = newList
        notifyDataSetChanged()
    }
}
