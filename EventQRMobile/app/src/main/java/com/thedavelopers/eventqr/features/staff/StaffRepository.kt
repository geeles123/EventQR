package com.thedavelopers.eventqr.features.staff

import android.content.Context
import com.thedavelopers.eventqr.core.api.ApiClient
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.safeApiCall
import com.thedavelopers.eventqr.features.idprinting.model.dto.IdPrintRequest
import com.thedavelopers.eventqr.features.notifications.model.dto.NotificationResponse
import com.thedavelopers.eventqr.features.registrations.model.dto.RegistrationResponse
import com.thedavelopers.eventqr.features.scanpurposes.model.dto.ScanPurposeRequest
import com.thedavelopers.eventqr.features.scanpurposes.model.dto.ScanPurposeResponse
import com.thedavelopers.eventqr.features.transactions.model.dto.TransactionRequest
import com.thedavelopers.eventqr.features.transactions.model.dto.TransactionResponse

class StaffRepository(context: Context) {
    private val apiService = ApiClient.getService(context)

    suspend fun getEvents() = safeApiCall { apiService.getEvents() }

    suspend fun getScanPurposesByEvent(eventId: String) = safeApiCall { apiService.getScanPurposesByEvent(eventId) }

    suspend fun createTransaction(request: TransactionRequest) = safeApiCall { apiService.createTransaction(request) }

    suspend fun getTransactionsByEvent(eventId: String) = safeApiCall { apiService.getTransactionsByEvent(eventId) }

    suspend fun printId(request: IdPrintRequest) = safeApiCall { apiService.printId(request) }

    suspend fun getIdPrintsByEvent(eventId: String) = safeApiCall { apiService.getIdPrintsByEvent(eventId) }

    suspend fun getRegistrationsByEvent(eventId: String) = safeApiCall { apiService.getRegistrationsByEvent(eventId) }

    suspend fun getNotificationsByRecipient(recipientUserId: String) = safeApiCall { apiService.getNotificationsByRecipient(recipientUserId) }
}