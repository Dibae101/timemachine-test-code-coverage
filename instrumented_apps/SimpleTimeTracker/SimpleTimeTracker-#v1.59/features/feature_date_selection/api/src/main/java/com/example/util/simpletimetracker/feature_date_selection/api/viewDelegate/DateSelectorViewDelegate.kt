package com.example.util.simpletimetracker.feature_date_selection.api.viewDelegate

import androidx.recyclerview.widget.LinearSnapHelper
import com.example.util.simpletimetracker.feature_base_adapter.InfiniteRecyclerAdapter

interface DateSelectorViewDelegate {

    fun initUi()
    fun initUx(
        onRecordAddClick: () -> Unit = {},
        onOptionsClick: () -> Unit = {},
        onOptionsLongClick: () -> Unit = {},
    )
    fun initViewModel()

    interface ViewHolder {
        val adapter: Lazy<InfiniteRecyclerAdapter>
        val snapHelper: LinearSnapHelper
    }
}