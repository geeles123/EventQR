package com.thedavelopers.eventqr.features.attendee

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.features.events.model.dto.AttendeeEventResponse
import java.time.Instant

class AttendeeEventAdapter(
    private val onClick: (AttendeeEventResponse) -> Unit,
) : RecyclerView.Adapter<AttendeeEventAdapter.ViewHolder>() {

    private val items = mutableListOf<AttendeeEventResponse>()

    fun submitItems(newItems: List<AttendeeEventResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendee_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.txtAttendeeEventTitle)
        private val statusView: TextView = itemView.findViewById(R.id.txtAttendeeEventStatus)
        private val dateTimeView: TextView = itemView.findViewById(R.id.txtAttendeeEventDateTime)
        private val locationView: TextView = itemView.findViewById(R.id.txtAttendeeEventLocation)
        private val rewardsView: TextView = itemView.findViewById(R.id.txtAttendeeEventRewards)

        fun bind(item: AttendeeEventResponse) {
            titleView.text = item.title.ifBlank { "Untitled event" }
            statusView.text = statusLabel(item)
            statusView.setBackgroundResource(if (isPast(item)) R.drawable.bg_teal_pill else R.drawable.bg_soft_gray_pill)
            statusView.setTextColor(if (isPast(item)) 0xFF0F766E.toInt() else 0xFF000000.toInt())
            dateTimeView.text = "▣    ${DateFormatters.formatInstant(item.eventStartAt)}"
            locationView.text = "⌖    ${item.location?.takeIf { it.isNotBlank() } ?: "Location not set"}"
            rewardsView.visibility = View.GONE
            itemView.setOnClickListener { onClick(item) }
        }
    }

    private fun statusLabel(item: AttendeeEventResponse): String {
        val now = Instant.now()
        return when {
            item.eventEndAt?.isBefore(now) == true -> "Completed"
            item.eventStartAt?.isAfter(now) == true -> "Upcoming"
            item.eventStartAt != null && item.eventEndAt != null &&
                !item.eventStartAt.isAfter(now) && !item.eventEndAt.isBefore(now) -> "Ongoing"
            else -> "Scheduled"
        }
    }

    private fun isPast(item: AttendeeEventResponse): Boolean {
        val now = Instant.now()
        return item.eventEndAt?.isBefore(now) == true
    }
}
