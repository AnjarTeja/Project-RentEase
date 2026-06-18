package com.example.rentease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ChatListAdapter(
    private var chats: List<Chat>,
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvContactName: TextView = view.findViewById(R.id.tv_contact_name)
        val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
        val tvLastMessage: TextView = view.findViewById(R.id.tv_last_message)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]

        val contactName = if (chat.renterId == currentUid) chat.ownerName else chat.renterName
        holder.tvContactName.text = contactName
        holder.tvItemName.text = chat.itemName
        holder.tvLastMessage.text = chat.lastMessage

        if (chat.lastMessageTime > 0) {
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale("id", "ID"))
            holder.tvTime.text = sdf.format(java.util.Date(chat.lastMessageTime))
        } else {
            holder.tvTime.text = ""
        }

        holder.itemView.setOnClickListener { onChatClick(chat) }
    }

    override fun getItemCount(): Int = chats.size

    fun updateData(newChats: List<Chat>) {
        chats = newChats
        notifyDataSetChanged()
    }
}
