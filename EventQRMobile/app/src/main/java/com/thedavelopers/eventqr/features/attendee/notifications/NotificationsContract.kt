package com.thedavelopers.eventqr.features.attendee

import com.thedavelopers.eventqr.features.notifications.model.dto.NotificationResponse

interface NotificationsContract {
    interface View : AttendeeView {
        fun showContent()
        fun showError(message: String)
        fun renderNotifications(items: List<NotificationResponse>)
        fun setMarkAllEnabled(enabled: Boolean)
    }
}
