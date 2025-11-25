package com.nottingham.mynottingham.ui.forum

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.ForumPost
import com.nottingham.mynottingham.databinding.FragmentForumDetailBinding
import com.nottingham.mynottingham.util.AvatarUtils
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ForumDetailFragment : Fragment() {

    private var _binding: FragmentForumDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForumDetailViewModel by viewModels()
    private lateinit var tokenManager: TokenManager
    private lateinit var commentAdapter: ForumCommentAdapter

    // ⚠️ 修复：ID 类型改为 String
    private var postId: String = ""
    private var currentUserId: String = ""
    private var currentPost: ForumPost? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumDetailBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get postId from arguments (handle both String and Long for compatibility)
        arguments?.let {
            postId = it.getString("postId") ?: it.getLong("postId", 0L).toString()
        }

        if (postId == "0" || postId.isEmpty()) {
            Toast.makeText(context, "Invalid Post ID", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        // Get current user ID
        lifecycleScope.launch {
            currentUserId = tokenManager.getUserId().first() ?: ""
        }

        setupUI()
        setupObservers()
        loadData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnMoreOptions.setOnClickListener { showPostOptionsMenu() }

        commentAdapter = ForumCommentAdapter { comment ->
            // Like comment
            viewModel.likeComment(comment.id)
        }
        binding.recyclerComments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentAdapter
        }

        binding.btnSendComment.setOnClickListener {
            val content = binding.etComment.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.sendComment(postId, content)
                binding.etComment.text.clear()
                hideKeyboard()
            }
        }

        binding.layoutLike.setOnClickListener {
            viewModel.likePost(postId)
        }
    }

    private fun setupObservers() {
        // 使用 viewLifecycleOwner.lifecycleScope 确保在 View 销毁时取消协程
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPostFlow(0L) // 参数为了兼容保留，实际 ViewModel 内部用 StateFlow
                .collect { post ->
                    post?.let {
                        if (_binding != null) {
                            bindPostData(it)
                        }
                    }
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getCommentsFlow(0L) // 同上
                .collect { comments ->
                    if (_binding != null) {
                        commentAdapter.submitList(comments)
                    }
                }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            _binding?.progressBar?.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.commentSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                viewModel.clearCommentSuccess()
            }
        }

        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                viewModel.clearDeleteSuccess()
                findNavController().navigateUp()
            }
        }
    }

    private fun showPostOptionsMenu() {
        val popupMenu = PopupMenu(requireContext(), binding.btnMoreOptions)
        popupMenu.menuInflater.inflate(R.menu.menu_forum_post_options, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit_post -> {
                    navigateToEditPost()
                    true
                }
                R.id.action_delete_post -> {
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun navigateToEditPost() {
        currentPost?.let { post ->
            val bundle = Bundle().apply {
                putBoolean("isEditMode", true)
                putString("editPostId", post.id)
                putString("editTitle", post.title)
                putString("editContent", post.content)
                putString("editCategory", post.category)
                putString("editTags", post.tags?.joinToString(", "))
                putBoolean("editIsPinned", post.isPinned)
            }
            findNavController().navigate(R.id.action_forumDetail_to_editPost, bundle)
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deletePost(postId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // ⚠️ 修复：接收 ForumPost 数据模型，而非 Entity
    private fun bindPostData(post: ForumPost) {
        currentPost = post
        binding.apply {
            tvAuthorName.text = post.authorName
            tvTimestamp.text = DateUtils.getRelativeTimeSpanString(
                post.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
            tvTitle.text = post.title
            tvContent.text = post.content
            tvLikes.text = post.likes.toString()
            tvViews.text = post.views.toString()

            chipCategory.text = post.category
            // Set category chip background color to match forum list
            chipCategory.setChipBackgroundColorResource(getCategoryColor(post.category))
            chipCategory.setTextColor(requireContext().getColor(android.R.color.white))

            // Avatar
            if (!post.authorAvatar.isNullOrEmpty()) {
                 val avatarResId = AvatarUtils.getDrawableId(post.authorAvatar)
                 ivAuthorAvatar.setImageResource(avatarResId)
            }

            // Post Image
            if (!post.imageUrl.isNullOrEmpty()) {
                ivPostImage.isVisible = true
                Glide.with(requireContext())
                    .load(post.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(ivPostImage)
            } else {
                ivPostImage.isVisible = false
            }

            // Tags
            if (!post.tags.isNullOrEmpty()) {
                chipGroupTags.isVisible = true
                chipGroupTags.removeAllViews()
                post.tags.forEach { tag ->
                    val chip = Chip(requireContext()).apply {
                        text = tag
                        textSize = 11f
                        chipMinHeight = 24f * resources.displayMetrics.density
                        isClickable = false
                        setChipBackgroundColorResource(R.color.chip_background)
                    }
                    chipGroupTags.addView(chip)
                }
            } else {
                chipGroupTags.isVisible = false
            }

            // Like Status
            if (post.isLiked) {
                ivLike.setImageResource(R.drawable.ic_favorite)
                ivLike.setColorFilter(requireContext().getColor(R.color.primary))
            } else {
                ivLike.setImageResource(R.drawable.ic_favorite_border)
                ivLike.clearColorFilter()
            }

            // Show/hide more options button based on author
            btnMoreOptions.isVisible = (currentUserId == post.authorId)
        }
    }

    private fun loadData() {
        viewModel.loadPostDetail(postId)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getCategoryColor(category: String): Int {
        return when (category.uppercase()) {
            "ACADEMIC" -> R.color.category_academic
            "EVENTS" -> R.color.category_events
            "SPORTS" -> R.color.category_sports
            "SOCIAL" -> R.color.category_social
            "ANNOUNCEMENTS" -> R.color.category_announcements
            "CAREER" -> R.color.category_career
            "FOOD" -> R.color.category_food
            "QUESTIONS" -> R.color.category_questions
            else -> R.color.category_general
        }
    }
}
