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
import com.google.android.material.chip.Chip
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

    private var currentCategory: String? = null

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
        setupCategoryFilter()
        setupFab()
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
            setHasFixedSize(true)
        }
    }

    private fun setupCategoryFilter() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val checkedChip = group.findViewById<Chip>(checkedIds.first())
            currentCategory = when (checkedChip.id) {
                R.id.chip_all -> null
                R.id.chip_academic -> "ACADEMIC"
                R.id.chip_events -> "EVENTS"
                R.id.chip_sports -> "SPORTS"
                R.id.chip_social -> "SOCIAL"
                else -> null
            }

            Log.d("ForumFragment", "Category filter changed to: $currentCategory")
            viewModel.filterByCategory(currentCategory)
        }
    }

    private fun setupFab() {
        binding.fabCreatePost.setOnClickListener {
            findNavController().navigate(R.id.action_forum_to_create_post)
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
            viewModel.loadPosts(token, currentCategory, refresh)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh posts when returning to fragment (e.g., after creating a new post)
        loadPosts(refresh = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
