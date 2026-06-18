package com.example.rentease

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ChatMessageAdapter(
    private var messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatMessageAdapter.ViewHolder>() {

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.message_container)
        val bubbleContainer: LinearLayout = view.findViewById(R.id.bubble_container)
        val tvSenderName: TextView = view.findViewById(R.id.tv_sender_name)
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]

        val isMine = msg.senderId == currentUid

        holder.tvSenderName.text = if (isMine) "Anda" else msg.senderName
        holder.tvMessage.text = msg.message

        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID"))
        holder.tvTime.text = sdf.format(java.util.Date(msg.timestamp))

        if (isMine) {
            holder.container.gravity = Gravity.END
            holder.bubbleContainer.setBackgroundResource(R.drawable.bg_btn_approve)
        } else {
            holder.container.gravity = Gravity.START
            holder.bubbleContainer.setBackgroundResource(R.drawable.bg_stat_card_user)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateData(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
