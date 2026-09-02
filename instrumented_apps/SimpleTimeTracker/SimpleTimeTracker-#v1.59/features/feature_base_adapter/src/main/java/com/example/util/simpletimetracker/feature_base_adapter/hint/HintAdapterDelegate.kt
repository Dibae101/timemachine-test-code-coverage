package com.example.util.simpletimetracker.feature_base_adapter.hint

import android.view.Gravity
import androidx.core.view.updatePadding
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.dpToPx
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemHintLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData as ViewData

fun createHintAdapterDelegate() = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    fun ViewData.Gravity.toViewData(): Int {
        return when (this) {
            ViewData.Gravity.CENTER -> Gravity.CENTER
            ViewData.Gravity.START -> Gravity.START
        }
    }

    with(binding) {
        item as ViewData

        tvHintItemText.updatePadding(
            top = item.paddingTop.dpToPx(),
            bottom = item.paddingBottom.dpToPx(),
        )
        tvHintItemText.text = item.text
        tvHintItemText.gravity = item.gravity.toViewData()
    }
}