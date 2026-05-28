package com.thedavelopers.eventqr.features.organizer.staff

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.features.organizer.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

open class ManageUsersActivity : AppCompatActivity() {
    private companion object {
        private const val TAG = "ManageUsersActivity"
    }

    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var search: EditText
    private lateinit var results: LinearLayout
    private lateinit var assigned: LinearLayout
    private lateinit var totalStaffValue: TextView
    private val assignedStaff = mutableListOf<OrganizerMvpStaff>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val eventId = intentEventId() ?: return showMissingEventScreen("Staff Access")
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents(), eventId) ?: return showMissingEventScreen("Staff Access")
        Log.d(TAG, "onCreate selectedEventId=${selectedEvent.id}")
        val content = organizerShell("Staff Access", selectedEvent.title, showBack = true, topRightLabel = "+") {
            renderSearch(showOnlyWhenQuery = false)
        }
        content.addView(card().apply {
            addView(text("Total Staff", 12, false, MUTED))
            totalStaffValue = text("0", 24, true)
            addView(totalStaffValue)
        })
        content.addView(primaryButton("+ Add Staff Member") { renderSearch(showOnlyWhenQuery = false) })
        search = EditText(this).apply {
            hint = "Search user by email/name"
            background = rounded(Color.WHITE, 10, BORDER, density = resources.displayMetrics.density)
            setPadding(dp(12), 0, dp(12), 0)
        }
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        assigned = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(search)
        content.addView(section("Search Results"))
        content.addView(results)
        content.addView(section("Staff Members"))
        content.addView(assigned)
        search.afterTextChanged { renderSearch() }
        renderSearch()
        loadAssigned()
    }

    private fun renderSearch(showOnlyWhenQuery: Boolean = true) {
        if (!::results.isInitialized) return
        val query = search.text.toString()
        Log.d(TAG, "renderSearch query=\"$query\" showOnlyWhenQuery=$showOnlyWhenQuery eventId=${selectedEvent.id}")
        results.removeAllViews()
        if (showOnlyWhenQuery && query.isBlank()) {
            results.addView(text("Search by name or email to add staff.", 13, false, MUTED))
            return
        }
        results.addView(loadingState("Searching users..."))
        MainScope().launch {
            val source = repository.searchStaffUsersForMvp(query)
            results.removeAllViews()
            dataSourceBanner(source)?.let { results.addView(it) }
            if (source.data.isEmpty()) {
                results.addView(text("User not found.", 13, true, ERROR))
                return@launch
            }
            source.data.forEach { user ->
                results.addView(staffCard(user.copy(assignedEventId = selectedEvent.id, assignedEvent = selectedEvent.title), true))
            }
        }
    }

    private fun renderAssigned(): Int {
        assigned.removeAllViews()
        val staff = assignedStaff.filter {
            it.assignedEventId.isBlank() || it.assignedEventId == selectedEvent.id
        }
        totalStaffValue.text = staff.size.toString()
        if (staff.isEmpty()) {
            assigned.addView(emptyState("Empty staff list. Add staff members before event day."))
            return 0
        }
        staff.forEach { assigned.addView(staffCard(it, false)) }
        assigned.addView(stateCard())
        return staff.size
    }

    private fun loadAssigned(showAlreadyAssignedRefreshFailureIfEmpty: Boolean = false) {
        assigned.removeAllViews()
        assigned.addView(loadingState("Loading staff..."))
        MainScope().launch {
            val source = repository.loadStaffForMvp(selectedEvent)
            Log.d(
                TAG,
                "loadAssigned result eventId=${selectedEvent.id} source=${source.source} message=${source.message} count=${source.data.size}",
            )
            assignedStaff.clear()
            source.data.forEach { incoming ->
                val existingIndex = assignedStaff.indexOfFirst { existing ->
                    existing.id == incoming.id ||
                        (
                            existing.email.equals(incoming.email, ignoreCase = true) &&
                                existing.assignedEventId == incoming.assignedEventId
                            )
                }
                if (existingIndex >= 0) {
                    assignedStaff[existingIndex] = incoming
                } else {
                    assignedStaff.add(incoming)
                }
            }
            source.message?.let {
                Toast.makeText(this@ManageUsersActivity, it, Toast.LENGTH_SHORT).show()
            }
            val renderedCount = renderAssigned()
            if (showAlreadyAssignedRefreshFailureIfEmpty && renderedCount == 0) {
                Toast.makeText(
                    this@ManageUsersActivity,
                    "Staff is already assigned, but assigned staff list could not be refreshed.",
                    Toast.LENGTH_LONG,
                ).show()
                Log.w(TAG, "already-assigned refresh completed with empty rendered list for eventId=${selectedEvent.id}")
            }
        }
    }

    private fun staffCard(staff: OrganizerMvpStaff, canAdd: Boolean): LinearLayout =
        card().apply {
            val top = row()
            top.addView(text(staff.name, 17, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            top.addView(badge(staff.accessStatus))
            addView(top)
            addView(chip(staff.roleLabel))
            addView(text(staff.email, 13, false, MUTED))
            addView(text("Permissions: ${staff.permissions.joinToString(", ")}", 12, false, MUTED))
            addView(text("Added: ${staff.addedDate}", 12, false, MUTED))
            if (canAdd) {
                addView(primaryButton("Add staff to event") {
                    Log.d(TAG, "addStaff click eventId=${selectedEvent.id} staffId=${staff.id} email=${staff.email}")
                    if (assignedStaff.any {
                            it.assignedEventId == selectedEvent.id &&
                                (
                                    it.id == staff.id ||
                                        it.email.equals(staff.email, ignoreCase = true)
                                    ) &&
                                it.accessStatus.equals("Active", ignoreCase = true)
                        }) {
                        Toast.makeText(this@ManageUsersActivity, "Duplicate staff assignment", Toast.LENGTH_SHORT).show()
                    } else {
                        MainScope().launch {
                            val source = repository.addStaffForMvp(selectedEvent, staff)
                            Log.d(
                                TAG,
                                "addStaff result eventId=${selectedEvent.id} source=${source.source} message=${source.message}",
                            )
                            source.message?.let {
                                Toast.makeText(this@ManageUsersActivity, it, Toast.LENGTH_SHORT).show()
                            }
                            val alreadyAssigned = source.message?.contains("already assigned", ignoreCase = true) == true
                            if (source.source == OrganizerMvpDataSource.BACKEND || alreadyAssigned) {
                                loadAssigned(showAlreadyAssignedRefreshFailureIfEmpty = alreadyAssigned)
                            } else {
                                renderAssigned()
                            }
                        }
                    }
                })
            } else {
                addView(ghostButton("Enable/disable staff access") {
                    val index = assignedStaff.indexOfFirst { it.id == staff.id && it.assignedEventId == staff.assignedEventId }
                    if (index >= 0) {
                        val nextStatus = if (assignedStaff[index].accessStatus == "Active") "Disabled" else "Active"
                        val updated = assignedStaff[index].copy(accessStatus = nextStatus)
                        MainScope().launch {
                            val source = repository.updateStaffForMvp(selectedEvent, updated)
                            if (source.source == OrganizerMvpDataSource.BACKEND) {
                                assignedStaff[index] = source.data
                            }
                            source.message?.let {
                                Toast.makeText(this@ManageUsersActivity, it, Toast.LENGTH_SHORT).show()
                            }
                            renderAssigned()
                        }
                    }
                })
                addView(ghostButton("View/edit staff permissions") {
                    AlertDialog.Builder(this@ManageUsersActivity)
                        .setTitle(staff.name)
                        .setMessage("Role: ${staff.roleLabel}\nPermissions: ${staff.permissions.joinToString(", ")}")
                        .setPositiveButton("Close", null)
                        .show()
                })
                addView(ghostButton("Remove staff from event") {
                    AlertDialog.Builder(this@ManageUsersActivity)
                        .setTitle("Remove staff?")
                        .setMessage("Remove ${staff.name} from ${staff.assignedEvent}?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            MainScope().launch {
                                val source = repository.removeStaffForMvp(selectedEvent, staff)
                                if (source.source == OrganizerMvpDataSource.BACKEND) {
                                    assignedStaff.removeAll { it.id == staff.id && it.assignedEventId == staff.assignedEventId }
                                }
                                source.message?.let {
                                    Toast.makeText(this@ManageUsersActivity, it, Toast.LENGTH_SHORT).show()
                                }
                                renderAssigned()
                            }
                        }
                        .show()
                })
            }
        }
}
