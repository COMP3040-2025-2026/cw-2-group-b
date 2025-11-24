package com.nottingham.mynottingham.ui.forum

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.ForumComment
import com.nottingham.mynottingham.databinding.ItemForumCommentBinding
import com.nottingham.mynottingham.util.AvatarUtils

class ForumCommentAdapter(
    private val onLikeClick: (ForumComment) -> Unit
) : ListAdapter<ForumComment, ForumCommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

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

        fun bind(comment: ForumComment) {
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
                     // 使用 AvatarUtils 获取资源 ID 或者加载 URL
                     val avatarResId = AvatarUtils.getDrawableId(comment.authorAvatar)
                     ivAuthorAvatar.setImageResource(avatarResId)
                } else {
                    ivAuthorAvatar.setImageResource(R.drawable.ic_profile)
                }

                // Like icon state
                if (comment.isLiked) {
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

    class CommentDiffCallback : DiffUtil.ItemCallback<ForumComment>() {
        override fun areItemsTheSame(oldItem: ForumComment, newItem: ForumComment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ForumComment, newItem: ForumComment): Boolean {
            return oldItem == newItem
        }
    }
}
