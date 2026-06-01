package com.thedavelopers.eventqr.features.organizer.rewards

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.dto.ScanPurposeCode
import com.thedavelopers.eventqr.features.organizer.BG
import com.thedavelopers.eventqr.features.organizer.BORDER
import com.thedavelopers.eventqr.features.organizer.MUTED
import com.thedavelopers.eventqr.features.organizer.NAV_EVENTS
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpEvent
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpScanPurpose
import com.thedavelopers.eventqr.features.organizer.OrganizerRepository
import com.thedavelopers.eventqr.features.organizer.PURPLE
import com.thedavelopers.eventqr.features.organizer.SUCCESS
import com.thedavelopers.eventqr.features.organizer.TEXT
import com.thedavelopers.eventqr.features.organizer.dp
import com.thedavelopers.eventqr.features.organizer.emptyState
import com.thedavelopers.eventqr.features.organizer.errorState
import com.thedavelopers.eventqr.features.organizer.eventSelector
import com.thedavelopers.eventqr.features.organizer.formatCount
import com.thedavelopers.eventqr.features.organizer.intentEventId
import com.thedavelopers.eventqr.features.organizer.organizerShell
import com.thedavelopers.eventqr.features.organizer.resolveSelectedEvent
import com.thedavelopers.eventqr.features.organizer.rounded
import com.thedavelopers.eventqr.features.organizer.saveSelectedEventId
import com.thedavelopers.eventqr.features.organizer.selectedEventId
import com.thedavelopers.eventqr.features.organizer.showMissingEventScreen
import com.thedavelopers.eventqr.features.organizer.text
import com.thedavelopers.eventqr.features.rewards.model.dto.PointRuleRequest
import com.thedavelopers.eventqr.features.rewards.model.dto.PointRuleResponse
import kotlinx.coroutines.launch
import java.util.UUID

