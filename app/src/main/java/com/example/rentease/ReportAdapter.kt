package com.example.rentease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReportAdapter(
    private var reportList: List<ReportItem>
) : RecyclerView.Adapter<ReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_report_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_report_subtitle)
        val tvDate: TextView = view.findViewById(R.id.tv_report_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = reportList[position]
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle
        holder.tvDate.text = item.dateStr
    }

    override fun getItemCount(): Int = reportList.size

    fun updateData(newList: List<ReportItem>) {
        reportList = newList
        notifyDataSetChanged()
    }
}
