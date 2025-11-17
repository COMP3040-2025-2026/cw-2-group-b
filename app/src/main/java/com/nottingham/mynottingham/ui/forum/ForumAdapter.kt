package com.nottingham.mynottingham.ui.forum

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.database.entities.ForumPostEntity
import com.nottingham.mynottingham.databinding.ItemForumPostBinding
import com.nottingham.mynottingham.util.Constants

/**
 * RecyclerView Adapter for displaying forum posts
 */
class ForumAdapter(
    private val onPostClick: (ForumPostEntity) -> Unit,
    private val onLikeClick: (ForumPostEntity) -> Unit
) : ListAdapter<ForumPostEntity, ForumAdapter.ForumPostViewHolder>(ForumPostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumPostViewHolder {
        val binding = ItemForumPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ForumPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForumPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ForumPostViewHolder(
        private val binding: ItemForumPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: ForumPostEntity) {
            binding.apply {
                // Author info
                tvAuthorName.text = post.authorName

                // Load author avatar
                if (!post.authorAvatar.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(Constants.BASE_URL + post.authorAvatar)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(ivAuthorAvatar)
                } else {
                    ivAuthorAvatar.setImageResource(R.drawable.ic_profile)
                }

                // Timestamp
                tvTimestamp.text = getRelativeTime(post.createdAt)

                // Category badge
                chipCategory.text = post.category
                chipCategory.setChipBackgroundColorResource(getCategoryColor(post.category))

                // Title and content
                tvTitle.text = post.title
                tvContent.text = post.content

                // Post image
                if (!post.imageUrl.isNullOrEmpty()) {
                    ivPostImage.isVisible = true
                    Glide.with(itemView.context)
                        .load(Constants.BASE_URL + post.imageUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .centerCrop()
                        .into(ivPostImage)
                } else {
                    ivPostImage.isVisible = false
                }

                // Tags
                if (!post.tags.isNullOrEmpty()) {
                    chipGroupTags.isVisible = true
                    chipGroupTags.removeAllViews()

                    val tagList = post.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    tagList.forEach { tag ->
                        val chip = Chip(itemView.context).apply {
                            text = tag
                            isClickable = false
                            isCheckable = false
                            setChipBackgroundColorResource(R.color.chip_background)
                            textSize = 11f
                        }
                        chipGroupTags.addView(chip)
                    }
                } else {
                    chipGroupTags.isVisible = false
                }

                // Stats
                tvLikes.text = post.likes.toString()
                tvComments.text = post.comments.toString()
                tvViews.text = post.views.toString()

                // Like icon state
                if (post.isLikedByCurrentUser) {
                    ivLike.setImageResource(R.drawable.ic_favorite)
                } else {
                    ivLike.setImageResource(R.drawable.ic_favorite_border)
                }

                // Pinned and locked indicators
                ivPinned.isVisible = post.isPinned
                ivLocked.isVisible = post.isLocked

                // Click listeners
                root.setOnClickListener { onPostClick(post) }
                ivLike.setOnClickListener {
                    onLikeClick(post)
                    // Prevent double-click
                    it.isEnabled = false
                    it.postDelayed({ it.isEnabled = true }, 1000)
                }
            }
        }

        private fun getRelativeTime(timestamp: Long): String {
            return DateUtils.getRelativeTimeSpanString(
                timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        }

        private fun getCategoryColor(category: String): Int {
            return when (category.uppercase()) {
                "ACADEMIC" -> R.color.category_academic
                "EVENTS" -> R.color.category_events
                "SPORTS" -> R.color.category_sports
                "SOCIAL" -> R.color.category_social
                "ANNOUNCEMENTS" -> R.color.category_announcements
                else -> R.color.category_general
            }
        }
    }

    class ForumPostDiffCallback : DiffUtil.ItemCallback<ForumPostEntity>() {
        override fun areItemsTheSame(oldItem: ForumPostEntity, newItem: ForumPostEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ForumPostEntity, newItem: ForumPostEntity): Boolean {
            return oldItem == newItem
        }
    }
}
