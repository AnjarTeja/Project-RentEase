package com.example.rentease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserSupportTicketAdapter(
    private var ticketList: List<SupportTicket>
) : RecyclerView.Adapter<UserSupportTicketAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubject: TextView = view.findViewById(R.id.tv_ticket_subject)
        val tvDate: TextView = view.findViewById(R.id.tv_ticket_date)
        val tvMessage: TextView = view.findViewById(R.id.tv_ticket_message)
        val tvStatus: TextView = view.findViewById(R.id.tv_ticket_status)
        val layoutReply: LinearLayout = view.findViewById(R.id.layout_reply)
        val tvReplyMessage: TextView = view.findViewById(R.id.tv_reply_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_support_ticket, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ticket = ticketList[position]
        
        holder.tvSubject.text = ticket.subject
        holder.tvMessage.text = ticket.message
        
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
        holder.tvDate.text = sdf.format(Date(ticket.createdAt))
        
        holder.tvStatus.text = ticket.status.uppercase()
        when (ticket.status) {
            SupportTicket.STATUS_OPEN -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
                holder.layoutReply.visibility = View.GONE
            }
            SupportTicket.STATUS_RESOLVED -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_returned)
                holder.layoutReply.visibility = View.VISIBLE
                holder.tvReplyMessage.text = ticket.replyMessage
            }
        }
    }

    override fun getItemCount() = ticketList.size

    fun updateData(newList: List<SupportTicket>) {
        ticketList = newList
        notifyDataSetChanged()
    }
}
