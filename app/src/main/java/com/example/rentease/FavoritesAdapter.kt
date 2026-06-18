package com.example.rentease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class FavoritesAdapter(
    private var items: List<Item>,
    private val onItemClick: (Item) -> Unit,
    private val onRemoveFavorite: (Item) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItemImage: ImageView = view.findViewById(R.id.iv_item_image)
        val ivPlaceholder: ImageView = view.findViewById(R.id.iv_item_placeholder)
        val tvName: TextView = view.findViewById(R.id.tv_item_name)
        val tvPrice: TextView = view.findViewById(R.id.tv_item_price)
        val tvStatus: TextView = view.findViewById(R.id.tv_item_status)
        val btnRemoveFavorite: ImageView = view.findViewById(R.id.btn_remove_favorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.name

        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0
        holder.tvPrice.text = "${formatter.format(item.price)} / hari"

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

        when (item.status) {
            Item.STATUS_AVAILABLE -> {
                holder.tvStatus.text = "Tersedia"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_btn_approve)
            }
            else -> {
                holder.tvStatus.text = "Tidak Tersedia"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_btn_reject)
            }
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnRemoveFavorite.setOnClickListener { onRemoveFavorite(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }
}
