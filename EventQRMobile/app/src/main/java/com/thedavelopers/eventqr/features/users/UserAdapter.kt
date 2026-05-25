package com.thedavelopers.eventqr.features.users

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.users.model.dto.UserResponse

class UserAdapter(
    private val onRoleClick: (UserResponse) -> Unit,
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    private val items = mutableListOf<UserResponse>()

    fun submitItems(newItems: List<UserResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.txtUserTitle)
        private val detailView: TextView = itemView.findViewById(R.id.txtUserDetails)
        private val roleButton: TextView = itemView.findViewById(R.id.txtUserChangeRole)

        fun bind(item: UserResponse) {
            titleView.text = item.fullName
            detailView.text = buildString {
                append(item.email)
                append("\nRole: ")
                append(item.role.name)
                append("\nStatus: ")
                append(item.status.name)
            }
            roleButton.setOnClickListener { onRoleClick(item) }
        }
    }
}