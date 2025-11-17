package com.nottingham.mynottingham.ui.forum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentForumBinding

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFab()
    }

    private fun setupFab() {
        binding.fabCreatePost.setOnClickListener {
            findNavController().navigate(R.id.action_forum_to_create_post)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
