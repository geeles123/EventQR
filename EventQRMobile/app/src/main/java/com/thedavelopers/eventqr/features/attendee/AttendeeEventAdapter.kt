package com.thedavelopers.eventqr.features.attendee

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
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
        private val topStrip: View = itemView.findViewById(R.id.viewEventTopStrip)
        private val dateLayout: View = itemView.findViewById(R.id.layoutEventDate)
        private val dayView: TextView = itemView.findViewById(R.id.txtEventDay)
        private val monthView: TextView = itemView.findViewById(R.id.txtEventMonth)
        private val regCountView: TextView = itemView.findViewById(R.id.txtRegistrationCount)
        private val regPercentView: TextView = itemView.findViewById(R.id.txtRegistrationPercent)
        private val progressBar: android.widget.ProgressBar = itemView.findViewById(R.id.pbRegistration)

        fun bind(item: AttendeeEventResponse) {
            val now = Instant.now()
            val isCompleted = item.eventEndAt?.isBefore(now) == true
            val isUpcoming = item.eventStartAt?.isAfter(now) == true
            val isOngoing = !isCompleted && !isUpcoming

            titleView.text = item.title.ifBlank { "Untitled event" }
            statusView.text = when {
                isCompleted -> "Completed"
                isUpcoming -> "Upcoming"
                else -> "Active"
            }

            // Status colors and drawables
            val primaryColor = when {
                isCompleted -> 0xFF10B981.toInt() // green
                isUpcoming -> 0xFF4F46E5.toInt() // purple/blue
                else -> 0xFF06B6D4.toInt() // cyan
            }

            topStrip.setBackgroundColor(primaryColor)
            statusView.setTextColor(primaryColor)
            dayView.setTextColor(primaryColor)
            monthView.setTextColor(primaryColor)

            statusView.setBackgroundResource(when {
                isCompleted -> R.drawable.bg_event_status_completed
                isUpcoming -> R.drawable.bg_event_status_upcoming
                else -> R.drawable.bg_event_status_active
            })

            dateLayout.setBackgroundResource(when {
                isCompleted -> R.drawable.bg_event_date_completed
                isUpcoming -> R.drawable.bg_event_date_upcoming
                else -> R.drawable.bg_event_date_active
            })

            progressBar.progressDrawable = itemView.context.getDrawable(when {
                isCompleted -> R.drawable.pb_event_completed
                isUpcoming -> R.drawable.pb_event_upcoming
                else -> R.drawable.pb_event_active
            })

            // Date formatting
            item.eventStartAt?.let {
                val zonedDateTime = it.atZone(java.time.ZoneId.of("Asia/Manila"))
                dayView.text = zonedDateTime.dayOfMonth.toString()
                monthView.text = zonedDateTime.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH).uppercase()
                
                val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH)
                dateTimeView.text = zonedDateTime.format(timeFormatter)
            }

            locationView.text = item.location?.takeIf { it.isNotBlank() } ?: "Location not set"

            // Registration progress
            val capacity = item.capacity.coerceAtLeast(1)
            val current = item.currentAttendeeCount
            val percent = (current.toFloat() / capacity.toFloat() * 100).toInt().coerceIn(0, 100)

            regCountView.text = "$current / $capacity registered"
            regPercentView.text = "$percent%"
            progressBar.progress = percent

            itemView.setOnClickListener { onClick(item) }
        }
    }
}
