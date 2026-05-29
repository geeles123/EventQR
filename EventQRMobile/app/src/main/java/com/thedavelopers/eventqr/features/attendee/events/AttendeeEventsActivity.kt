package com.thedavelopers.eventqr.features.attendee

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.features.events.model.dto.AttendeeEventResponse
import java.time.Instant

open class AttendeeEventsActivity : AppCompatActivity(), EventsContract.View {
    private lateinit var presenter: EventsPresenter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var loadingView: TextView
    private lateinit var retryButton: Button
    private lateinit var allTab: TextView
    private lateinit var upcomingTab: TextView
    private lateinit var activeTab: TextView
    private lateinit var pastTab: TextView
    private lateinit var adapter: AttendeeEventAdapter
    private var allEvents: List<AttendeeEventResponse> = emptyList()
    private var selectedFilter: EventFilter = EventFilter.ALL

    enum class EventFilter { ALL, UPCOMING, ACTIVE, COMPLETED }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_events)
        configureAttendeeBottomNav(AttendeeBottomNavItem.EVENTS)

        presenter = EventsPresenter(this, AttendeeRepository(this))
        recyclerView = findViewById(R.id.recyclerEvents)
        emptyView = findViewById(R.id.txtEventsEmpty)
        loadingView = findViewById(R.id.txtEventsLoading)
        retryButton = findViewById(R.id.btnRefreshEvents)
        allTab = findViewById(R.id.tabEventsAll)
        upcomingTab = findViewById(R.id.tabEventsUpcoming)
        activeTab = findViewById(R.id.tabEventsActive)
        pastTab = findViewById(R.id.tabEventsPast)
        adapter = AttendeeEventAdapter { event -> openEventDetail(event) }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        retryButton.setOnClickListener { presenter.loadEvents() }
        allTab.setOnClickListener { selectFilter(EventFilter.ALL) }
        upcomingTab.setOnClickListener { selectFilter(EventFilter.UPCOMING) }
        activeTab.setOnClickListener { selectFilter(EventFilter.ACTIVE) }
        pastTab.setOnClickListener { selectFilter(EventFilter.COMPLETED) }
        updateTabs()
        presenter.loadEvents()

        findViewById<android.widget.EditText>(R.id.inputEventSearch).addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterBySearch(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filterBySearch(query: String) {
        val filtered = if (query.isBlank()) {
            getFilteredByStatus()
        } else {
            getFilteredByStatus().filter { it.title.contains(query, ignoreCase = true) }
        }
        adapter.submitItems(filtered)
        recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun getFilteredByStatus(): List<AttendeeEventResponse> {
        return when (selectedFilter) {
            EventFilter.ALL -> sortAll(allEvents)
            EventFilter.UPCOMING -> allEvents.filter { it.eventStartAt?.isAfter(Instant.now()) == true }.sortedBy { it.eventStartAt }
            EventFilter.ACTIVE -> allEvents.filter { isOngoingEvent(it) }.sortedBy { it.eventStartAt }
            EventFilter.COMPLETED -> allEvents.filter { isPastEvent(it) }.sortedByDescending { it.eventEndAt }
        }
    }

    private fun isOngoingEvent(item: AttendeeEventResponse): Boolean {
        val now = Instant.now()
        return item.eventStartAt != null && item.eventEndAt != null &&
                !item.eventStartAt.isAfter(now) && !item.eventEndAt.isBefore(now)
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    private fun openEventDetail(event: AttendeeEventResponse) {
        startActivity(
            Intent(this, EventDetailActivity::class.java)
                .putExtra(EXTRA_EVENT_ID, event.eventId.toString())
                .putExtra(EXTRA_EVENT_TITLE, event.title)
                .putExtra(EXTRA_EVENT_LOCATION, event.location ?: "")
                .putExtra(EXTRA_EVENT_DESCRIPTION, event.description ?: "")
                .putExtra(EXTRA_EVENT_CATEGORY, event.category ?: "")
                .putExtra(EXTRA_EVENT_START, DateFormatters.formatInstant(event.eventStartAt))
                .putExtra(EXTRA_EVENT_END, DateFormatters.formatInstant(event.eventEndAt))
                .putExtra(EXTRA_EVENT_STATUS, computedStatusLabel(event))
                .putExtra(EXTRA_EVENT_CAPACITY, event.capacity.toString())
                .putExtra(EXTRA_EVENT_COUNT, event.currentAttendeeCount.toString())
        )
    }

    override fun showLoading(isLoading: Boolean) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            emptyView.visibility = View.GONE
            retryButton.visibility = View.GONE
            recyclerView.visibility = View.GONE
        }
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showEvents(items: List<AttendeeEventResponse>) {
        allEvents = items
        retryButton.visibility = View.GONE
        renderFilteredEvents()
    }

    override fun showError(message: String) {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.text = when (selectedFilter) {
            EventFilter.ALL -> "No events available yet."
            EventFilter.UPCOMING -> "No upcoming events yet."
            EventFilter.ACTIVE -> "No active events yet."
            EventFilter.COMPLETED -> "No completed events yet."
        }
        retryButton.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun selectFilter(filter: EventFilter) {
        selectedFilter = filter
        updateTabs()
        renderFilteredEvents()
    }

    private fun updateTabs() {
        val selectedBg = R.drawable.bg_event_filter_chip_selected
        val unselectedBg = R.drawable.bg_event_filter_chip_unselected
        val white = 0xFFFFFFFF.toInt()
        val gray = 0xFF6B7280.toInt()

        allTab.setBackgroundResource(if (selectedFilter == EventFilter.ALL) selectedBg else unselectedBg)
        allTab.setTextColor(if (selectedFilter == EventFilter.ALL) white else gray)

        upcomingTab.setBackgroundResource(if (selectedFilter == EventFilter.UPCOMING) selectedBg else unselectedBg)
        upcomingTab.setTextColor(if (selectedFilter == EventFilter.UPCOMING) white else gray)

        activeTab.setBackgroundResource(if (selectedFilter == EventFilter.ACTIVE) selectedBg else unselectedBg)
        activeTab.setTextColor(if (selectedFilter == EventFilter.ACTIVE) white else gray)

        pastTab.setBackgroundResource(if (selectedFilter == EventFilter.COMPLETED) selectedBg else unselectedBg)
        pastTab.setTextColor(if (selectedFilter == EventFilter.COMPLETED) white else gray)
    }

    private fun renderFilteredEvents() {
        val filtered = getFilteredByStatus()
        adapter.submitItems(filtered)
        recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        emptyView.text = when (selectedFilter) {
            EventFilter.ALL -> "No events available yet."
            EventFilter.UPCOMING -> "No upcoming events yet."
            EventFilter.ACTIVE -> "No active events yet."
            EventFilter.COMPLETED -> "No completed events yet."
        }
    }

    private fun sortAll(items: List<AttendeeEventResponse>): List<AttendeeEventResponse> {
        val ongoing = items.filter { isOngoingEvent(it) }.sortedBy { it.eventStartAt }
        val upcoming = items.filter { it.eventStartAt?.isAfter(Instant.now()) == true }.sortedBy { it.eventStartAt }
        val completed = items.filter { isPastEvent(it) }.sortedByDescending { it.eventEndAt }
        return ongoing + upcoming + completed
    }

    private fun isPastEvent(item: AttendeeEventResponse): Boolean {
        val now = Instant.now()
        return item.eventEndAt?.isBefore(now) == true
    }
}
