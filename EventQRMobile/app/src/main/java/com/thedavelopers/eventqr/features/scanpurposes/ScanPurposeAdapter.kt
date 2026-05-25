package com.thedavelopers.eventqr.features.scanpurposes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.scanpurposes.model.dto.ScanPurposeResponse

class ScanPurposeAdapter : RecyclerView.Adapter<ScanPurposeAdapter.ViewHolder>() {

    private val items = mutableListOf<ScanPurposeResponse>()

    fun submitItems(newItems: List<ScanPurposeResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scan_purpose, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.txtScanPurposeTitle)
        private val detailView: TextView = itemView.findViewById(R.id.txtScanPurposeDetails)
        private val statusView: TextView = itemView.findViewById(R.id.txtScanPurposeStatus)

        fun bind(item: ScanPurposeResponse) {
            titleView.text = item.name
            detailView.text = buildString {
                append(item.code.name.replace('_', ' '))
                append("\nTracking only: ")
                append(item.trackingOnly)
                append("\nDescription: ")
                append(item.description ?: "-")
            }
            statusView.text = if (item.active) "Active" else "Inactive"
        }
    }
}