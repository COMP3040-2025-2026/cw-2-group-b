package com.nottingham.mynottingham.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.Contact
import com.nottingham.mynottingham.databinding.ItemContactSelectableBinding

/**
 * Adapter for selectable contacts (multi-select for group creation)
 */
class SelectableContactAdapter(
    private val onSelectionChanged: (Set<Contact>) -> Unit
) : ListAdapter<Contact, SelectableContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    private val selectedContacts = mutableSetOf<Contact>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactSelectableBinding.inflate(
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
        private val binding: ItemContactSelectableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val contact = getItem(position)
                    toggleSelection(contact)
                }
            }
        }

        fun bind(contact: Contact) {
            binding.apply {
                // Set avatar initials
                val initials = contact.name.split(" ")
                    .take(2)
                    .map { it.firstOrNull()?.uppercase() ?: "" }
                    .joinToString("")
                tvAvatar.text = initials

                // Set name
                tvName.text = contact.name

                // Set program/department info
                val programText = when {
                    contact.program != null && contact.year != null -> {
                        "${contact.program}, Year ${contact.year}"
                    }
                    contact.program != null -> contact.program
                    else -> "Faculty Member"
                }
                tvProgram.text = programText

                // Set online status
                viewOnline.visibility = if (contact.isOnline) View.VISIBLE else View.GONE

                // Set checkbox state
                checkboxSelect.isChecked = selectedContacts.contains(contact)
            }
        }
    }

    private fun toggleSelection(contact: Contact) {
        if (selectedContacts.contains(contact)) {
            selectedContacts.remove(contact)
        } else {
            selectedContacts.add(contact)
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedContacts)
    }

    fun getSelectedContacts(): Set<Contact> = selectedContacts.toSet()

    fun clearSelection() {
        selectedContacts.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedContacts)
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}
