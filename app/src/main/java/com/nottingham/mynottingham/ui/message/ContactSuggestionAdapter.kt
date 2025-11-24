package com.nottingham.mynottingham.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.remote.dto.ContactSuggestionDto
import com.nottingham.mynottingham.databinding.ItemContactSuggestionBinding
import com.nottingham.mynottingham.util.AvatarUtils

/**
 * Adapter for contact suggestions in new message screen
 */
class ContactSuggestionAdapter(
    private val onContactClick: (ContactSuggestionDto) -> Unit
) : ListAdapter<ContactSuggestionDto, ContactSuggestionAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(
        private val binding: ItemContactSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onContactClick(getItem(position))
                }
            }
        }

        fun bind(contact: ContactSuggestionDto) {
            binding.apply {
                // Load avatar
                imageAvatar.setImageResource(AvatarUtils.getDrawableId(contact.userAvatar))

                // Show/hide online indicator
                viewOnlineIndicator.visibility = if (contact.isOnline) View.VISIBLE else View.GONE

                // Set name
                textName.text = contact.userName

                // Set program info
                val programInfo = buildString {
                    contact.program?.let { append(it) }
                    contact.year?.let {
                        if (isNotEmpty()) append(", ")
                        append("Year $it")
                    }
                }

                if (programInfo.isNotEmpty()) {
                    textProgram.text = programInfo
                    textProgram.visibility = View.VISIBLE
                } else {
                    textProgram.visibility = View.GONE
                }
            }
        }
    }
}

class ContactDiffCallback : DiffUtil.ItemCallback<ContactSuggestionDto>() {
    override fun areItemsTheSame(oldItem: ContactSuggestionDto, newItem: ContactSuggestionDto): Boolean {
        return oldItem.userId == newItem.userId
    }

    override fun areContentsTheSame(oldItem: ContactSuggestionDto, newItem: ContactSuggestionDto): Boolean {
        return oldItem == newItem
    }
}
