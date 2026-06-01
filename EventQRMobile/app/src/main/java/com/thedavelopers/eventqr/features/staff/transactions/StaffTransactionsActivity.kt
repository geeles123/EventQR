package com.thedavelopers.eventqr.features.staff

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

open class StaffTransactionsActivity : AppCompatActivity(), StaffTransactionsContract.View {
    private lateinit var presenter: StaffTransactionsPresenter
    private lateinit var repository: StaffRepository
    private lateinit var adapter: TransactionLogAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var eventCard: LinearLayout
    private lateinit var eventSpinner: Spinner
    private lateinit var eventTitle: TextView
    private lateinit var eventDate: TextView
    private lateinit var eventChevron: TextView

    private val eventOptions = mutableListOf<EventSpinnerOption>()
    private var selectedEventId: String = ""
    private var eventPopup: PopupWindow? = null
    private var isEventDropdownOpen = false
    private val manilaZone: ZoneId = ZoneId.of("Asia/Manila")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)

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
        presenter = StaffTransactionsPresenter(this, repository)
        adapter = TransactionLogAdapter()
        selectedEventId = intent.getStringExtra(StaffScreenExtras.EXTRA_EVENT_ID).orEmpty()

        eventCard = findViewById(R.id.cardStaffTransactionsEvent)
        eventSpinner = findViewById(R.id.spnStaffTransactionsEvent)
        eventTitle = findViewById(R.id.txtStaffTransactionsEventTitle)
        eventDate = findViewById(R.id.txtStaffTransactionsEventDate)
        eventChevron = findViewById(R.id.txtStaffTransactionsEventChevron)
        eventChevron.includeFontPadding = false
        eventChevron.translationY = -dp(1).toFloat()
        eventChevron.text = "▾"
        eventCard.setOnClickListener { setEventDropdownOpen(!isEventDropdownOpen) }

        swipeRefresh = findViewById(R.id.swipeRefreshStaffTransactions)
        swipeRefresh.setColorSchemeResources(R.color.eventqr_purple)
        swipeRefresh.setOnRefreshListener { refreshTransactions() }

        findViewById<RecyclerView>(R.id.recyclerStaffTransactions).apply {
            layoutManager = LinearLayoutManager(this@StaffTransactionsActivity)
            adapter = this@StaffTransactionsActivity.adapter
        }

        setupBottomNav()
        loadAssignedEvents()
    }

    private fun loadAssignedEvents() {
        MainScope().launch {
            when (val eventsResult = repository.getEvents()) {
                is NetworkResult.Success -> {
                    eventOptions.clear()
                    eventOptions.addAll(
                        eventsResult.data
                            .filter { it.canScan && it.status.name != "ENDED" }
                            .map { EventSpinnerOption(it.eventId.toString(), it.title, it.canScan, it.eventStartAt) }
                    )
                    if (eventOptions.isEmpty()) {
                        bindSelectedEventHeader(null)
                        findViewById<TextView>(R.id.txtStaffTransactionsEmptyState).visibility = View.VISIBLE
                        showMessage("No assigned events found")
                        return@launch
                    }
                    val selectedIndex = eventOptions.indexOfFirst { it.id == selectedEventId }.takeIf { it >= 0 } ?: 0
                    selectedEventId = eventOptions[selectedIndex].id
                    eventSpinner.adapter = ArrayAdapter(this@StaffTransactionsActivity, android.R.layout.simple_spinner_dropdown_item, eventOptions.map { it.label })
                    eventSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val selected = eventOptions.getOrNull(position) ?: return
                            selectedEventId = selected.id
                            bindSelectedEventHeader(selected)
                            renderEventDropdown()
                            findViewById<EditText>(R.id.edtStaffTransactionsEventId).setText(selectedEventId)
                            presenter.load(selectedEventId)
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                    }
                    eventSpinner.setSelection(selectedIndex, false)
                    bindSelectedEventHeader(eventOptions[selectedIndex])
                    renderEventDropdown()
                    findViewById<EditText>(R.id.edtStaffTransactionsEventId).setText(selectedEventId)
                    presenter.load(selectedEventId)
                }
                is NetworkResult.Error -> showMessage(eventsResult.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun bindSelectedEventHeader(event: EventSpinnerOption?) {
        eventTitle.text = event?.label ?: "No assigned event"
        eventDate.text = event?.eventStartAt?.atZone(manilaZone)?.format(dateFormatter).orEmpty()
    }

    private fun renderEventDropdown() {
        eventPopup?.dismiss()
        eventPopup = PopupWindow(
            buildEventDropdownView(),
            eventCard.width.takeIf { it > 0 } ?: ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = dp(8).toFloat()
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setOnDismissListener { isEventDropdownOpen = false; eventChevron.text = "▾" }
        }
    }

    private fun buildEventDropdownView(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        eventOptions.forEachIndexed { index, event ->
            addView(LinearLayout(this@StaffTransactionsActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setBackgroundColor(if (index == eventSpinner.selectedItemPosition) Color.parseColor("#EEF2FF") else Color.WHITE)
                setOnClickListener {
                    eventSpinner.setSelection(index, false)
                    selectedEventId = event.id
                    bindSelectedEventHeader(event)
                    findViewById<EditText>(R.id.edtStaffTransactionsEventId).setText(selectedEventId)
                    setEventDropdownOpen(false)
                    presenter.load(selectedEventId)
                }
                addView(TextView(this@StaffTransactionsActivity).apply {
                    text = event.label
                    setTextColor(if (index == eventSpinner.selectedItemPosition) 0xFF4F46E5.toInt() else 0xFF111827.toInt())
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                addView(TextView(this@StaffTransactionsActivity).apply {
                    text = event.eventStartAt?.atZone(manilaZone)?.format(dateFormatter).orEmpty().ifBlank { "Assigned event" }
                    setTextColor(0xFF6B7280.toInt())
                    textSize = 13f
                })
            })
        }
    }

    private fun setEventDropdownOpen(open: Boolean) {
        if (open && eventOptions.isEmpty()) return
        if (open) {
            if (eventPopup == null || eventCard.width > 0 && eventPopup?.width != eventCard.width) renderEventDropdown()
            isEventDropdownOpen = true
            eventChevron.text = "▴"
            eventPopup?.showAsDropDown(eventCard, 0, 0)
        } else {
            eventPopup?.dismiss()
            isEventDropdownOpen = false
            eventChevron.text = "▾"
        }
    }

    private fun refreshTransactions() {
        selectedEventId = findViewById<EditText>(R.id.edtStaffTransactionsEventId).text.toString().ifBlank { selectedEventId }
        if (selectedEventId.isBlank()) {
            swipeRefresh.isRefreshing = false
            showMessage("No assigned event selected.")
            return
        }
        presenter.load(selectedEventId)
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
        eventPopup?.dismiss()
        presenter.detach()
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
        findViewById<View>(R.id.btnLoadStaffTransactions)?.isEnabled = !isLoading
        if (!isLoading) {
            swipeRefresh.isRefreshing = false
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
