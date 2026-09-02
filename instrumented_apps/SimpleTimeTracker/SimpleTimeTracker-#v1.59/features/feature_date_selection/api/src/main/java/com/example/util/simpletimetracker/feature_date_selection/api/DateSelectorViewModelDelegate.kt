package com.example.util.simpletimetracker.feature_date_selection.api

import androidx.lifecycle.LiveData
import com.example.util.simpletimetracker.feature_base_adapter.InfiniteRecyclerAdapter
import com.example.util.simpletimetracker.feature_date_selection.api.viewData.DateSelectorButtonsViewData
import com.example.util.simpletimetracker.feature_date_selection.api.viewData.DateSelectorScrollViewData
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams

interface DateSelectorViewModelDelegate {
    val dataProvider: DateSelectorMapper
    val dateScrollPosition: LiveData<DateSelectorScrollViewData>
    val buttonsViewData: LiveData<DateSelectorButtonsViewData>
    val updateDatesViewData: LiveData<Unit>
    val borderShadowsVisibility: LiveData<Boolean>

    fun attach(parent: Parent)
    suspend fun initialize(position: Int)
    suspend fun setup()
    fun updatePosition(shift: Int)
    fun onScrolledToDate(position: Int)
    fun onDateLongClick(item: InfiniteRecyclerAdapter.Data)
    fun onDateClick(item: InfiniteRecyclerAdapter.Data)
    fun getOptionsButton(
        options: List<OptionsListParams.Item>,
    ): DateSelectorMapper.SetupData.Button

    interface Parent {
        val currentPosition: Int

        fun onDateClick()
        fun updatePosition(newPosition: Int)
        suspend fun getSetupData(): DateSelectorMapper.SetupData.Type
    }
}