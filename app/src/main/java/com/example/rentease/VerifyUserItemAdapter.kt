package com.example.rentease

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class VerifyUserItemAdapter(
    private var itemList: List<Item>,
    private val onApproveClick: (Item) -> Unit,
    private val onRejectClick: (Item) -> Unit
) : RecyclerView.Adapter<VerifyUserItemAdapter.ViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
        val tvItemPrice: TextView = view.findViewById(R.id.tv_item_price)
        val tvOwnerName: TextView = view.findViewById(R.id.tv_owner_name)
        val ivPhoto: ImageView = view.findViewById(R.id.iv_item_photo)
        val ivPlaceholder: ImageView = view.findViewById(R.id.iv_photo_placeholder)
        val btnApprove: TextView = view.findViewById(R.id.btn_approve)
        val btnReject: TextView = view.findViewById(R.id.btn_reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verify_user_item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]

        holder.tvItemName.text = item.name.ifEmpty { "Barang Tanpa Nama" }

        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        holder.tvItemPrice.text = "${format.format(item.price)} / hari"

        // Handle Image
        if (item.imageUrl.isNotEmpty()) {
            try {
                holder.ivPhoto.setImageURI(Uri.parse(item.imageUrl))
                holder.ivPlaceholder.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
                holder.ivPhoto.setImageDrawable(null)
                holder.ivPlaceholder.visibility = View.VISIBLE
            }
        } else {
            holder.ivPhoto.setImageDrawable(null)
            holder.ivPlaceholder.visibility = View.VISIBLE
        }

        // Fetch Owner Name
        holder.tvOwnerName.text = "Memuat nama pengunggah..."
        if (item.ownerId.isNotEmpty()) {
            firestore.collection("users").document(item.ownerId).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "Pengguna Tidak Dikenal"
                    holder.tvOwnerName.text = "Di-upload oleh: $name"
                }
                .addOnFailureListener {
                    holder.tvOwnerName.text = "Di-upload oleh: User ID ${item.ownerId.take(5)}..."
                }
        } else {
            holder.tvOwnerName.text = "Pengunggah tidak diketahui"
        }

        holder.btnApprove.setOnClickListener { onApproveClick(item) }
        holder.btnReject.setOnClickListener { onRejectClick(item) }
    }

    override fun getItemCount(): Int = itemList.size

    fun updateData(newList: List<Item>) {
        itemList = newList
        notifyDataSetChanged()
    }
}
