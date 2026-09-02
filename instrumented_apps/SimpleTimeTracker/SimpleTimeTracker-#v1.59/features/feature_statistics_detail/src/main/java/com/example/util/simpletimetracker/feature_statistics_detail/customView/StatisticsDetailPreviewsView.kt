package com.example.util.simpletimetracker.feature_statistics_detail.customView

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_statistics_detail.adapter.createStatisticsPreviewAdapterDelegate
import com.example.util.simpletimetracker.feature_statistics_detail.adapter.createStatisticsPreviewCompareAdapterDelegate
import com.example.util.simpletimetracker.feature_statistics_detail.adapter.createStatisticsPreviewMoreAdapterDelegate
import com.example.util.simpletimetracker.feature_statistics_detail.databinding.StatisticsDetailPreviewsViewLayoutBinding
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailPreview
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailPreviewViewData
import com.example.util.simpletimetracker.feature_views.extension.layoutInflater
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

// Separate view container for FLEX_START content.
class StatisticsDetailPreviewsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(
    context,
    attrs,
    defStyleAttr,
) {

    val adapter: BaseRecyclerAdapter by lazy {
        BaseRecyclerAdapter(
            createStatisticsPreviewCompareAdapterDelegate(),
            createStatisticsPreviewMoreAdapterDelegate(
                onClick = ::onItemClick,
            ),
            createStatisticsPreviewAdapterDelegate(
                onClick = ::onItemClick,
                onLongClick = ::onItemLongClick,
            ),
        )
    }

    private val binding = StatisticsDetailPreviewsViewLayoutBinding.inflate(layoutInflater, this)
    private var clickListener: (StatisticsDetailPreview) -> Unit = {}
    private var longClickListener: (StatisticsDetailPreview) -> Unit = {}

    init {
        initRecycler()
        initEditMode()
    }

    fun setClickListener(listener: (StatisticsDetailPreview) -> Unit) {
        clickListener = listener
    }

    fun setLongClickListener(listener: (StatisticsDetailPreview) -> Unit) {
        longClickListener = listener
    }

    private fun initRecycler() {
        binding.rvStatisticsDetailPreviewsContainer.apply {
            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START
                flexWrap = FlexWrap.WRAP
            }
            adapter = this@StatisticsDetailPreviewsView.adapter
        }
    }

    private fun onItemClick(item: StatisticsDetailPreview) {
        clickListener(item)
    }

    private fun onItemLongClick(item: StatisticsDetailPreview) {
        longClickListener(item)
    }

    private fun initEditMode() {
        if (isInEditMode) {
            List(3) {
                StatisticsDetailPreviewViewData(
                    id = it.toLong(),
                    type = StatisticsDetailPreviewViewData.Type.FILTER,
                    dataType = StatisticsDetailPreviewViewData.DataType.OTHER,
                    name = it.toString(),
                    iconId = null,
                    iconColor = null,
                    color = Color.BLACK,
                    isFiltered = false,
                )
            }.let(adapter::replace)
        }
    }
}