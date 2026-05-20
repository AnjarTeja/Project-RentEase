package com.example.rentease

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ItemAdapter(
    private val context: Context,
    private var itemList: List<Item>,
    private val onEditClick: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
        val tvItemPrice: TextView = view.findViewById(R.id.tv_item_price)
        val tvItemStatus: TextView = view.findViewById(R.id.tv_item_status)
        val ivPhoto: ImageView = view.findViewById(R.id.iv_item_photo)
        val ivPlaceholder: ImageView = view.findViewById(R.id.iv_photo_placeholder)
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit_item)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]

        holder.tvItemName.text = item.name.ifEmpty { "Barang Tanpa Nama" }

        // Format price to IDR
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        val priceString = format.format(item.price)
        holder.tvItemPrice.text = "$priceString / hari"

        // Status styling
        when (item.status) {
            Item.STATUS_AVAILABLE -> {
                holder.tvItemStatus.text = "Tersedia"
                holder.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.success_color))
            }
            Item.STATUS_RENTED -> {
                holder.tvItemStatus.text = "Disewa"
                holder.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_color))
            }
            Item.STATUS_MAINTENANCE -> {
                holder.tvItemStatus.text = "Perbaikan"
                holder.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.error_color))
            }
            else -> {
                holder.tvItemStatus.text = item.status
                holder.tvItemStatus.setTextColor(Color.WHITE)
            }
        }

        // Handle Image using Glide
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(item.imageUrl)
                .placeholder(R.drawable.bg_card_dark)
                .error(R.drawable.bg_card_dark)
                .into(holder.ivPhoto)
            holder.ivPlaceholder.visibility = View.GONE
        } else {
            holder.ivPhoto.setImageDrawable(null)
            holder.ivPlaceholder.visibility = View.VISIBLE
        }

        // Actions
        holder.btnEdit.setOnClickListener { onEditClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = itemList.size

    fun updateData(newList: List<Item>) {
        itemList = newList
        notifyDataSetChanged()
    }
}
