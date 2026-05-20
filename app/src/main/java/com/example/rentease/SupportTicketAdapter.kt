package com.example.rentease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupportTicketAdapter(
    private var ticketList: List<SupportTicket>,
    private val onItemClick: (SupportTicket) -> Unit
) : RecyclerView.Adapter<SupportTicketAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStatus: TextView = view.findViewById(R.id.tv_ticket_status)
        val tvDate: TextView = view.findViewById(R.id.tv_ticket_date)
        val tvSubject: TextView = view.findViewById(R.id.tv_ticket_subject)
        val tvUserName: TextView = view.findViewById(R.id.tv_user_name)
        val tvMessage: TextView = view.findViewById(R.id.tv_ticket_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_support_ticket, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ticket = ticketList[position]
        
        holder.tvSubject.text = ticket.subject
        holder.tvUserName.text = "Dari: ${ticket.userName}"
        holder.tvMessage.text = ticket.message
        
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
        holder.tvDate.text = sdf.format(Date(ticket.createdAt))
        
        holder.tvStatus.text = ticket.status.uppercase()
        when (ticket.status) {
            SupportTicket.STATUS_OPEN -> holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            SupportTicket.STATUS_IN_PROGRESS -> holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_approved)
            SupportTicket.STATUS_RESOLVED -> holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_returned)
        }
        
        holder.itemView.setOnClickListener { onItemClick(ticket) }
    }

    override fun getItemCount() = ticketList.size

    fun updateData(newList: List<SupportTicket>) {
        ticketList = newList
        notifyDataSetChanged()
    }
}
