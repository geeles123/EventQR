package com.thedavelopers.eventqr.features.attendee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.dto.EventRequestStatus
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.events.model.dto.EventRequestResponse
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MyEventRequestsActivity : AppCompatActivity() {
    private lateinit var repository: AttendeeRepository
    private lateinit var btnBack: ImageButton
    private lateinit var recyclerRequests: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressLoading: ProgressBar
    private lateinit var txtEmpty: TextView
    private lateinit var txtError: TextView
    private lateinit var btnRetry: Button
    private lateinit var adapter: MyEventRequestsAdapter

    private val eventDateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.of("Asia/Manila"))
    private val submittedDateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("MMM d, yyyy")
        .withZone(ZoneId.of("Asia/Manila"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_event_requests)
        repository = AttendeeRepository(this)

        btnBack = findViewById(R.id.btnBack)
        swipeRefresh = findViewById(R.id.swipeRefreshMyEventRequests)
        recyclerRequests = findViewById(R.id.recyclerMyEventRequests)
        progressLoading = findViewById(R.id.progressMyRequestsLoading)
        txtEmpty = findViewById(R.id.txtMyRequestsEmpty)
        txtError = findViewById(R.id.txtMyRequestsError)
        btnRetry = findViewById(R.id.btnMyRequestsRetry)

        adapter = MyEventRequestsAdapter(
            eventDateFormatter = eventDateFormatter,
            submittedDateFormatter = submittedDateFormatter,
            onTap = { request -> onRequestTapped(request) }
        )

        recyclerRequests.layoutManager = LinearLayoutManager(this)
        recyclerRequests.adapter = adapter

        btnBack.setOnClickListener { finish() }
        btnRetry.setOnClickListener { loadRequests() }
        swipeRefresh.setOnRefreshListener { loadRequests() }

        loadRequests()
    }

    private fun loadRequests() {
        showLoadingState()
        lifecycleScope.launch {
            when (val result = repository.getMyEventRequests()) {
                is NetworkResult.Success -> showDataState(result.data)
                is NetworkResult.Error -> {
                    showErrorState(result.message.ifBlank { "Unable to load event requests." })
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun showLoadingState() {
        if (!swipeRefresh.isRefreshing) {
            progressLoading.visibility = View.VISIBLE
        }
        txtEmpty.visibility = View.GONE
        txtError.visibility = View.GONE
        btnRetry.visibility = View.GONE
        recyclerRequests.visibility = View.GONE
    }

    private fun showDataState(requests: List<EventRequestResponse>) {
        swipeRefresh.isRefreshing = false
        progressLoading.visibility = View.GONE
        txtError.visibility = View.GONE
        btnRetry.visibility = View.GONE

        if (requests.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            recyclerRequests.visibility = View.GONE
            txtEmpty.text = "No event requests yet."
            return
        }

        txtEmpty.visibility = View.GONE
        recyclerRequests.visibility = View.VISIBLE
        adapter.submitItems(requests)
    }

    private fun showErrorState(message: String) {
        swipeRefresh.isRefreshing = false
        progressLoading.visibility = View.GONE
        recyclerRequests.visibility = View.GONE
        txtEmpty.visibility = View.GONE

        txtError.visibility = View.VISIBLE
        btnRetry.visibility = View.VISIBLE
        txtError.text = message
    }

    private fun onRequestTapped(request: EventRequestResponse) {
        val detailActivity = runCatching {
            Class.forName("com.thedavelopers.eventqr.features.attendee.AttendeeEventRequestDetailActivity")
        }.getOrNull()

        if (detailActivity != null) {
            startActivity(
                Intent(this, detailActivity).putExtra("extra_event_request_id", request.eventRequestId.toString())
            )
            return
        }

        showMessage("Request details screen is not available yet.")
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private class MyEventRequestsAdapter(
        private val eventDateFormatter: DateTimeFormatter,
        private val submittedDateFormatter: DateTimeFormatter,
        private val onTap: (EventRequestResponse) -> Unit,
    ) : RecyclerView.Adapter<MyEventRequestsAdapter.RequestViewHolder>() {

        private val items = mutableListOf<EventRequestResponse>()

        fun submitItems(newItems: List<EventRequestResponse>) {
            items.clear()
            items.addAll(newItems.sortedByDescending { it.createdAt ?: Instant.EPOCH })
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_my_event_request, parent, false)
            return RequestViewHolder(view)
        }

        override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
            holder.bind(items[position], eventDateFormatter, submittedDateFormatter, onTap)
        }

        override fun getItemCount(): Int = items.size

        class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val txtTitle: TextView = itemView.findViewById(R.id.txtRequestTitle)
            private val txtStatus: TextView = itemView.findViewById(R.id.txtRequestStatus)
            private val txtDescription: TextView = itemView.findViewById(R.id.txtRequestDescription)
            private val txtDate: TextView = itemView.findViewById(R.id.txtRequestDate)
            private val txtLocation: TextView = itemView.findViewById(R.id.txtRequestLocation)
            private val txtSubmitted: TextView = itemView.findViewById(R.id.txtRequestSubmitted)

            fun bind(
                request: EventRequestResponse,
                eventDateFormatter: DateTimeFormatter,
                submittedDateFormatter: DateTimeFormatter,
                onTap: (EventRequestResponse) -> Unit,
            ) {
                txtTitle.text = request.eventName.ifBlank { "Untitled Event" }
                txtDescription.text = request.eventDescription?.takeIf { it.isNotBlank() }
                    ?: "No description provided."

                txtDate.text = request.startDateTime?.let { eventDateFormatter.format(it) } ?: "-"
                txtLocation.text = request.venue?.takeIf { it.isNotBlank() } ?: "TBD"
                txtSubmitted.text = "Submitted ${request.createdAt?.let { submittedDateFormatter.format(it) } ?: "-"}"

                when (request.status) {
                    EventRequestStatus.APPROVED -> {
                        txtStatus.text = "Approved"
                        txtStatus.setBackgroundResource(R.drawable.bg_admin_approved_badge)
                        txtStatus.setTextColor(0xFF047857.toInt())
                    }

                    EventRequestStatus.PENDING -> {
                        txtStatus.text = "Pending"
                        txtStatus.setBackgroundResource(R.drawable.bg_admin_pending_badge)
                        txtStatus.setTextColor(0xFFB45309.toInt())
                    }

                    EventRequestStatus.REJECTED -> {
                        txtStatus.text = "Rejected"
                        txtStatus.setBackgroundResource(R.drawable.bg_admin_rejected_badge)
                        txtStatus.setTextColor(0xFFB91C1C.toInt())
                    }
                }

                itemView.setOnClickListener { onTap(request) }
            }
        }
    }
}
