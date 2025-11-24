package com.nottingham.mynottingham.ui.message

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.nottingham.mynottingham.R

/**
 * Custom view for alphabet index sidebar
 * Allows quick scrolling through contacts by letter
 */
class AlphabetIndexView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val letters = listOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private var onLetterSelectedListener: ((String) -> Unit)? = null

    fun setOnLetterSelectedListener(listener: (String) -> Unit) {
        onLetterSelectedListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val sectionHeight = height / letters.size.toFloat()
        val textCenterY = (paint.descent() + paint.ascent()) / 2

        letters.forEachIndexed { index, letter ->
            val y = sectionHeight * (index + 0.5f) - textCenterY
            canvas.drawText(letter, width / 2f, y, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val sectionHeight = height / letters.size.toFloat()
                val index = (event.y / sectionHeight).toInt().coerceIn(0, letters.size - 1)
                onLetterSelectedListener?.invoke(letters[index])
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
