package com.thedavelopers.eventqr.features.staff

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.dto.AccountRole
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.core.util.RoleMapper
import com.thedavelopers.eventqr.features.staff.scanner.ScannerActivity
import com.thedavelopers.eventqr.features.transactions.TransactionLogAdapter
import com.thedavelopers.eventqr.features.transactions.model.dto.TransactionResponse
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.Instant

open class StaffTransactionsActivity : AppCompatActivity(), StaffTransactionsContract.View {
    private lateinit var repository: StaffRepository
    private lateinit var adapter: TransactionLogAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var selectedEventId: String = ""
    private var assignedEventIds: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        if (RoleMapper.normalizeRole(sessionManager.getUserRole()) != AccountRole.STAFF.name) {
            Toast.makeText(this, "Access Denied: Staff only", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_staff_transaction_logs)

        repository = StaffRepository(this)
        adapter = TransactionLogAdapter()
        selectedEventId = intent.getStringExtra(StaffScreenExtras.EXTRA_EVENT_ID).orEmpty()

        findViewById<View>(R.id.cardStaffTransactionsEvent)?.visibility = View.GONE
        findViewById<View>(R.id.spnStaffTransactionsEvent)?.visibility = View.GONE

        swipeRefresh = findViewById(R.id.swipeRefreshStaffTransactions)
        swipeRefresh.setColorSchemeResources(R.color.eventqr_purple)
        swipeRefresh.setOnRefreshListener { loadAllAssignedTransactions() }

        findViewById<RecyclerView>(R.id.recyclerStaffTransactions).apply {
            layoutManager = LinearLayoutManager(this@StaffTransactionsActivity)
            adapter = this@StaffTransactionsActivity.adapter
        }

        setupBottomNav()
        loadAllAssignedTransactions()
    }

    private fun loadAllAssignedTransactions() {
        MainScope().launch {
            showLoading(true)
            when (val eventsResult = repository.getEvents()) {
                is NetworkResult.Success -> {
                    val assignedEvents = eventsResult.data.filter { it.canScan && it.status.name != "ENDED" }
                    assignedEventIds = assignedEvents.map { it.eventId.toString() }
                    selectedEventId = selectedEventId.takeIf { it in assignedEventIds }
                        ?: assignedEventIds.firstOrNull().orEmpty()

                    if (assignedEvents.isEmpty()) {
                        renderTransactions(emptyList())
                        showMessage("No assigned events found")
                        showLoading(false)
                        return@launch
                    }

                    val combinedLogs = mutableListOf<TransactionResponse>()
                    var partialError: String? = null

                    assignedEvents.forEach { event ->
                        when (val logsResult = repository.getTransactionsByEvent(event.eventId.toString())) {
                            is NetworkResult.Success -> combinedLogs.addAll(logsResult.data)
                            is NetworkResult.Error -> partialError = logsResult.message
                            NetworkResult.Loading -> Unit
                        }
                    }

                    renderTransactions(combinedLogs.sortedByDescending { it.scannedAt ?: Instant.EPOCH })
                    if (partialError != null) showMessage("Some transaction logs could not be loaded.")
                }
                is NetworkResult.Error -> {
                    renderTransactions(emptyList())
                    showMessage(eventsResult.message)
                }
                NetworkResult.Loading -> Unit
            }
            showLoading(false)
        }
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.navDashboard)?.setOnClickListener {
            startActivity(Intent(this, StaffDashboardActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navScanner)?.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java).apply {
                selectedEventId.takeIf { it.isNotBlank() }?.let { putExtra(StaffScreenExtras.EXTRA_EVENT_ID, it) }
            })
            finish()
        }
        findViewById<View>(R.id.navEvents)?.setOnClickListener {
            startActivity(Intent(this, StaffAssignedEventsActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun renderTransactions(items: List<TransactionResponse>) {
        swipeRefresh.isRefreshing = false
        adapter.submitItems(items)
        findViewById<TextView>(R.id.txtTotalScans).text = items.size.toString()
        findViewById<TextView>(R.id.txtSuccessfulScans).text = items.count { it.transactionResult.name == "APPROVED" || it.transactionResult.name == "SUCCESS" }.toString()
        findViewById<TextView>(R.id.txtRejectedScans).text = items.count { it.transactionResult.name != "APPROVED" && it.transactionResult.name != "SUCCESS" }.toString()
        findViewById<TextView>(R.id.txtStaffTransactionsEmptyState).visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.recyclerStaffTransactions).visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun showMessage(message: String) {
        swipeRefresh.isRefreshing = false
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading(isLoading: Boolean) {
        if (!swipeRefresh.isRefreshing) {
            findViewById<View>(R.id.progressScanner)?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        if (!isLoading) {
            swipeRefresh.isRefreshing = false
        }
    }
}