open class PointRulesPlaceholderActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var root: LinearLayout
    private lateinit var selectedEvent: OrganizerMvpEvent
    private var eventOptions: List<OrganizerMvpEvent> = emptyList()
    private var scanPurposes: List<OrganizerMvpScanPurpose> = emptyList()
    private var pointRules: List<PointRuleResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        eventOptions = repository.getApprovedOrganizerEvents()
        val requestedEventId = intentEventId() ?: selectedEventId().takeIf { it.isNotBlank() }
        selectedEvent = resolveSelectedEvent(eventOptions, requestedEventId)
            ?: return showMissingEventScreen("Organizer / Point Rules")
        renderListShell()
        loadPointRules()
    }

    private fun renderListShell() {
        root = organizerShell(
            title = "Organizer / Point Rules",
            selectedNav = NAV_EVENTS,
            showBack = true,
            topRightLabel = "+ Add",
            onTopRight = { showRuleForm(null) },
        )
        root.setBackgroundColor(BG)
        root.removeAllViews()
        root.addView(text("Loading point rules...", 14, false, MUTED).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, dp(28))
        })
    }

    private fun loadPointRules() {
        val eventId = selectedEvent.id
        lifecycleScope.launch {
            root.removeAllViews()
            root.addView(text("Loading point rules...", 14, false, MUTED).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(28), 0, dp(28))
            })
            val purposeLoad = repository.loadScanPurposesForMvp(eventId)
            val ruleResult = repository.getPointRules(eventId)
            if (selectedEvent.id != eventId) return@launch
            scanPurposes = purposeLoad.data.sortedBy { purposeOrder(it.code) }
            pointRules = when (ruleResult) {
                is NetworkResult.Success -> ruleResult.data
                is NetworkResult.Error -> {
                    Toast.makeText(this@PointRulesPlaceholderActivity, ruleResult.message, Toast.LENGTH_SHORT).show()
                    emptyList()
                }
                NetworkResult.Loading -> emptyList()
            }
            renderPointRules(purposeLoad.source == com.thedavelopers.eventqr.features.organizer.OrganizerMvpDataSource.ERROR, purposeLoad.message)
        }
    }

    private fun renderPointRules(hasPurposeError: Boolean = false, purposeError: String? = null) {
        root.removeAllViews()
        root.addView(eventSelector(eventOptions, selectedEvent.id) { event ->
            if (event.id == selectedEvent.id) return@eventSelector
            selectedEvent = event
            repository.saveSelectedEventId(event.id)
            saveSelectedEventId(event.id)
            loadPointRules()
        }.apply {
            background = rounded(Color.WHITE, 10, BORDER, density = resources.displayMetrics.density)
            setPadding(dp(12), 0, dp(12), 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
        })

        root.addView(eventBanner())

        if (hasPurposeError) {
            root.addView(errorState(purposeError ?: "Unable to load scan purposes.") { loadPointRules() })
            return
        }

        if (scanPurposes.isEmpty()) {
            root.addView(emptyState("Create scan purposes first before adding point rules.", "Open Scan Purposes") {
                openOrganizerPage(com.thedavelopers.eventqr.features.organizer.scanpurposes.ManageScanPurposesActivity::class.java, selectedEvent.id, selectedEvent.title)
            })
            return
        }

        scanPurposes.forEach { purpose -> root.addView(pointRuleCard(purpose)) }
    }

    private fun eventBanner(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = rounded(Color.parseColor("#EEF2FF"), 12, null, density = resources.displayMetrics.density)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(16), 0, dp(14))
        }
        addView(ImageView(this@PointRulesPlaceholderActivity).apply {
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
            setImageResource(R.drawable.ic_star_outline)
            setColorFilter(PURPLE)
        })
        addView(text(selectedEvent.title, 13, true, PURPLE).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) }
        })
    }

    private fun pointRuleCard(purpose: OrganizerMvpScanPurpose): LinearLayout {
        val rule = ruleFor(purpose)
        val points = rule?.points ?: if (purpose.pointsEnabled) purpose.pointsValue else 0
        val active = rule?.active ?: (purpose.pointsEnabled && !purpose.trackingOnly && points > 0)
        val subtitle = ruleSubtitle(purpose, active, points)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(Color.WHITE, 14, BORDER, density = resources.displayMetrics.density)
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }

            addView(ImageView(this@PointRulesPlaceholderActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                background = rounded(Color.parseColor("#6D3FF2"), 12, null, density = resources.displayMetrics.density)
                setPadding(dp(9), dp(9), dp(9), dp(9))
                setImageResource(R.drawable.ic_star_outline)
                setColorFilter(Color.WHITE)
            })

            addView(LinearLayout(this@PointRulesPlaceholderActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(14) }
                addView(text(displayPurposeName(purpose), 15, true, TEXT))
                addView(text(subtitle, 13, true, if (active && points > 0) SUCCESS else SUCCESS).apply {
                    setPadding(0, dp(3), 0, 0)
                })
            })

            addView(text("Edit", 13, true, PURPLE).apply {
                setPadding(dp(10), dp(10), 0, dp(10))
                setOnClickListener { showRuleForm(purpose) }
            })
        }
    }

    private fun ruleSubtitle(purpose: OrganizerMvpScanPurpose, active: Boolean, points: Int): String {
        val code = purpose.code
        if (code == ScanPurposeCode.REWARD_REDEMPTION || code == ScanPurposeCode.REWARD_REDEMPTION_SCAN) {
            return "Variable deduction · Deducts reward cost"
        }
        if (purpose.trackingOnly || !active || points <= 0) {
            return "No points · Tracking only"
        }
        return "+${formatCount(points)} pts"
    }

    private fun showRuleForm(purposeToEdit: OrganizerMvpScanPurpose?) {
        if (scanPurposes.isEmpty()) {
            Toast.makeText(this, "Create scan purposes first.", Toast.LENGTH_SHORT).show()
            return
        }
        val editablePurposes = scanPurposes.filterNot {
            it.code == ScanPurposeCode.REWARD_REDEMPTION || it.code == ScanPurposeCode.REWARD_REDEMPTION_SCAN
        }
        val initialPurpose = purposeToEdit?.takeIf { editablePurposes.any { item -> item.id == it.id } } ?: editablePurposes.firstOrNull()
        if (initialPurpose == null) {
            Toast.makeText(this, "No editable point purposes available.", Toast.LENGTH_SHORT).show()
            return
        }

        val formRoot = organizerShell(
            title = if (purposeToEdit == null) "Add Point Rule" else "Edit Point Rule",
            showBack = true,
        )
        formRoot.setBackgroundColor(BG)
        formRoot.removeAllViews()
        var selectedPurpose = initialPurpose

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = rounded(Color.WHITE, 14, BORDER, density = resources.displayMetrics.density)
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val purposeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@PointRulesPlaceholderActivity,
                android.R.layout.simple_spinner_item,
                editablePurposes.map { displayPurposeName(it) },
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(editablePurposes.indexOfFirst { it.id == selectedPurpose.id }.coerceAtLeast(0), false)
            background = rounded(Color.parseColor("#F9FAFB"), 10, BORDER, density = resources.displayMetrics.density)
            setPadding(dp(14), 0, dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50))
        }
        val pointsInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            background = rounded(Color.parseColor("#F9FAFB"), 10, BORDER, density = resources.displayMetrics.density)
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50))
        }
        val trackingSwitch = SwitchCompat(this)

        fun bindPurposeFields() {
            val existingRule = ruleFor(selectedPurpose)
            val existingPoints = existingRule?.points ?: if (selectedPurpose.pointsEnabled) selectedPurpose.pointsValue else 0
            val trackingOnly = selectedPurpose.trackingOnly || existingRule?.active == false || existingPoints <= 0
            pointsInput.setText(if (trackingOnly) "0" else existingPoints.coerceAtLeast(0).toString())
            pointsInput.isEnabled = !trackingOnly
            trackingSwitch.isChecked = trackingOnly
        }

        purposeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPurpose = editablePurposes.getOrNull(position) ?: selectedPurpose
                bindPurposeFields()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        card.addView(label("Scan Purpose"))
        card.addView(purposeSpinner)
        card.addView(label("Points to Award"))
        card.addView(pointsInput)
        card.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, 0)
            addView(trackingSwitch.apply {
                layoutParams = LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT)
                setOnCheckedChangeListener { _, checked ->
                    pointsInput.isEnabled = !checked
                    if (checked) pointsInput.setText("0")
                }
            })
            addView(LinearLayout(this@PointRulesPlaceholderActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) }
                addView(text("Tracking Only", 15, true, TEXT))
                addView(text("Record scan but don't award points", 13, false, MUTED))
            })
        })
        formRoot.addView(card)

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        footer.addView(Button(this).apply {
            text = "Save Rule"
            setAllCaps(false)
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = rounded(PURPLE, 10, null, density = resources.displayMetrics.density)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
                setMargins(0, dp(20), 0, 0)
            }
            setOnClickListener {
                val trackingOnly = trackingSwitch.isChecked
                val points = pointsInput.text.toString().trim().toIntOrNull() ?: 0
                if (!trackingOnly && points <= 0) {
                    Toast.makeText(this@PointRulesPlaceholderActivity, "Enter points greater than 0 or enable Tracking Only.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                saveRule(selectedPurpose, if (trackingOnly) 0 else points, !trackingOnly)
            }
        })
        formRoot.addView(footer)
        bindPurposeFields()
    }

    private fun saveRule(purpose: OrganizerMvpScanPurpose, points: Int, active: Boolean) {
        val purposeId = purpose.id ?: return Toast.makeText(this, "Scan purpose ID is missing.", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val eventId = selectedEvent.id
            val pointRuleRequest = PointRuleRequest(UUID.fromString(eventId), UUID.fromString(purposeId), points, active)
            val ruleResult = repository.savePointRule(pointRuleRequest)
            val purposeUpdate = repository.saveScanPurposesForMvp(
                eventId,
                listOf(
                    purpose.copy(
                        trackingOnly = !active,
                        pointsEnabled = active,
                        pointsValue = points,
                    ),
                ),
            )
            val failedMessage = when {
                ruleResult is NetworkResult.Error -> ruleResult.message
                purposeUpdate.source == com.thedavelopers.eventqr.features.organizer.OrganizerMvpDataSource.ERROR -> purposeUpdate.message ?: "Scan purpose could not be updated."
                else -> null
            }
            if (failedMessage != null) {
                Toast.makeText(this@PointRulesPlaceholderActivity, failedMessage, Toast.LENGTH_LONG).show()
                return@launch
            }
            Toast.makeText(this@PointRulesPlaceholderActivity, "Point rule saved.", Toast.LENGTH_SHORT).show()
            renderListShell()
            loadPointRules()
        }
    }

    private fun label(value: String): TextView = text(value, 14, true, TEXT).apply {
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun ruleFor(purpose: OrganizerMvpScanPurpose): PointRuleResponse? {
        val purposeId = purpose.id ?: return null
        return pointRules.firstOrNull { it.scanPurposeId.toString() == purposeId }
    }

    private fun displayPurposeName(purpose: OrganizerMvpScanPurpose): String = when (purpose.code) {
        ScanPurposeCode.ENTRY -> "Event Entry"
        ScanPurposeCode.ATTENDANCE -> "Session Attendance"
        ScanPurposeCode.BOOTH_VISIT -> "Booth Visit"
        ScanPurposeCode.SESSION_VISIT -> "Session Attendance"
        ScanPurposeCode.BENEFIT_CLAIM -> "Benefit Claim"
        ScanPurposeCode.REWARD_REDEMPTION,
        ScanPurposeCode.REWARD_REDEMPTION_SCAN -> "Reward Redemption"
        ScanPurposeCode.EXIT -> "Event Exit"
        else -> purpose.label.ifBlank { "Scan Purpose" }
    }

    private fun purposeOrder(code: ScanPurposeCode?): Int = when (code) {
        ScanPurposeCode.ENTRY -> 1
        ScanPurposeCode.ATTENDANCE -> 2
        ScanPurposeCode.SESSION_VISIT -> 3
        ScanPurposeCode.BOOTH_VISIT -> 4
        ScanPurposeCode.BENEFIT_CLAIM -> 5
        ScanPurposeCode.REWARD_REDEMPTION,
        ScanPurposeCode.REWARD_REDEMPTION_SCAN -> 6
        ScanPurposeCode.EXIT -> 7
        else -> 99
    }
}
