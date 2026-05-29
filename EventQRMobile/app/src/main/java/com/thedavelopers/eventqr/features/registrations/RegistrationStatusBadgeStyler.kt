package com.thedavelopers.eventqr.features.registrations

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.dto.RegistrationStatus
import java.util.Locale

object RegistrationStatusBadgeStyler {

    fun bind(view: TextView, status: RegistrationStatus) {
        val style = when (status) {
            RegistrationStatus.REGISTERED -> BadgeStyle(
                backgroundResId = R.drawable.bg_badge_registered,
                textColorResId = R.color.eventqr_badge_registered_text,
            )
            RegistrationStatus.ENTERED -> BadgeStyle(
                backgroundResId = R.drawable.bg_badge_entered,
                textColorResId = R.color.eventqr_badge_entered_text,
            )
            RegistrationStatus.EXITED -> BadgeStyle(
                backgroundResId = R.drawable.bg_badge_exited,
                textColorResId = R.color.eventqr_badge_exited_text,
            )
            RegistrationStatus.CANCELLED -> BadgeStyle(
                backgroundResId = R.drawable.bg_badge_cancelled,
                textColorResId = R.color.eventqr_badge_cancelled_text,
            )
            RegistrationStatus.NO_SHOW -> BadgeStyle(
                backgroundResId = R.drawable.bg_badge_cancelled,
                textColorResId = R.color.eventqr_badge_cancelled_text,
            )
        }

        view.text = status.name.replace('_', ' ').uppercase(Locale.US)
        view.setBackgroundResource(style.backgroundResId)
        view.setTextColor(ContextCompat.getColor(view.context, style.textColorResId))
    }

    private data class BadgeStyle(
        @DrawableRes val backgroundResId: Int,
        @ColorRes val textColorResId: Int,
    )
}