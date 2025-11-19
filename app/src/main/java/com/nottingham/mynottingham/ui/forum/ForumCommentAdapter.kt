package com.nottingham.mynottingham.ui.forum

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.database.entities.ForumCommentEntity
import com.nottingham.mynottingham.databinding.ItemForumCommentBinding
import com.nottingham.mynottingham.util.Constants

class ForumCommentAdapter(
    private val onLikeClick: (ForumCommentEntity) -> Unit
) : ListAdapter<ForumCommentEntity, ForumCommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemForumCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(private val binding: ItemForumCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: ForumCommentEntity) {
            binding.apply {
                tvAuthorName.text = comment.authorName
                tvContent.text = comment.content
                tvLikes.text = comment.likes.toString()

                // Timestamp
                tvTimestamp.text = DateUtils.getRelativeTimeSpanString(
                    comment.createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )

                // Avatar
                if (!comment.authorAvatar.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(Constants.BASE_URL + comment.authorAvatar)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(ivAuthorAvatar)
                } else {
                    ivAuthorAvatar.setImageResource(R.drawable.ic_profile)
                }

                // Like icon state
                if (comment.isLikedByCurrentUser) {
                    ivLike.setImageResource(R.drawable.ic_favorite)
                    ivLike.setColorFilter(itemView.context.getColor(R.color.primary))
                } else {
                    ivLike.setImageResource(R.drawable.ic_favorite_border)
                    ivLike.clearColorFilter()
                }

                // Like click listener
                ivLike.setOnClickListener {
                    onLikeClick(comment)
                }
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<ForumCommentEntity>() {
        override fun areItemsTheSame(oldItem: ForumCommentEntity, newItem: ForumCommentEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ForumCommentEntity, newItem: ForumCommentEntity): Boolean {
            return oldItem == newItem
        }
    }
}
