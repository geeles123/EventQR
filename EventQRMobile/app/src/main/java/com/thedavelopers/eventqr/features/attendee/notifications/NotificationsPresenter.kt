package com.thedavelopers.eventqr.features.attendee

import com.thedavelopers.eventqr.core.api.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationsPresenter(
    private var view: NotificationsContract.View?,
    private val repository: AttendeeRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun detach() {
        scope.cancel()
        view = null
    }

    fun load() {
        view?.showLoading(true)
        view?.setMarkAllEnabled(false)
        scope.launch {
            when (val result = repository.getMyNotifications()) {
                is NetworkResult.Success -> {
                    view?.showLoading(false)
                    view?.showContent()
                    view?.setMarkAllEnabled(result.data.isNotEmpty())
                    view?.renderNotifications(result.data)
                }
                is NetworkResult.Error -> {
                    view?.showLoading(false)
                    view?.setMarkAllEnabled(false)
                    view?.showError(result.message.ifBlank { "Unable to load notifications." })
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun markAllRead() {
        view?.setMarkAllEnabled(false)
        scope.launch {
            when (val result = repository.markAllNotificationsRead()) {
                is NetworkResult.Success -> load()
                is NetworkResult.Error -> {
                    view?.setMarkAllEnabled(true)
                    view?.showMessage(result.message.ifBlank { "Unable to mark notifications as read." })
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun markRead(notificationId: String) {
        scope.launch {
            when (val result = repository.markNotificationRead(notificationId)) {
                is NetworkResult.Success -> load()
                is NetworkResult.Error -> {
                    view?.showMessage(result.message.ifBlank { "Unable to mark notification as read." })
                }
                NetworkResult.Loading -> Unit
            }
        }
    }
}
