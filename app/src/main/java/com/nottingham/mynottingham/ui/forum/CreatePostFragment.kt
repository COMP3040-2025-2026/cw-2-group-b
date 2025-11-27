package com.nottingham.mynottingham.ui.forum

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.ForumCategory
import com.nottingham.mynottingham.databinding.FragmentCreatePostBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CreatePostViewModel
    private lateinit var tokenManager: TokenManager

    // Edit mode properties
    private var isEditMode: Boolean = false
    private var editPostId: String? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            viewModel.setSelectedImage(imageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CreatePostViewModel::class.java]
        tokenManager = TokenManager(requireContext())

        // Check for edit mode arguments
        arguments?.let { args ->
            isEditMode = args.getBoolean("isEditMode", false)
            editPostId = args.getString("editPostId")
        }

        setupToolbar()
        setupCategorySpinner()
        setupPinCheckbox()
        setupImageSelection()
        observeViewModel()

        // If in edit mode, populate the fields
        if (isEditMode) {
            populateEditData()
        }
    }

    private fun setupImageSelection() {
        // Add image button click
        binding.btnAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Remove image button click
        binding.btnRemoveImage.setOnClickListener {
            viewModel.clearSelectedImage()
        }

        // Observe selected image
        viewModel.selectedImageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                binding.cardImagePreview.visibility = View.VISIBLE
                binding.btnAddImage.text = "Change Image"
                Glide.with(requireContext())
                    .load(uri)
                    .into(binding.ivImagePreview)
            } else {
                binding.cardImagePreview.visibility = View.GONE
                binding.btnAddImage.text = "Add Image"
                binding.ivImagePreview.setImageDrawable(null)
            }
        }
    }

    private fun setupPinCheckbox() {
        // Only show pin checkbox for teachers
        lifecycleScope.launch {
            val userType = tokenManager.getUserType().first()
            if (userType == "TEACHER") {
                binding.checkboxPin.visibility = View.VISIBLE
            }
        }
    }

    private fun populateEditData() {
        arguments?.let { args ->
            val title = args.getString("editTitle")
            val content = args.getString("editContent")
            val category = args.getString("editCategory")
            val tags = args.getString("editTags")
            val isPinned = args.getBoolean("editIsPinned", false)

            binding.editTitle.setText(title)
            binding.editContent.setText(content)

            // Set category in spinner
            if (category != null) {
                val categoryEnum = ForumCategory.values().find { it.name == category }
                categoryEnum?.let {
                    binding.spinnerCategory.setText(it.displayName, false)
                }
            }

            // Set tags
            if (tags != null) {
                binding.editTags.setText(tags)
            }

            // Set pin status
            binding.checkboxPin.isChecked = isPinned
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Update toolbar title for edit mode
        if (isEditMode) {
            binding.toolbar.title = "Edit Post"
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_post -> {
                    submitPost()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupCategorySpinner() {
        val categories = ForumCategory.values().map { it.displayName }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.spinnerCategory.setAdapter(adapter)

        // Set default category
        if (categories.isNotEmpty()) {
            binding.spinnerCategory.setText(categories[0], false)
        }
    }

    private fun submitPost() {
        val title = binding.editTitle.text.toString()
        val content = binding.editContent.text.toString()
        val categoryText = binding.spinnerCategory.text.toString()
        val tagsText = binding.editTags.text.toString()
        val isPinned = binding.checkboxPin.isChecked

        // Parse tags
        val tags = if (tagsText.isNotBlank()) {
            tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            null
        }

        // Find matching category enum value
        val category = ForumCategory.values().find { it.displayName == categoryText }
            ?: ForumCategory.GENERAL

        lifecycleScope.launch {
            if (isEditMode && editPostId != null) {
                // Edit existing post
                viewModel.updatePost(
                    postId = editPostId!!,
                    title = title,
                    content = content,
                    category = category.name,
                    tags = tags,
                    isPinned = isPinned
                )
            } else {
                // Create new post
                viewModel.createPost(
                    title = title,
                    content = content,
                    category = category.name,
                    tags = tags,
                    isPinned = isPinned
                )
            }
        }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            // Disable inputs while loading
            binding.editTitle.isEnabled = !isLoading
            binding.editContent.isEnabled = !isLoading
            binding.editTags.isEnabled = !isLoading
            binding.spinnerCategory.isEnabled = !isLoading
            binding.btnAddImage.isEnabled = !isLoading
            binding.btnRemoveImage.isEnabled = !isLoading
            binding.toolbar.menu.findItem(R.id.action_post)?.isEnabled = !isLoading
        }

        viewModel.uploadingImage.observe(viewLifecycleOwner) { isUploading ->
            binding.btnAddImage.alpha = if (isUploading) 0.5f else 1.0f
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.postCreated.observe(viewLifecycleOwner) { created ->
            if (created) {
                Toast.makeText(context, "Post created successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
