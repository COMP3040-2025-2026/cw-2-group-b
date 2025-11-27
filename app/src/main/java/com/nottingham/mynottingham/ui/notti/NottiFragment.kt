package com.nottingham.mynottingham.ui.notti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentNottiBinding

/**
 * NottiFragment - Notti AI Assistant 界面
 *
 * 使用 Firebase AI Logic (Gemini) 提供 AI 对话功能
 */
class NottiFragment : Fragment() {

    private var _binding: FragmentNottiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NottiViewModel by viewModels()
    private lateinit var chatAdapter: NottiMessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNottiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupInputArea()
        setupQuickActions()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = NottiMessageAdapter(
            onBookNowClick = {
                findNavController().navigate(R.id.action_notti_to_booking)
            }
        )
        binding.recyclerMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupInputArea() {
        // 发送按钮点击
        binding.fabSend.setOnClickListener {
            sendMessage()
        }

        // 键盘发送
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun setupQuickActions() {
        binding.chipShuttle.setOnClickListener {
            viewModel.handleQuickAction(NottiViewModel.QuickAction.SHUTTLE)
        }

        binding.chipBooking.setOnClickListener {
            viewModel.handleQuickAction(NottiViewModel.QuickAction.BOOKING)
        }

        binding.chipHelp.setOnClickListener {
            viewModel.handleQuickAction(NottiViewModel.QuickAction.HELP)
        }
    }

    private fun observeViewModel() {
        // 观察消息列表
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages) {
                // 滚动到最新消息
                if (messages.isNotEmpty()) {
                    binding.recyclerMessages.smoothScrollToPosition(messages.size - 1)
                }
            }

            // 显示/隐藏空状态
            binding.layoutEmpty.isVisible = messages.size <= 1
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.fabSend.isEnabled = !isLoading
        }
    }

    private fun sendMessage() {
        val message = binding.etMessage.text?.toString()?.trim()
        if (!message.isNullOrBlank()) {
            viewModel.sendMessage(message)
            binding.etMessage.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
