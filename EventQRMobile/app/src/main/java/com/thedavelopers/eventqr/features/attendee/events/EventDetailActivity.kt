package com.thedavelopers.eventqr.features.attendee

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.features.events.model.dto.AttendeeEventResponse
import com.thedavelopers.eventqr.features.events.model.dto.EventAvailabilityResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

open class EventDetailActivity : AppCompatActivity(), EventDetailContract.View {
    private lateinit var repository: AttendeeRepository
    private lateinit var presenter: EventDetailPresenter
    private lateinit var eventId: String
    private var currentEvent: AttendeeEventResponse? = null
    private var isAlreadyRegistered = false

    private val manilaZoneId: ZoneId = ZoneId.of("Asia/Manila")
    private val manilaDateTimeFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("MMM d, yyyy • h:mm a")
        .withZone(manilaZoneId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        repository = AttendeeRepository(this)
        presenter = EventDetailPresenter(this, repository)
        eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.txtDetailTitle).text = intent.getStringExtra(EXTRA_EVENT_TITLE).orEmpty()
        findViewById<TextView>(R.id.txtDetailDescription).text = intent.getStringExtra(EXTRA_EVENT_DESCRIPTION).orEmpty()
        findViewById<TextView>(R.id.txtDetailVenue).text = intent.getStringExtra(EXTRA_EVENT_LOCATION).orEmpty().ifBlank { "Location not specified" }
        findViewById<TextView>(R.id.txtTagCategory).text = intent.getStringExtra(EXTRA_EVENT_CATEGORY).orEmpty().ifBlank { "Technology" }
        findViewById<TextView>(R.id.txtDetailStart).text = intent.getStringExtra(EXTRA_EVENT_START).orEmpty()
        findViewById<TextView>(R.id.txtDetailStatus).text = intent.getStringExtra(EXTRA_EVENT_STATUS).orEmpty()
        findViewById<TextView>(R.id.txtDetailCapacity).text = "${intent.getStringExtra(EXTRA_EVENT_COUNT).orEmpty()} / ${intent.getStringExtra(EXTRA_EVENT_CAPACITY).orEmpty()} Registered"

        findViewById<Button>(R.id.btnRegisterForEvent).apply {
            isEnabled = false
            text = "Loading..."
            setBackgroundResource(R.drawable.bg_disabled_button)
        }

        findViewById<View>(R.id.layoutDetailCategory)?.visibility = View.GONE
        findViewById<View>(R.id.layoutDetailRewards)?.visibility = View.GONE
        findViewById<View>(R.id.layoutDetailAgenda)?.visibility = View.GONE

        findViewById<Button>(R.id.btnViewRewards).setOnClickListener {
            startActivity(Intent(this, AttendeeRewardsActivity::class.java).putExtra(EXTRA_EVENT_ID, eventId))
        }

        findViewById<Button>(R.id.btnRegisterForEvent).setOnClickListener {
            currentEvent?.let { event ->
                presenter.registerForEvent(eventId, event.title)
            } ?: presenter.registerForEvent(eventId, intent.getStringExtra(EXTRA_EVENT_TITLE).orEmpty())
        }

