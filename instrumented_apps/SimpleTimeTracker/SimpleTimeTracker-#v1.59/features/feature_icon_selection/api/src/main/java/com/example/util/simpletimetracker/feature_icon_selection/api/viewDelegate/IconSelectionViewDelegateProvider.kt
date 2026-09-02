package com.example.util.simpletimetracker.feature_icon_selection.api.viewDelegate

import com.example.util.simpletimetracker.feature_base_adapter.RecyclerAdapterDelegate
import com.example.util.simpletimetracker.feature_icon_selection.api.IconSelectionViewModelDelegate
import com.example.util.simpletimetracker.feature_icon_selection.api.databinding.IconSelectionLayoutBinding
import com.example.util.simpletimetracker.feature_icon_selection.api.viewData.IconSelectionViewData

interface IconSelectionViewDelegateProvider {
    fun provide(
        viewModel: IconSelectionViewModelDelegate,
        binding: IconSelectionLayoutBinding,
    ): IconSelectionViewDelegate

    fun provideIconSelectionAdapterDelegate(
        onIconItemClick: ((IconSelectionViewData) -> Unit),
    ): RecyclerAdapterDelegate
}