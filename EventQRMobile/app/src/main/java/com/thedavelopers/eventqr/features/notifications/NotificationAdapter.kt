package com.thedavelopers.eventqr.features.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.features.notifications.model.dto.NotificationResponse

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private val items = mutableListOf<NotificationResponse>()

    fun submitItems(newItems: List<NotificationResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.txtNotificationTitle)
        private val detailView: TextView = itemView.findViewById(R.id.txtNotificationDetails)
        private val statusView: TextView = itemView.findViewById(R.id.txtNotificationStatus)

        fun bind(item: NotificationResponse) {
            titleView.text = item.title
            detailView.text = buildString {
                append(item.message)
                append("\nRecipient: ")
                append(item.recipientUserId)
                append("\nRead: ")
                append(DateFormatters.formatInstant(item.readAt))
            }
            statusView.text = item.status.name
        }
    }
}