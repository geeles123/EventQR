package com.thedavelopers.eventqr.features.attendee

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.transactions.TransactionAdapter
import com.thedavelopers.eventqr.features.transactions.model.dto.TransactionResponse
import java.time.Instant
import java.util.UUID

open class AttendeeTransactionsActivity : AppCompatActivity(), TransactionHistoryContract.View {
    private lateinit var presenter: TransactionHistoryPresenter
    private lateinit var adapter: TransactionAdapter
    private lateinit var loadingView: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var errorText: TextView
    private lateinit var retryButton: Button
    private lateinit var filterSpinner: Spinner
    private lateinit var summaryCountText: TextView
    private lateinit var summaryNetText: TextView
    private lateinit var recyclerView: RecyclerView

    private var allTransactions: List<TransactionResponse> = emptyList()
    private var eventFilterOptions: List<Pair<String?, String>> = emptyList()
    private var selectedEventId: String? = null
    private var pendingInitialEventId: String? = null
    private val isDebuggableBuild: Boolean by lazy {
        (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_transaction_history)
        configureAttendeeBottomNav(AttendeeBottomNavItem.PROFILE)

        presenter = TransactionHistoryPresenter(this, AttendeeRepository(this))

        loadingView = findViewById(R.id.progressTransactionsLoading)
        emptyText = findViewById(R.id.txtTransactionsEmpty)
        errorText = findViewById(R.id.txtTransactionsError)
        retryButton = findViewById(R.id.btnTransactionsRetry)
        filterSpinner = findViewById(R.id.spinnerEventFilter)
        summaryCountText = findViewById(R.id.txtHistoryTransactionCount)
        summaryNetText = findViewById(R.id.txtHistoryNetPoints)
        recyclerView = findViewById(R.id.recyclerTransactions)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        retryButton.setOnClickListener { presenter.load(null) }

        pendingInitialEventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty().ifBlank { null }

        adapter = TransactionAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AttendeeTransactionsActivity)
            adapter = this@AttendeeTransactionsActivity.adapter
        }

        presenter.load(null)
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun showLoading(isLoading: Boolean) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            retryButton.visibility = View.GONE
            errorText.visibility = View.GONE
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.GONE
        }
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        loadingView.visibility = View.GONE

        if (isDebuggableBuild) {
            errorText.text = "Unable to load live transactions. Showing sample data for development."
            errorText.visibility = View.VISIBLE
            retryButton.visibility = View.VISIBLE
            renderTransactions(sampleFallbackTransactions())
            return
        }

        summaryCountText.text = "0 transactions"
        summaryNetText.text = "+0 pts net"
        summaryNetText.setTextColor(getColor(R.color.eventqr_success))
        errorText.text = message.ifBlank { "Unable to load transactions." }
        errorText.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        recyclerView.visibility = View.GONE
    }

    override fun renderTransactions(items: List<TransactionResponse>) {
        loadingView.visibility = View.GONE
        retryButton.visibility = View.GONE
        errorText.visibility = View.GONE
        allTransactions = items
        updateFilterOptions(items)
        applySelectedFilter()
    }

    private fun updateFilterOptions(items: List<TransactionResponse>) {
        val groupedEvents = items
            .groupBy { it.eventId.toString() }
            .toSortedMap(compareBy<String> { key ->
                items.firstOrNull { it.eventId.toString() == key }?.eventTitle.orEmpty().lowercase()
            })

        val options = mutableListOf<Pair<String?, String>>()
        options += null to "All Events"
        groupedEvents.forEach { (eventId, eventItems) ->
            val title = eventItems.firstOrNull()?.eventTitle?.takeIf { it.isNotBlank() } ?: "Event"
            options += eventId to title
        }
        eventFilterOptions = options

        val labels = options.map { it.second }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        filterSpinner.adapter = spinnerAdapter

        val initialEventId = pendingInitialEventId
        val initialIndex = if (initialEventId == null) 0 else options.indexOfFirst { it.first == initialEventId }.coerceAtLeast(0)
        selectedEventId = options[initialIndex].first
        pendingInitialEventId = null
        filterSpinner.setSelection(initialIndex, false)

        filterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedEventId = eventFilterOptions.getOrNull(position)?.first
                applySelectedFilter()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun applySelectedFilter() {
        val filtered = if (selectedEventId.isNullOrBlank()) {
            allTransactions
        } else {
            allTransactions.filter { it.eventId.toString() == selectedEventId }
        }

        adapter.submitItems(filtered)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        emptyText.text = "No transactions found for the selected event."

        val netPoints = filtered.sumOf { it.pointsDelta }
        summaryCountText.text = if (filtered.size == 1) "1 transaction" else "${filtered.size} transactions"
        val netPrefix = if (netPoints >= 0) "+" else ""
        summaryNetText.text = "$netPrefix$netPoints pts net"
        summaryNetText.setTextColor(
            if (netPoints >= 0) getColor(R.color.eventqr_success) else getColor(R.color.eventqr_error)
        )
    }

    private fun sampleFallbackTransactions(): List<TransactionResponse> {
        val attendeeId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val qrCredentialId = UUID.randomUUID()
        val eventIdA = UUID.randomUUID()
        val eventIdB = UUID.randomUUID()

        return listOf(
            TransactionResponse(
                transactionId = UUID.randomUUID(),
                eventId = eventIdA,
                eventTitle = "UI/UX Design Conference",
                attendeeUserId = attendeeId,
                registrationId = registrationId,
                qrCredentialId = qrCredentialId,
                scanPurposeId = UUID.randomUUID(),
                transactionType = com.thedavelopers.eventqr.core.api.dto.TransactionType.ENTRY,
                transactionResult = com.thedavelopers.eventqr.core.api.dto.TransactionResult.APPROVED,
                pointsDelta = 50,
                scannedAt = Instant.parse("2026-05-10T02:15:00Z")
            ),
            TransactionResponse(
                transactionId = UUID.randomUUID(),
                eventId = eventIdA,
                eventTitle = "UI/UX Design Conference",
                attendeeUserId = attendeeId,
                registrationId = registrationId,
                qrCredentialId = qrCredentialId,
                scanPurposeId = UUID.randomUUID(),
                transactionType = com.thedavelopers.eventqr.core.api.dto.TransactionType.ATTENDANCE,
                transactionResult = com.thedavelopers.eventqr.core.api.dto.TransactionResult.APPROVED,
                pointsDelta = 50,
                scannedAt = Instant.parse("2026-05-10T03:00:00Z")
            ),
            TransactionResponse(
                transactionId = UUID.randomUUID(),
                eventId = eventIdA,
                eventTitle = "UI/UX Design Conference",
                attendeeUserId = attendeeId,
                registrationId = registrationId,
                qrCredentialId = qrCredentialId,
                scanPurposeId = UUID.randomUUID(),
                transactionType = com.thedavelopers.eventqr.core.api.dto.TransactionType.BOOTH_VISIT,
                transactionResult = com.thedavelopers.eventqr.core.api.dto.TransactionResult.APPROVED,
                pointsDelta = 25,
                scannedAt = Instant.parse("2026-05-10T05:30:00Z")
            ),
            TransactionResponse(
                transactionId = UUID.randomUUID(),
                eventId = eventIdA,
                eventTitle = "UI/UX Design Conference",
                attendeeUserId = attendeeId,
                registrationId = registrationId,
                qrCredentialId = qrCredentialId,
                scanPurposeId = UUID.randomUUID(),
                transactionType = com.thedavelopers.eventqr.core.api.dto.TransactionType.REWARD_REDEMPTION,
                transactionResult = com.thedavelopers.eventqr.core.api.dto.TransactionResult.APPROVED,
                pointsDelta = -100,
                scannedAt = Instant.parse("2026-05-10T06:00:00Z")
            ),
            TransactionResponse(
                transactionId = UUID.randomUUID(),
                eventId = eventIdA,
                eventTitle = "UI/UX Design Conference",
                attendeeUserId = attendeeId,
                registrationId = registrationId,
                qrCredentialId = qrCredentialId,
                scanPurposeId = UUID.randomUUID(),
                transactionType = com.thedavelopers.eventqr.core.api.dto.TransactionType.EXIT,
                transactionResult = com.thedavelopers.eventqr.core.api.dto.TransactionResult.APPROVED,
                pointsDelta = 25,
                scannedAt = Instant.parse("2026-05-10T09:00:00Z")
            ),
            TransactionResponse(
                transactionId = UUID.randomUUID(),
                eventId = eventIdB,
                eventTitle = "Startup Expo 2026",
                attendeeUserId = attendeeId,
                registrationId = registrationId,
                qrCredentialId = qrCredentialId,
                scanPurposeId = UUID.randomUUID(),
                transactionType = com.thedavelopers.eventqr.core.api.dto.TransactionType.ENTRY,
                transactionResult = com.thedavelopers.eventqr.core.api.dto.TransactionResult.APPROVED,
                pointsDelta = 50,
                scannedAt = Instant.parse("2026-04-01T01:05:00Z")
            )
        )
    }
}
