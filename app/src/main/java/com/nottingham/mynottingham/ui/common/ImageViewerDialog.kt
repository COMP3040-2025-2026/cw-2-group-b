package com.nottingham.mynottingham.ui.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.nottingham.mynottingham.R

/**
 * Full screen image viewer dialog
 */
class ImageViewerDialog(
    context: Context,
    private val imageUrl: String
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.dialog_image_viewer)

        // Make dialog full screen with transparent background
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#DD000000")))
        }

        // Load image
        val imageView = findViewById<ImageView>(R.id.iv_fullscreen_image)
        Glide.with(context)
            .load(imageUrl)
            .into(imageView)

        // Close button
        findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dismiss()
        }

        // Click outside to dismiss
        imageView.setOnClickListener {
            dismiss()
        }
    }
}
