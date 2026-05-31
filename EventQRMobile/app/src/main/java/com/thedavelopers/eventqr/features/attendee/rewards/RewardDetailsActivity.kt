package com.thedavelopers.eventqr.features.attendee

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.session.SessionManager

open class RewardDetailsActivity : AppCompatActivity(), RewardsContract.View {
    private lateinit var presenter: RewardsPresenter
    private var eventId: String = ""
    private var rewardId: String = ""
    private var pointsRequired: Int = 0
    private var stockQuantity: Int = -1
    private var currentBalance: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_reward_details)

        presenter = RewardsPresenter(this, AttendeeRepository(this))
        eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()
        rewardId = intent.getStringExtra(EXTRA_REWARD_ID).orEmpty()
        pointsRequired = intent.getIntExtra(EXTRA_REWARD_POINTS, 0)
        stockQuantity = intent.getIntExtra(EXTRA_REWARD_STOCK, -1)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        val rewardName = intent.getStringExtra(EXTRA_REWARD_NAME).orEmpty().ifBlank { "Reward" }
        findViewById<TextView>(R.id.txtRewardTitle)?.text = rewardName
        findViewById<TextView>(R.id.txtRewardDescription)?.text = "Redeem this reward using your event points."
        findViewById<TextView>(R.id.txtPointsValue)?.text = pointsRequired.toString()
        findViewById<TextView>(R.id.txtRewardRemaining)?.text = formatRemainingStock()
        findViewById<TextView>(R.id.txtRewardExpires)?.text = "At event end"
        findViewById<TextView>(R.id.txtUserPoints)?.text = "0 pts"
        updateAvailabilityUi()

        val userId = SessionManager(this).getUserId()
        if (eventId.isNotBlank() && userId != null) {
            presenter.load(eventId, userId)
        }

        findViewById<Button>(R.id.btnRedeemReward)?.setOnClickListener {
            presenter.redeem(eventId, userId, rewardId)
        }
    }

    override fun showLoading(isLoading: Boolean) = Unit

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showBalance(balance: com.thedavelopers.eventqr.features.rewards.model.dto.PointBalanceResponse) {
        currentBalance = balance.pointsBalance
        findViewById<TextView>(R.id.txtUserPoints)?.text = "${balance.pointsBalance} pts"
        updateAvailabilityUi()
    }

    override fun renderRewards(items: List<com.thedavelopers.eventqr.features.rewards.model.dto.RewardResponse>) = Unit

    private fun updateAvailabilityUi() {
        val missingPoints = (pointsRequired - currentBalance).coerceAtLeast(0)
        val isOutOfStock = stockQuantity == 0
        val canRedeem = missingPoints == 0 && !isOutOfStock
        val redeemButton = findViewById<Button>(R.id.btnRedeemReward)
        val warning = findViewById<TextView>(R.id.warningBox)
        val status = findViewById<TextView>(R.id.txtRewardStatus)

        if (isOutOfStock) {
            status?.text = "Out of Stock"
            status?.setBackgroundResource(R.drawable.bg_red_warning)
            status?.setTextColor(0xFFB91C1C.toInt())
            warning?.visibility = View.VISIBLE
            warning?.text = "This reward is currently out of stock."
            redeemButton?.isEnabled = false
            redeemButton?.alpha = 0.65f
            redeemButton?.text = "Out of Stock"
            return
        }

        status?.text = "Available"
        status?.setBackgroundResource(R.drawable.bg_green_pill)
        status?.setTextColor(0xFF065F46.toInt())

        if (canRedeem) {
            warning?.visibility = View.GONE
            redeemButton?.isEnabled = true
            redeemButton?.alpha = 1.0f
            redeemButton?.text = "Redeem Reward"
        } else {
            warning?.visibility = View.VISIBLE
            warning?.text = "You need $missingPoints more points to redeem this reward."
            redeemButton?.isEnabled = false
            redeemButton?.alpha = 0.65f
            redeemButton?.text = "Need $missingPoints more points"
        }
    }

    private fun formatRemainingStock(): String {
        return if (stockQuantity < 0) {
            "Stock unavailable"
        } else {
            "$stockQuantity left"
        }
    }
}
