package com.thedavelopers.eventqr.features.organizer.attendees

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.organizer.AttendeeManagementAdapter
import com.thedavelopers.eventqr.features.organizer.EXTRA_EVENT_ID
import com.thedavelopers.eventqr.features.organizer.EXTRA_EVENT_TITLE
import com.thedavelopers.eventqr.features.organizer.NAV_ATTENDEES
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpAttendee
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpDataSource
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpEvent
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpLoad
import com.thedavelopers.eventqr.features.organizer.OrganizerRepository
import com.thedavelopers.eventqr.features.organizer.bottomNav
import com.thedavelopers.eventqr.features.organizer.eventSelector
import com.thedavelopers.eventqr.features.organizer.intentEventId
import com.thedavelopers.eventqr.features.organizer.organizerEventDateLine
import com.thedavelopers.eventqr.features.organizer.resolveSelectedEvent
import com.thedavelopers.eventqr.features.organizer.saveSelectedEventId
import com.thedavelopers.eventqr.features.organizer.selectedEventId
import com.thedavelopers.eventqr.features.organizer.statusBucket
import com.thedavelopers.eventqr.features.organizer.showMissingEventScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

open class AttendeeManagementActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var adapter: AttendeeManagementAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var txtTotal: TextView
    private lateinit var txtCheckedIn: TextView
    private lateinit var txtNoShow: TextView
    private lateinit var txtBannerTitle: TextView
    private lateinit var txtBannerDate: TextView
    private lateinit var eventSelectorHost: LinearLayout
    private lateinit var bottomNavHost: LinearLayout

    private var attendees: List<OrganizerMvpAttendee> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendee_management)

        repository = OrganizerRepository(this)
        val eventId = intentEventId() ?: selectedEventId().takeIf { it.isNotBlank() }
            ?: return showMissingEventScreen("Attendee Management")
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents(), eventId)
            ?: return showMissingEventScreen("Attendee Management")

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnFilter).setOnClickListener {
            startActivity(
                Intent(this, SearchAttendeesActivity::class.java)
                    .putExtra(EXTRA_EVENT_ID, selectedEvent.id)
                    .putExtra(EXTRA_EVENT_TITLE, selectedEvent.title)
            )
        }

        txtBannerTitle = findViewById(R.id.txtEventMiniTitle)
        txtBannerDate = findViewById(R.id.txtEventMiniDate)
        txtTotal = findViewById(R.id.txtTotalCount)
        txtCheckedIn = findViewById(R.id.txtCheckedInCount)
        txtNoShow = findViewById(R.id.txtNoShowCount)
        progressBar = findViewById(R.id.progressAttendees)
        emptyState = findViewById(R.id.txtAttendeesEmpty)
        eventSelectorHost = findViewById(R.id.layoutEventSelectorHost)
        bottomNavHost = findViewById(R.id.layoutBottomNavHost)

        val spinner: Spinner = eventSelector(repository.getApprovedOrganizerEvents(), selectedEvent.id) { event ->
            selectedEvent = event
            repository.saveSelectedEventId(event.id)
            saveSelectedEventId(event.id)
            bindEventHeader()
            loadAttendees()
        }
        eventSelectorHost.addView(spinner)

        adapter = AttendeeManagementAdapter { attendee -> openDetails(attendee) }
        findViewById<RecyclerView>(R.id.recyclerAttendees).apply {
            layoutManager = LinearLayoutManager(this@AttendeeManagementActivity)
            adapter = this@AttendeeManagementActivity.adapter
        }

        bottomNavHost.addView(bottomNav(NAV_ATTENDEES))
        bindEventHeader()
        loadAttendees()
    }

    private fun loadAttendees() {
        progressBar.visibility = View.VISIBLE
        MainScope().launch {
            val load = repository.loadAttendeesForMvp(selectedEvent.id)
            attendees = load.data
            progressBar.visibility = View.GONE
            render(load)
        }
    }

    private fun bindEventHeader() {
        findViewById<TextView>(R.id.txtManagementEventName).text = selectedEvent.title
        txtBannerTitle.text = selectedEvent.title
        txtBannerDate.text = organizerEventDateLine(selectedEvent.shortDate, selectedEvent.title, selectedEvent.venue)
    }

    private fun render(load: OrganizerMvpLoad<List<OrganizerMvpAttendee>>) {
        val checkedIn = attendees.count { it.statusBucket().equals("Checked In", ignoreCase = true) }
        val noShow = attendees.count { it.statusBucket().equals("No Show", ignoreCase = true) }

        txtTotal.text = attendees.size.toString()
        txtCheckedIn.text = checkedIn.toString()
        txtNoShow.text = noShow.toString()

        adapter.submitItems(attendees)
        emptyState.visibility = if (attendees.isEmpty()) View.VISIBLE else View.GONE
        emptyState.text = when {
            load.source == OrganizerMvpDataSource.ERROR -> load.message ?: "Attendees could not be loaded."
            attendees.isEmpty() -> "No attendees registered yet."
            else -> ""
        }
    }

    private fun openDetails(attendee: OrganizerMvpAttendee) {
        startActivity(
            Intent(this, AttendeeDetailsActivity::class.java)
                .putExtra(EXTRA_EVENT_ID, selectedEvent.id)
                .putExtra(EXTRA_EVENT_TITLE, selectedEvent.title)
                .putExtra(SearchAttendeesActivity.EXTRA_ATTENDEE_ID, attendee.id)
        )
    }
}