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
        observeViewModel()
        loadPosts()
    }

    private fun setupRecyclerView() {
        adapter = ForumAdapter(
            onPostClick = { post ->
                // Navigate to post detail
                val bundle = Bundle().apply {
                    putLong("postId", post.id)
                }
                findNavController().navigate(R.id.action_forum_to_post_detail, bundle)
            },
            onLikeClick = { post ->
                lifecycleScope.launch {
                    val token = tokenManager.getToken().first() ?: ""
                    if (token.isNotEmpty()) {
                        viewModel.likePost(token, post.id)
                    }
                }
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
        // Observe posts from local database
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collect { posts ->
                Log.d("ForumFragment", "Received ${posts.size} posts")
                adapter.submitList(posts)

                // Show/hide empty state
                binding.layoutEmpty.isVisible = posts.isEmpty() && !viewModel.loading.value!!
            }
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e("ForumFragment", "Error: $it")
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun loadPosts(refresh: Boolean = true) {
        lifecycleScope.launch {
            val token = tokenManager.getToken().first() ?: ""
            if (token.isEmpty()) {
                Log.e("ForumFragment", "No token available")
                Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Log.d("ForumFragment", "Loading posts with token: ${token.take(20)}...")
            viewModel.loadPosts(token, if (currentCategory == "All") null else currentCategory, refresh)
        }
    }

    override fun onResume() {
        super.onResume()
        // Don't auto-refresh to prevent flickering
        // Users can use pull-to-refresh if needed
        // loadPosts(refresh = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}