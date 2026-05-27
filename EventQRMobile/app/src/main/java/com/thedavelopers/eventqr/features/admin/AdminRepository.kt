package com.thedavelopers.eventqr.features.admin

import android.content.Context
import com.thedavelopers.eventqr.core.api.ApiClient
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.safeApiCall
import com.thedavelopers.eventqr.features.events.model.dto.EventRequestDecisionRequest
import com.thedavelopers.eventqr.features.events.model.dto.EventRequestResponse
import com.thedavelopers.eventqr.features.users.model.dto.UserResponse

class AdminRepository(private val context: Context) {
    private val apiService = ApiClient.getService(context)

    suspend fun getCurrentUser(): NetworkResult<UserResponse> = safeApiCall { apiService.getAuthMe() }

    suspend fun loadAllEventRequests(): NetworkResult<List<EventRequestResponse>> =
        safeApiCall { apiService.getAdminEventRequests() }

    suspend fun getEventRequest(requestId: String): NetworkResult<EventRequestResponse> =
        safeApiCall { apiService.getAdminEventRequest(requestId) }

    suspend fun approveEvent(eventId: String, remarks: String?): NetworkResult<EventRequestResponse> {
        return safeApiCall { apiService.approveEventRequest(eventId, EventRequestDecisionRequest(remarks)) }
    }

    suspend fun rejectEvent(eventId: String, remarks: String?): NetworkResult<EventRequestResponse> {
        return safeApiCall { apiService.rejectEventRequest(eventId, EventRequestDecisionRequest(remarks)) }
    }

    suspend fun upgradeOrganizer(eventId: String): NetworkResult<EventRequestResponse> =
        safeApiCall { apiService.upgradeOrganizerFromEventRequest(eventId) }
}
