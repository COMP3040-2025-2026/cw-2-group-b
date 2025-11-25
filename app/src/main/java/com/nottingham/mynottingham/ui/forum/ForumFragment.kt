package com.nottingham.mynottingham.ui.forum

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.databinding.FragmentForumBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForumViewModel by viewModels()
    private lateinit var tokenManager: TokenManager
    private lateinit var adapter: ForumAdapter
    private lateinit var categoryTabsAdapter: CategoryTabsAdapter

    private var currentCategory: String? = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupHeader()
        setupFab()
        observeViewModel()
        loadPosts()
    }

    private fun setupFab() {
        binding.fabCreatePost.setOnClickListener {
            findNavController().navigate(R.id.action_forum_to_create_post)
        }
    }

    private fun setupRecyclerView() {
        adapter = ForumAdapter(
            onPostClick = { post ->
                // Navigate to post detail
                val bundle = Bundle().apply {
                    // ⚠️ 修复：Firebase ID 是 String 类型
                    putString("postId", post.id)
                }
                findNavController().navigate(R.id.action_forum_to_post_detail, bundle)
            },
            onLikeClick = { post ->
                // ⚠️ 修复：Firebase 实现不需要 Token，只需要 ID
                viewModel.likePost(post.id)
            }
        )

        binding.recyclerPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ForumFragment.adapter
            setHasFixedSize(true) // Items have fixed size, improves performance
            itemAnimator = null // Disable default animations to prevent flickering
        }
    }

    private fun setupHeader() {


        val categories = listOf(
            "All",
            "🔥 Trending",
            "❓ " + com.nottingham.mynottingham.data.model.ForumCategory.QUESTIONS.displayName,
            "📚 " + com.nottingham.mynottingham.data.model.ForumCategory.ACADEMIC.displayName,
            "🎉 " + com.nottingham.mynottingham.data.model.ForumCategory.EVENTS.displayName,
            "💼 " + com.nottingham.mynottingham.data.model.ForumCategory.CAREER.displayName,
            "🍴 " + com.nottingham.mynottingham.data.model.ForumCategory.FOOD.displayName,
            "🏀 " + com.nottingham.mynottingham.data.model.ForumCategory.SPORTS.displayName,
            "💬 " + com.nottingham.mynottingham.data.model.ForumCategory.GENERAL.displayName,
            "📣 " + com.nottingham.mynottingham.data.model.ForumCategory.ANNOUNCEMENTS.displayName,
            "❤️ " + com.nottingham.mynottingham.data.model.ForumCategory.SOCIAL.displayName
        )
        categoryTabsAdapter = CategoryTabsAdapter(categories) { category ->
            currentCategory = when (category) {
                "All" -> null
                "🔥 Trending" -> "TRENDING"
                "❓ " + com.nottingham.mynottingham.data.model.ForumCategory.QUESTIONS.displayName -> com.nottingham.mynottingham.data.model.ForumCategory.QUESTIONS.name
                "📚 " + com.nottingham.mynottingham.data.model.ForumCategory.ACADEMIC.displayName -> com.nottingham.mynottingham.data.model.ForumCategory.ACADEMIC.name
                "🎉 " + com.nottingham.mynottingham.data.model.ForumCategory.EVENTS.displayName -> com.nottingham.mynottingham.data.model.ForumCategory.EVENTS.name
                "💼 " + com.nottingham.mynottingham.data.model.ForumCategory.CAREER.displayName -> com.nottingham.mynottingham.data.model.ForumCategory.CAREER.name
                "🍴 " + com.nottingham.mynottingham.data.model.ForumCategory.FOOD.displayName -> com.nottingham.mynottingham.data.model.ForumCategory.FOOD.name
                "🏀 " + com.nottingham.mynottingham.data.model.ForumCategory.SPORTS.displayName -> com.nottingham.mynottingham.data.model.ForumCategory.SPORTS.name
                "💬 " + com.nottingham.mynottingham.data.model.ForumCategory.GENERAL.displayName -> com.nottingham.mynottingham.data.model.ForumCategory.GENERAL.name
                "📣 " + com.nottingham.mynottingham.data.model.ForumCategory.ANNOUNCEMENTS.displayName -> com.nottingham.mynottingham.data.model.ForumCategory.ANNOUNCEMENTS.name
                "❤️ " + com.nottingham.mynottingham.data.model.ForumCategory.SOCIAL.displayName -> com.nottingham.mynottingham.data.model.ForumCategory.SOCIAL.name
                else -> null
            }
            viewModel.filterByCategory(currentCategory)
            categoryTabsAdapter.setSelectedCategory(category)
        }

        binding.categoryTabsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryTabsAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collect { posts ->
                Log.d("ForumFragment", "Received ${posts.size} posts")
                adapter.submitList(posts)
                binding.layoutEmpty.isVisible = posts.isEmpty() && viewModel.loading.value == false
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                // 暂时忽略错误显示，避免频繁弹窗
                viewModel.clearError()
            }
        }
    }

    private fun loadPosts(refresh: Boolean = true) {
        lifecycleScope.launch {
            val token = tokenManager.getToken().first() ?: ""
            // Firebase 实际上不需要这个 token 来加载公开帖子
            viewModel.loadPosts(token, if (currentCategory == "All") null else currentCategory, refresh)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}