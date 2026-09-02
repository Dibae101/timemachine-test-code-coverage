package com.example.util.simpletimetracker.feature_statistics_detail.adapter

import android.content.res.ColorStateList
import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setMargins
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_views.extension.visible
import com.example.util.simpletimetracker.feature_statistics_detail.adapter.StatisticsDetailButtonViewData as ViewData
import com.example.util.simpletimetracker.feature_statistics_detail.databinding.StatisticsDetailButtonItemBinding as Binding

fun createStatisticsDetailButtonAdapterDelegate(
    onClick: (StatisticsDetailBlock) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        root.tag = item.data.block
        root.setMargins(top = item.marginTopDp)

        btnStatisticsDetailButtonItem.text = item.data.text
        btnStatisticsDetailButtonItem.backgroundTintList =
            ColorStateList.valueOf(item.data.color)
        btnStatisticsDetailButtonItem.setOnClickWith(item.data.block, onClick)

        if (item.dataSecond != null) {
            btnStatisticsDetailButtonSecondItem.visible = true
            btnStatisticsDetailButtonSecondItem.text = item.dataSecond.text
            btnStatisticsDetailButtonSecondItem.backgroundTintList =
                ColorStateList.valueOf(item.dataSecond.color)
            btnStatisticsDetailButtonSecondItem.setOnClickWith(item.dataSecond.block, onClick)
        } else {
            btnStatisticsDetailButtonSecondItem.visible = false
        }
    }
}

data class StatisticsDetailButtonViewData(
    val marginTopDp: Int,
    val data: Button,
    val dataSecond: Button?,
) : ViewHolderType {

    data class Button(
        val block: StatisticsDetailBlock,
        val text: String,
        @ColorInt val color: Int,
    )

    override fun getUniqueId(): Long = data.block.ordinal.toLong()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ViewData
}