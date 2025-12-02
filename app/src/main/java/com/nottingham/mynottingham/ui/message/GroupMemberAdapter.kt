package com.nottingham.mynottingham.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.GroupMember
import com.nottingham.mynottingham.data.model.GroupRole
import com.nottingham.mynottingham.databinding.ItemGroupMemberBinding
import com.nottingham.mynottingham.util.AvatarUtils

/**
 * Adapter for group member list
 */
class GroupMemberAdapter(
    private val currentUserId: String,
    private val currentUserRole: GroupRole,
    private val onMemberClick: (GroupMember) -> Unit,
    private val onSetAdmin: (GroupMember) -> Unit,
    private val onRemoveAdmin: (GroupMember) -> Unit,
    private val onRemoveMember: (GroupMember) -> Unit,
    private val onTransferOwnership: (GroupMember) -> Unit
) : ListAdapter<GroupMember, GroupMemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(
        private val binding: ItemGroupMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: GroupMember) {
            binding.apply {
                // Set avatar
                ivAvatar.setImageResource(AvatarUtils.getDrawableId(member.avatar))

                // Set name
                tvName.text = member.name

                // Set role badge
                when (member.role) {
                    GroupRole.OWNER -> {
                        tvRoleBadge.visibility = View.VISIBLE
                        tvRoleBadge.text = "Owner"
                        tvRoleBadge.setBackgroundResource(R.drawable.bg_role_owner)
                    }
                    GroupRole.ADMIN -> {
                        tvRoleBadge.visibility = View.VISIBLE
                        tvRoleBadge.text = "Admin"
                        tvRoleBadge.setBackgroundResource(R.drawable.bg_role_admin)
                    }
                    GroupRole.MEMBER -> {
                        tvRoleBadge.visibility = View.GONE
                    }
                }

                // Set info (program/faculty + year)
                val info = buildString {
                    member.program?.let { append(it) }
                    member.year?.let {
                        if (isNotEmpty()) append(" • ")
                        append("Year $it")
                    }
                    if (isEmpty()) {
                        member.faculty?.let { append(it) }
                    }
                }
                tvInfo.text = info
                tvInfo.visibility = if (info.isNotEmpty()) View.VISIBLE else View.GONE

                // Show more button only if current user has permission
                val canManage = when (currentUserRole) {
                    GroupRole.OWNER -> member.id != currentUserId // Owner can manage anyone except self
                    GroupRole.ADMIN -> member.role == GroupRole.MEMBER && member.id != currentUserId
                    GroupRole.MEMBER -> false
                }
                btnMore.visibility = if (canManage) View.VISIBLE else View.GONE

                // Click listener for viewing profile
                root.setOnClickListener {
                    onMemberClick(member)
                }

                // More button click
                btnMore.setOnClickListener { view ->
                    showPopupMenu(view, member)
                }
            }
        }

        private fun showPopupMenu(view: View, member: GroupMember) {
            val popup = PopupMenu(view.context, view)

            // Add menu items based on current user's role
            when (currentUserRole) {
                GroupRole.OWNER -> {
                    if (member.role == GroupRole.ADMIN) {
                        popup.menu.add(0, 1, 0, "Remove Admin")
                    } else if (member.role == GroupRole.MEMBER) {
                        popup.menu.add(0, 2, 0, "Set as Admin")
                    }
                    popup.menu.add(0, 3, 0, "Transfer Ownership")
                    popup.menu.add(0, 4, 0, "Remove from Group")
                }
                GroupRole.ADMIN -> {
                    popup.menu.add(0, 4, 0, "Remove from Group")
                }
                else -> {}
            }

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> onRemoveAdmin(member)
                    2 -> onSetAdmin(member)
                    3 -> onTransferOwnership(member)
                    4 -> onRemoveMember(member)
                }
                true
            }

            popup.show()
        }
    }
}

class MemberDiffCallback : DiffUtil.ItemCallback<GroupMember>() {
    override fun areItemsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean {
        return oldItem == newItem
    }
}
