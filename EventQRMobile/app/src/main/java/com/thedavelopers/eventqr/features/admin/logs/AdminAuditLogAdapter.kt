package com.thedavelopers.eventqr.features.admin.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.audit.model.dto.AuditLogResponse
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AdminAuditLogAdapter : RecyclerView.Adapter<AdminAuditLogAdapter.AdminAuditLogViewHolder>() {
    private val rows = mutableListOf<AuditLogResponse>()
    private val timestampFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(ZoneId.systemDefault())

    fun submitItems(items: List<AuditLogResponse>) {
        rows.clear()
        rows.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminAuditLogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_audit_log, parent, false)
        return AdminAuditLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminAuditLogViewHolder, position: Int) {
        holder.bind(rows[position], timestampFormatter)
    }

    override fun getItemCount(): Int = rows.size

    class AdminAuditLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textActionTitle: TextView = itemView.findViewById(R.id.textAuditActionTitle)
        private val textAuditActorTarget: TextView = itemView.findViewById(R.id.textAuditActorTarget)
        private val textAuditTimestamp: TextView = itemView.findViewById(R.id.textAuditTimestamp)
        private val iconTile: View = itemView.findViewById(R.id.auditIconTile)

        fun bind(item: AuditLogResponse, formatter: DateTimeFormatter) {
            textActionTitle.text = item.action.ifBlank { "Audit entry" }.toFriendlyTitle()
            val actor = item.performedByFullName?.takeIf { it.isNotBlank() } ?: "Admin User"
            val target = item.details?.takeIf { it.isNotBlank() } ?: "System"
            textAuditActorTarget.text = "By $actor · Target: $target"
            textAuditTimestamp.text = formatter.format(item.timestamp)
            bindIconStyle(item.action)
        }

        private fun bindIconStyle(action: String) {
            val normalized = action.lowercase()
            val background = when {
                normalized.contains("approve") || normalized.contains("request") ->
                    R.drawable.bg_admin_role_badge_green
                normalized.contains("account") || normalized.contains("role") || normalized.contains("user") ->
                    R.drawable.bg_admin_role_badge_blue
                normalized.contains("security") || normalized.contains("suspend") ->
                    R.drawable.bg_admin_role_badge_pink
                else -> R.drawable.bg_admin_role_badge_purple
            }
            iconTile.setBackgroundResource(background)
        }

        private fun String.toFriendlyTitle(): String {
            return trim().lowercase().split('_', ' ').joinToString(" ") { token ->
                token.replaceFirstChar { it.uppercase() }
            }
        }
    }
}
