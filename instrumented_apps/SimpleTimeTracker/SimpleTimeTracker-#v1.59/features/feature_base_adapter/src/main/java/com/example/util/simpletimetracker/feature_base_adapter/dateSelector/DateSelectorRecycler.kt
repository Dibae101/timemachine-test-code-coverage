package com.example.util.simpletimetracker.feature_base_adapter.dateSelector

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.R
import androidx.recyclerview.widget.RecyclerView

class DateSelectorRecycler @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.recyclerViewStyle,
) : RecyclerView(
    context,
    attrs,
    defStyleAttr,
) {

    private var touchInterceptListener: ((MotionEvent) -> Unit)? = null

    fun setTouchInterceptListener(listener: (MotionEvent) -> Unit) {
        this.touchInterceptListener = listener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        touchInterceptListener?.invoke(event)
        return super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        touchInterceptListener?.invoke(event)
        return super.onInterceptTouchEvent(event)
    }
}