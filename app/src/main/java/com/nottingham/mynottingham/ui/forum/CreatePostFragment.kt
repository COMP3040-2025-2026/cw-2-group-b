package com.nottingham.mynottingham.ui.forum

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CreatePostViewModel
    private lateinit var tokenManager: TokenManager

    private var selectedImageUri: Uri? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displayImagePreview(uri)
            }
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

        setupToolbar()
        setupCategorySpinner()
        setupImagePicker()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClick {
            findNavController().navigateUp()
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

    private fun setupImagePicker() {
        binding.btnAddImage.setOnClickListener {
            openImagePicker()
        }

        binding.btnRemoveImage.setOnClickListener {
            removeImage()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun displayImagePreview(uri: Uri) {
        binding.cardImage.visibility = View.VISIBLE
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.imagePreview)
    }

    private fun removeImage() {
        selectedImageUri = null
        binding.cardImage.visibility = View.GONE
        binding.imagePreview.setImageDrawable(null)
    }

    private fun submitPost() {
        val title = binding.editTitle.text.toString()
        val content = binding.editContent.text.toString()
        val categoryText = binding.spinnerCategory.text.toString()
        val tagsText = binding.editTags.text.toString()

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
            val token = tokenManager.getToken().first() ?: ""

            // Prepare image part if present
            val imagePart = selectedImageUri?.let { uri ->
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { output ->
                        inputStream?.copyTo(output)
                    }
                    val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("image", file.name, requestBody)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to prepare image", Toast.LENGTH_SHORT).show()
                    null
                }
            }

            viewModel.createPost(
                token = token,
                title = title,
                content = content,
                category = category.name,
                tags = tags,
                image = imagePart
            )
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
            binding.toolbar.menu.findItem(R.id.action_post)?.isEnabled = !isLoading
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
