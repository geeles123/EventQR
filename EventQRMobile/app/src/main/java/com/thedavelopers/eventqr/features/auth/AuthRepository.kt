package com.thedavelopers.eventqr.features.auth

import android.content.Context
import com.thedavelopers.eventqr.core.api.ApiClient
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.dto.AccountRole
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.features.auth.model.dto.LoginRequest
import com.thedavelopers.eventqr.features.auth.model.dto.LoginResponse
import com.thedavelopers.eventqr.features.users.model.dto.UserRequest
import com.thedavelopers.eventqr.features.users.model.dto.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(context: Context) {
    private val apiService = ApiClient.getService(context)
    private val sessionManager = SessionManager(context)

    suspend fun login(email: String): NetworkResult<LoginResponse> = withContext(Dispatchers.IO) {
        runCatching {
            apiService.login(LoginRequest(email))
        }.fold(
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    NetworkResult.Success(response.data, response.message)
                } else {
                    NetworkResult.Error(response.message ?: "Login failed")
                }
            },
            onFailure = { throwable ->
                NetworkResult.Error(throwable.message ?: "Login failed", throwable)
            }
        )
    }

    suspend fun createUser(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        role: AccountRole = AccountRole.ATTENDEE,
    ): NetworkResult<UserResponse> = withContext(Dispatchers.IO) {
        val fullName = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ").trim()
        runCatching {
            apiService.createUser(
                UserRequest(
                    email = email,
                    fullName = fullName,
                    phoneNumber = phoneNumber.ifBlank { null },
                    role = role,
                )
            )
        }.fold(
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    NetworkResult.Success(response.data, response.message)
                } else {
                    NetworkResult.Error(response.message ?: "Account creation failed")
                }
            },
            onFailure = { throwable ->
                NetworkResult.Error(throwable.message ?: "Account creation failed", throwable)
            }
        )
    }

    fun storeSession(loginResponse: LoginResponse) {
        sessionManager.saveLoginResponse(loginResponse)
    }

    fun saveUserRole(role: AccountRole?) {
        sessionManager.saveRole(role)
    }
}
