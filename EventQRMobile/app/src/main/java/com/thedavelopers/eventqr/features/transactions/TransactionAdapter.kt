package com.thedavelopers.eventqr.features.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.features.transactions.model.dto.TransactionResponse

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private val items = mutableListOf<TransactionResponse>()

    fun submitItems(newItems: List<TransactionResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.txtTransactionTitle)
        private val detailView: TextView = itemView.findViewById(R.id.txtTransactionDetails)
        private val statusView: TextView = itemView.findViewById(R.id.txtTransactionStatus)

        fun bind(item: TransactionResponse) {
            titleView.text = item.transactionType.name.replace('_', ' ')
            detailView.text = buildString {
                append("Result: ")
                append(item.transactionResult.name)
                append("\nPoints: ")
                append(item.pointsDelta)
                append("\nScanned: ")
                append(DateFormatters.formatInstant(item.scannedAt))
                append("\nReason: ")
                append(item.reason ?: "-")
            }
            statusView.text = item.transactionResult.name
        }
    }
}