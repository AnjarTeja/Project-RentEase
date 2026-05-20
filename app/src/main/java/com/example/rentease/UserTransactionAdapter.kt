package com.example.rentease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class UserTransactionAdapter(
    private var transactions: List<RentalRequest>,
    private val onReturnClick: (RentalRequest) -> Unit
) : RecyclerView.Adapter<UserTransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
        val tvStatusBadge: TextView = view.findViewById(R.id.tv_status_badge)
        val tvDurationDate: TextView = view.findViewById(R.id.tv_duration_date)
        val btnReturn: Button = view.findViewById(R.id.btn_return_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tx = transactions[position]

        holder.tvItemName.text = tx.itemName
        holder.tvDurationDate.text = "Durasi: ${tx.duration} Hari • Mulai: ${tx.startDate}"

        val context = holder.itemView.context

        when (tx.status) {
            RentalRequest.STATUS_PENDING -> {
                holder.tvStatusBadge.text = "Menunggu"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.btnReturn.visibility = View.GONE
            }
            RentalRequest.STATUS_APPROVED -> {
                holder.tvStatusBadge.text = "Aktif (Disewa)"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_approved)
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.btnReturn.visibility = View.VISIBLE
            }
            RentalRequest.STATUS_RETURNED -> {
                holder.tvStatusBadge.text = "Selesai"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_returned)
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.btnReturn.visibility = View.GONE
            }
            RentalRequest.STATUS_REJECTED -> {
                holder.tvStatusBadge.text = "Ditolak"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rejected)
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.btnReturn.visibility = View.GONE
            }
            "return_pending" -> {
                holder.tvStatusBadge.text = "Proses Kembali"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.btnReturn.visibility = View.GONE
            }
        }

        holder.btnReturn.setOnClickListener {
            onReturnClick(tx)
        }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<RentalRequest>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}
