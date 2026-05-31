package com.thedavelopers.eventqr.features.staff

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.dto.AccountRole
import com.thedavelopers.eventqr.core.api.dto.EventStatus
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.core.util.RoleMapper
import com.thedavelopers.eventqr.features.staff.model.dto.StaffAssignedEventResponse
import com.thedavelopers.eventqr.features.staff.scanner.ScannerActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

open class StaffAssignedEventsActivity : AppCompatActivity() {
    private lateinit var repository: StaffRepository
    private lateinit var eventHost: LinearLayout
    private lateinit var emptyState: TextView
    private lateinit var progress: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
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

        setContentView(R.layout.activity_staff_assigned_events)
        repository = StaffRepository(this)
        eventHost = findViewById(R.id.layoutAssignedEvents)
        emptyState = findViewById(R.id.txtAssignedEventsEmpty)
        progress = findViewById(R.id.progressAssignedEvents)
        swipeRefresh = findViewById(R.id.swipeRefreshAssignedEvents)
        swipeRefresh.setColorSchemeResources(R.color.eventqr_purple)
        swipeRefresh.setOnRefreshListener { loadEvents(showLoading = false) }

        setupBottomNav()
        loadEvents()
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.navDashboard)?.setOnClickListener {
            startActivity(Intent(this, StaffDashboardActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navScanner)?.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navLogs)?.setOnClickListener {
            startActivity(Intent(this, StaffTransactionsActivity::class.java))
            finish()
        }
    }

    private fun loadEvents(showLoading: Boolean = true) {
        if (showLoading && !swipeRefresh.isRefreshing) {
            progress.visibility = View.VISIBLE
        }
        MainScope().launch {
            when (val result = repository.getEvents()) {
                is NetworkResult.Success -> renderEvents(result.data)
                is NetworkResult.Error -> {
                    eventHost.removeAllViews()
                    emptyState.text = result.message
                    emptyState.visibility = View.VISIBLE
                    Toast.makeText(this@StaffAssignedEventsActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                NetworkResult.Loading -> Unit
            }
            progress.visibility = View.GONE
            swipeRefresh.isRefreshing = false
        }
    }

    private fun renderEvents(items: List<StaffAssignedEventResponse>) {
        eventHost.removeAllViews()
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        if (items.isEmpty()) return

        items.sortedBy { it.eventStartAt }.forEach { event ->
            eventHost.addView(eventCard(event))
        }
    }

    private fun eventCard(event: StaffAssignedEventResponse): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(Color.WHITE, 16, Color.parseColor("#E5E7EB"), 1)
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(0, 0, 0, dp(14)) }

            addView(View(this@StaffAssignedEventsActivity).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(Color.parseColor("#5A45F2"), Color.parseColor("#7C3AED")),
                ).apply { cornerRadius = dp(2).toFloat() }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(4),
                ).apply { setMargins(0, 0, 0, dp(14)) }
            })

            addView(LinearLayout(this@StaffAssignedEventsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(this@StaffAssignedEventsActivity).apply {
                    text = event.title.ifBlank { "Untitled Event" }
                    setTextColor(Color.parseColor("#111827"))
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(statusBadge(event.status))
            })

            addView(LinearLayout(this@StaffAssignedEventsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, 0)
                addView(TextView(this@StaffAssignedEventsActivity).apply {
                    text = "☷ ${formatDate(event)}"
                    setTextColor(Color.parseColor("#6B7280"))
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(this@StaffAssignedEventsActivity).apply {
                    text = "⌖ ${event.location?.takeIf { it.isNotBlank() } ?: "Location not set"}"
                    setTextColor(Color.parseColor("#6B7280"))
                    textSize = 13f
                    maxLines = 1
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
            })

            addView(LinearLayout(this@StaffAssignedEventsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(14), 0, 0)
                addView(actionButton("⌗  Scan", filled = true) {
                    startActivity(Intent(this@StaffAssignedEventsActivity, ScannerActivity::class.java).apply {
                        putExtra(StaffScreenExtras.EXTRA_EVENT_ID, event.eventId.toString())
                    })
                })
                addView(actionButton("♙  Attendees", filled = false) {
                    startActivity(Intent(this@StaffAssignedEventsActivity, EventRegistrationsActivity::class.java).apply {
                        putExtra(StaffScreenExtras.EXTRA_EVENT_ID, event.eventId.toString())
                    })
                })
            })
        }
    }

    private fun statusBadge(status: EventStatus): TextView {
        val label = when (status) {
            EventStatus.ACTIVE -> "Active"
            EventStatus.ENDED -> "Ended"
            else -> "Upcoming"
        }
        return TextView(this).apply {
            text = label
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#4F46E5"))
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = rounded(Color.parseColor("#EEF2FF"), 16, null, 0)
        }
    }

    private fun actionButton(label: String, filled: Boolean, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (filled) Color.WHITE else Color.parseColor("#4F46E5"))
            background = if (filled) {
                rounded(Color.parseColor("#5B25C9"), 10, null, 0)
            } else {
                rounded(Color.WHITE, 10, Color.parseColor("#4F46E5"), 1)
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(if (filled) 8 else 0), 0) }
            setOnClickListener { onClick() }
        }
    }

    private fun formatDate(event: StaffAssignedEventResponse): String =
        event.eventStartAt?.atZone(manilaZone)?.format(dateFormatter) ?: "Date not set"

    private fun rounded(color: Int, radius: Int, strokeColor: Int?, strokeWidth: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != null && strokeWidth > 0) setStroke(dp(strokeWidth), strokeColor)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
