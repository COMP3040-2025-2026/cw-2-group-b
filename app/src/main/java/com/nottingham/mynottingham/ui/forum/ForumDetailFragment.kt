package com.nottingham.mynottingham.ui.forum

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
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
import com.nottingham.mynottingham.data.local.database.entities.ForumPostEntity
import com.nottingham.mynottingham.databinding.FragmentForumDetailBinding
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ForumDetailFragment : Fragment() {

    private var _binding: FragmentForumDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForumDetailViewModel by viewModels()
    private lateinit var tokenManager: TokenManager
    private lateinit var commentAdapter: ForumCommentAdapter

    private var postId: Long = 0L

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

        // Get postId from arguments
        arguments?.let {
            postId = it.getLong("postId")
        }

        if (postId == 0L) {
            Toast.makeText(context, "Invalid Post ID", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupUI()
        setupObservers()
        loadData()
    }

    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Comments RecyclerView
        commentAdapter = ForumCommentAdapter { comment ->
            lifecycleScope.launch {
                val token = tokenManager.getToken().first() ?: ""
                if (token.isNotEmpty()) {
                    viewModel.likeComment(token, comment.id)
                } else {
                    Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.recyclerComments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentAdapter
        }

        // Send Comment
        binding.btnSendComment.setOnClickListener {
            val content = binding.etComment.text.toString().trim()
            if (content.isNotEmpty()) {
                lifecycleScope.launch {
                    val token = tokenManager.getToken().first() ?: ""
                    if (token.isNotEmpty()) {
                        viewModel.sendComment(token, postId, content)
                        binding.etComment.text.clear()
                        hideKeyboard()
                    }
                }
            }
        }

        // Like Post
        binding.layoutLike.setOnClickListener {
            lifecycleScope.launch {
                val token = tokenManager.getToken().first() ?: ""
                if (token.isNotEmpty()) {
                    viewModel.likePost(token, postId)
                }
            }
        }
    }

    private fun setupObservers() {
        // Observe Post Data
        lifecycleScope.launch {
            viewModel.getPostFlow(postId).collect { post ->
                post?.let { bindPostData(it) }
            }
        }

        // Observe Comments
        lifecycleScope.launch {
            viewModel.getCommentsFlow(postId).collect { comments ->
                commentAdapter.submitList(comments)
            }
        }

        // Loading State
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        // Error State
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Comment Success
        viewModel.commentSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                viewModel.clearCommentSuccess()
                // Refresh data to ensure everything is synced
                lifecycleScope.launch {
                    val token = tokenManager.getToken().first() ?: ""
                    if (token.isNotEmpty()) viewModel.loadPostDetail(token, postId)
                }
            }
        }
    }

    private fun bindPostData(post: ForumPostEntity) {
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
            // Set chip color based on category
            chipCategory.setChipBackgroundColorResource(
                when (post.category) {
                    "ACADEMIC" -> R.color.category_academic
                    "EVENTS" -> R.color.category_events
                    "SPORTS" -> R.color.category_sports
                    "SOCIAL" -> R.color.category_social
                    else -> R.color.category_general
                }
            )

            // Avatar
            if (!post.authorAvatar.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(Constants.BASE_URL + post.authorAvatar)
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivAuthorAvatar)
            }

            // Post Image
            if (!post.imageUrl.isNullOrEmpty()) {
                ivPostImage.isVisible = true
                Glide.with(requireContext())
                    .load(Constants.BASE_URL + post.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(ivPostImage)
            } else {
                ivPostImage.isVisible = false
            }

            // Like Status
            if (post.isLikedByCurrentUser) {
                ivLike.setImageResource(R.drawable.ic_favorite)
                ivLike.setColorFilter(requireContext().getColor(R.color.primary))
            } else {
                ivLike.setImageResource(R.drawable.ic_favorite_border)
                ivLike.clearColorFilter()
            }

            // Tags
            chipGroupTags.removeAllViews()
            post.tags?.split(",")?.forEach { tag ->
                if (tag.isNotBlank()) {
                    val chip = Chip(context).apply {
                        text = tag.trim()
                        isClickable = false
                    }
                    chipGroupTags.addView(chip)
                }
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val token = tokenManager.getToken().first() ?: ""
            if (token.isNotEmpty()) {
                viewModel.loadPostDetail(token, postId)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