        if (eventId.isNotBlank()) {
            presenter.loadEventDetails(eventId)
        } else {
            showMessage("Missing event information.")
        }
    }

    override fun renderEvent(event: AttendeeEventResponse) {
        currentEvent = event
        findViewById<TextView>(R.id.txtDetailTitle).text = event.title
        findViewById<TextView>(R.id.txtDetailDescription).text = event.description?.takeIf { it.isNotBlank() } ?: "No event description provided."
        findViewById<TextView>(R.id.txtDetailVenue).text = event.location?.takeIf { it.isNotBlank() } ?: "Location not specified."

        val startStr = formatInstantManila(event.eventStartAt)
        val endStr = if (event.eventEndAt != null) " - ${formatInstantManila(event.eventEndAt)}" else ""
        findViewById<TextView>(R.id.txtDetailStart).text = if (event.eventStartAt != null) "$startStr$endStr" else "Date and time not specified."

        findViewById<TextView>(R.id.txtDetailCapacity).text = "${event.currentAttendeeCount} / ${event.capacity} Registered"

        if (!event.category.isNullOrBlank()) {
            findViewById<View>(R.id.layoutDetailCategory)?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.txtTagCategory).text = event.category
        } else {
            findViewById<View>(R.id.layoutDetailCategory)?.visibility = View.GONE
        }

        findViewById<View>(R.id.layoutDetailRewards)?.visibility = if (event.rewardsEnabled) View.VISIBLE else View.GONE

        loadEventAvailability(event)
    }

    private fun loadEventAvailability(event: AttendeeEventResponse) {
        lifecycleScope.launch {
            when (val result = repository.getEventAvailability(event.eventId.toString())) {
                is NetworkResult.Success -> {
                    updateRegisterButtonFromAvailability(event, result.data)
                }
                is NetworkResult.Error -> {
                    // Fallback path: backend registration validation remains the final gate.
                    updateRegisterButtonWithFallback(event, result.message.ifBlank { "Availability unavailable" })
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun updateRegisterButtonFromAvailability(event: AttendeeEventResponse, availability: EventAvailabilityResponse) {
        val btn = findViewById<Button>(R.id.btnRegisterForEvent)

        if (isAlreadyRegistered) {
            setAlreadyRegisteredState(btn)
            logRegistrationWindow(event, availability, "Already Registered", false)
            return
        }

        if (availability.available) {
            btn.isEnabled = true
            btn.text = "Register"
            btn.setBackgroundResource(R.drawable.bg_eventqr_gradient)
            logRegistrationWindow(event, availability, availability.message, true)
            return
        }

        btn.isEnabled = false
        btn.text = availability.message.ifBlank { "Registration Unavailable" }
        btn.setBackgroundResource(R.drawable.bg_disabled_button)
        logRegistrationWindow(event, availability, availability.message, false)
    }

    private fun updateRegisterButtonWithFallback(event: AttendeeEventResponse, message: String) {
        val btn = findViewById<Button>(R.id.btnRegisterForEvent)

        if (isAlreadyRegistered) {
            setAlreadyRegisteredState(btn)
            logRegistrationWindow(event, null, "Already Registered", false)
            return
        }

        btn.isEnabled = true
        btn.text = "Register"
        btn.setBackgroundResource(R.drawable.bg_eventqr_gradient)
        logRegistrationWindow(event, null, "Availability endpoint failed: $message", true)
    }

    override fun updateRegistrationStatus(isRegistered: Boolean) {
        isAlreadyRegistered = isRegistered
        if (isRegistered) {
            val btn = findViewById<Button>(R.id.btnRegisterForEvent)
            setAlreadyRegisteredState(btn)

            if (currentEvent?.rewardsEnabled == true) {
                findViewById<View>(R.id.btnViewRewards)?.visibility = View.VISIBLE
            }
        }
    }

    override fun showLoading(isLoading: Boolean) = Unit

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun openRegistration(eventId: String, eventTitle: String, email: String, fullName: String) {
        startActivity(
            Intent(this, AttendeeRegistrationActivity::class.java)
                .putExtra(EXTRA_EVENT_ID, eventId)
                .putExtra(EXTRA_EVENT_TITLE, eventTitle)
                .putExtra(EXTRA_PREFILL_EMAIL, email)
                .putExtra(EXTRA_PREFILL_FULL_NAME, fullName)
        )
    }

    override fun getSessionUserId(): String? = SessionManager(this).getUserId()
    override fun getSessionEmail(): String = SessionManager(this).getEmail().orEmpty()
    override fun getSessionFullName(): String = SessionManager(this).getFullName().orEmpty()

    private fun setAlreadyRegisteredState(button: Button) {
        button.isEnabled = false
        button.text = "Already Registered"
        button.setBackgroundResource(R.drawable.bg_disabled_button)
    }

    private fun formatInstantManila(value: Instant?): String {
        return if (value == null) "-" else manilaDateTimeFormatter.format(value)
    }

    private fun logRegistrationWindow(
        event: AttendeeEventResponse,
        availability: EventAvailabilityResponse?,
        availabilityMessage: String,
        finalButtonEnabled: Boolean,
    ) {
        val finalButtonLabel = findViewById<Button>(R.id.btnRegisterForEvent).text?.toString().orEmpty()
        Log.d(
            "EventRegistrationWindow",
            "eventId=${event.eventId}," +
                " deviceNow=${Instant.now()}," +
                " serverNow=${availability?.serverNow}," +
                " zoneUsed=Asia/Manila," +
                " registrationOpenAt=${availability?.registrationOpenAt ?: event.registrationOpenAt}," +
                " registrationCloseAt=${availability?.registrationCloseAt ?: event.registrationCloseAt}," +
                " eventStartAt=${event.eventStartAt}," +
                " eventEndAt=${event.eventEndAt}," +
                " availabilityMessage=$availabilityMessage," +
                " finalButtonState=enabled:$finalButtonEnabled,text:$finalButtonLabel"
        )
    }
}
