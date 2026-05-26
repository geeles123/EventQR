package com.thedavelopers.eventqr.features.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.dto.EventStatus
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.features.events.model.dto.EventResponse

class EventAdapter(
    private val onClick: (EventResponse) -> Unit,
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val items = mutableListOf<EventResponse>()

    fun submitItems(newItems: List<EventResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.txtEventTitle)
        private val detailView: TextView = itemView.findViewById(R.id.txtEventDetails)
        private val statusView: TextView = itemView.findViewById(R.id.txtEventStatus)

        fun bind(item: EventResponse) {
            titleView.text = item.title
            detailView.text = buildString {
                append(item.location ?: "Location not set")
                append("\n")
                append("Starts: ")
                append(DateFormatters.formatInstant(item.eventStartAt))
                append("\nEnds: ")
                append(DateFormatters.formatInstant(item.eventEndAt))
                append("\nCapacity: ")
                append(item.currentAttendeeCount)
                append("/")
                append(item.capacity)
            }
            statusView.text = item.status.name.replace('_', ' ')
            statusView.setBackgroundResource(
                if (item.status == EventStatus.ACTIVE || item.status == EventStatus.APPROVED) {
                    R.drawable.button_rounded
                } else {
                    R.drawable.transparent_alt
                }
            )
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
