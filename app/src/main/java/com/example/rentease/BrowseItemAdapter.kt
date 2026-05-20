package com.example.rentease

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class BrowseItemAdapter(
    private var items: List<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<BrowseItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItemImage: ImageView = view.findViewById(R.id.iv_item_image)
        val ivPlaceholder: ImageView = view.findViewById(R.id.iv_item_placeholder)
        val tvName: TextView = view.findViewById(R.id.tv_item_name)
        val tvPrice: TextView = view.findViewById(R.id.tv_item_price)
        val tvOwner: TextView = view.findViewById(R.id.tv_item_owner)
        val tvItemRentCount: TextView = view.findViewById(R.id.tv_item_rent_count)
        val tvStatus: TextView = view.findViewById(R.id.tv_item_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_browse_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.name

        // Format price
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0
        holder.tvPrice.text = "${formatter.format(item.price)} / hari"

        // Load image using Glide
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.bg_card_dark)
                .error(R.drawable.bg_card_dark)
                .into(holder.ivItemImage)
            holder.ivPlaceholder.visibility = View.GONE
        } else {
            holder.ivItemImage.setImageDrawable(null)
            holder.ivPlaceholder.visibility = View.VISIBLE
        }

        // Status badge
        when (item.status) {
            Item.STATUS_AVAILABLE -> {
                holder.tvStatus.text = "Tersedia"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_btn_approve)
            }
            Item.STATUS_RENTED -> {
                holder.tvStatus.text = "Disewa"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_btn_reject)
            }
            else -> {
                holder.tvStatus.text = "Tersedia"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_btn_approve)
            }
        }

        // Owner info (will be set from activity after fetching owner names)
        holder.tvOwner.text = item.ownerId

        if (item.rentCount > 0) {
            holder.tvItemRentCount.visibility = View.VISIBLE
            holder.tvItemRentCount.text = "${item.rentCount}x Disewa"
        } else {
            holder.tvItemRentCount.visibility = View.GONE
        }

        // Click listener
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }
}
