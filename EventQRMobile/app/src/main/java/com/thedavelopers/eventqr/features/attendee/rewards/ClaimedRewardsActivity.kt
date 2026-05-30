package com.thedavelopers.eventqr.features.attendee

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.dto.RedemptionStatus
import com.thedavelopers.eventqr.features.rewards.model.dto.RewardRedemptionResponse
import java.time.Instant
import java.util.UUID

open class ClaimedRewardsActivity : AppCompatActivity(), ClaimedRewardsContract.View {
    private lateinit var presenter: ClaimedRewardsPresenter
    private lateinit var adapter: com.thedavelopers.eventqr.features.rewards.ClaimedRewardAdapter
    private lateinit var loadingView: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var errorView: TextView
    private lateinit var retryButton: Button
    private lateinit var recyclerView: RecyclerView
    private var eventId: String = ""
    private val isDebuggableBuild: Boolean by lazy {
        (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_claimed_rewards)

        presenter = ClaimedRewardsPresenter(this, AttendeeRepository(this))
        adapter = com.thedavelopers.eventqr.features.rewards.ClaimedRewardAdapter()

        loadingView = findViewById(R.id.progressClaimedRewardsLoading)
        emptyView = findViewById(R.id.txtClaimedRewardsEmpty)
        errorView = findViewById(R.id.txtClaimedRewardsError)
        retryButton = findViewById(R.id.btnClaimedRewardsRetry)
        recyclerView = findViewById(R.id.recyclerClaimedRewards)

        eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
        retryButton.setOnClickListener {
            if (eventId.isBlank()) {
                showError("No event selected for claimed rewards.")
            } else {
                presenter.loadRedemptions(eventId)
            }
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ClaimedRewardsActivity)
            adapter = this@ClaimedRewardsActivity.adapter
        }

        if (eventId.isNotBlank()) {
            presenter.loadRedemptions(eventId)
        } else {
            if (isDebuggableBuild) {
                renderRedemptions(
                    items = sampleFallbackItems(),
                    eventTitle = "UI/UX Design Conference",
                    rewardNamesById = emptyMap()
                )
            } else {
                showError("No event selected for claimed rewards.")
            }
        }
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun showLoading(isLoading: Boolean) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            emptyView.visibility = View.GONE
            errorView.visibility = View.GONE
            retryButton.visibility = View.GONE
            recyclerView.visibility = View.GONE
        }
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        loadingView.visibility = View.GONE

        if (isDebuggableBuild) {
            errorView.text = "Unable to load live claimed rewards. Showing sample data for development."
            errorView.visibility = View.VISIBLE
            retryButton.visibility = View.VISIBLE
            renderRedemptions(
                items = sampleFallbackItems(),
                eventTitle = "UI/UX Design Conference",
                rewardNamesById = emptyMap()
            )
            return
        }

        errorView.text = message.ifBlank { "Unable to load claimed rewards." }
        errorView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.GONE
    }

    override fun renderRedemptions(
        items: List<RewardRedemptionResponse>,
        eventTitle: String?,
        rewardNamesById: Map<String, String>,
    ) {
        loadingView.visibility = View.GONE
        errorView.visibility = View.GONE
        retryButton.visibility = View.GONE

        val sorted = items.sortedByDescending { it.redeemedAt ?: Instant.EPOCH }
        adapter.submitItems(sorted, eventTitle, rewardNamesById)

        val isEmpty = sorted.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun sampleFallbackItems(): List<RewardRedemptionResponse> {
        val event = UUID.randomUUID()
        val attendee = UUID.randomUUID()
        val reward = UUID.randomUUID()
        return listOf(
            RewardRedemptionResponse(
                redemptionId = UUID.randomUUID(),
                eventId = event,
                attendeeUserId = attendee,
                rewardId = reward,
                pointsSpent = 100,
                status = RedemptionStatus.REDEEMED,
                redeemedAt = Instant.parse("2026-05-10T06:00:00Z"),
                reason = "Coffee Voucher"
            )
        )
    }
}
