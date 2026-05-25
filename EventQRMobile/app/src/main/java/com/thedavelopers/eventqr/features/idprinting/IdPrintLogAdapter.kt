package com.thedavelopers.eventqr.features.idprinting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.features.idprinting.model.dto.IdPrintResponse

class IdPrintLogAdapter : RecyclerView.Adapter<IdPrintLogAdapter.ViewHolder>() {

    private val items = mutableListOf<IdPrintResponse>()

    fun submitItems(newItems: List<IdPrintResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_id_print_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.txtIdPrintLogTitle)
        private val detailView: TextView = itemView.findViewById(R.id.txtIdPrintLogDetails)
        private val statusView: TextView = itemView.findViewById(R.id.txtIdPrintLogStatus)

        fun bind(item: IdPrintResponse) {
            titleView.text = if (item.reprint) "Reprint" else "Print"
            detailView.text = buildString {
                append(item.message)
                append("\nPrinted: ")
                append(DateFormatters.formatInstant(item.printedAt))
                append("\nTemplate: ")
                append(item.templateId)
            }
            statusView.text = if (item.success) "Success" else "Failed"
        }
    }
}