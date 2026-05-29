package com.thedavelopers.eventqr.features.admin.users

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.dto.AccountStatus
import com.thedavelopers.eventqr.features.users.model.dto.UserResponse

class AdminAccountAdapter : RecyclerView.Adapter<AdminAccountAdapter.AdminAccountViewHolder>() {
    private val items = mutableListOf<UserResponse>()

    fun submitItems(users: List<UserResponse>) {
        items.clear()
        items.addAll(users)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminAccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_account, parent, false)
        return AdminAccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminAccountViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AdminAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textAvatar: TextView = itemView.findViewById(R.id.textAccountAvatar)
        private val textName: TextView = itemView.findViewById(R.id.textAccountName)
        private val textEmail: TextView = itemView.findViewById(R.id.textAccountEmail)
        private val textRole: TextView = itemView.findViewById(R.id.textAccountRoleBadge)
        private val textStatus: TextView = itemView.findViewById(R.id.textAccountStatusBadge)

        fun bind(user: UserResponse) {
            textAvatar.text = user.fullName.trim().take(1).uppercase().ifBlank { "U" }
            textName.text = user.fullName
            textEmail.text = user.email
            textRole.text = formatRole(user.role.name)
            textStatus.text = formatStatus(user.status)
            bindRoleStyle(user.role.name)
            bindStatusStyle(user.status)
        }

        private fun formatRole(role: String): String {
            return role.lowercase().replace('_', ' ').split(' ').joinToString(" ") { token ->
                token.replaceFirstChar { it.uppercase() }
            }
        }

        private fun formatStatus(status: AccountStatus): String {
            val normalized = status.name.lowercase().replace('_', ' ')
            return normalized.replaceFirstChar { it.uppercase() }
        }

        private fun bindRoleStyle(role: String) {
            val normalized = role.uppercase()
            val background = when {
                normalized.contains("ADMIN") -> R.drawable.bg_admin_role_badge_pink
                normalized.contains("ORGANIZER") -> R.drawable.bg_admin_role_badge_blue
                normalized.contains("STAFF") -> R.drawable.bg_admin_role_badge_green
                else -> R.drawable.bg_admin_role_badge_purple
            }
            textRole.setBackgroundResource(background)
        }

        private fun bindStatusStyle(status: AccountStatus) {
            when (status) {
                AccountStatus.ACTIVE -> {
                    textStatus.setBackgroundResource(R.drawable.bg_admin_status_active_badge)
                    textStatus.setTextColor(0xFF065F46.toInt())
                }
                AccountStatus.PENDING -> {
                    textStatus.setBackgroundResource(R.drawable.bg_admin_status_pending_badge)
                    textStatus.setTextColor(0xFF92400E.toInt())
                }
                else -> {
                    textStatus.setBackgroundResource(R.drawable.bg_admin_status_inactive_badge)
                    textStatus.setTextColor(0xFF991B1B.toInt())
                }
            }
        }
    }
}
