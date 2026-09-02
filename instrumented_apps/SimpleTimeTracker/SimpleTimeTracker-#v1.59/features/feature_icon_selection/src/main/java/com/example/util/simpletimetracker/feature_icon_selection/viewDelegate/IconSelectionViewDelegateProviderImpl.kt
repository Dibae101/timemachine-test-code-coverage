package com.example.util.simpletimetracker.feature_icon_selection.viewDelegate

import com.example.util.simpletimetracker.feature_base_adapter.RecyclerAdapterDelegate
import com.example.util.simpletimetracker.feature_icon_selection.adapter.createIconSelectionAdapterDelegate
import com.example.util.simpletimetracker.feature_icon_selection.api.IconSelectionViewModelDelegate
import com.example.util.simpletimetracker.feature_icon_selection.api.databinding.IconSelectionLayoutBinding
import com.example.util.simpletimetracker.feature_icon_selection.api.viewData.IconSelectionViewData
import com.example.util.simpletimetracker.feature_icon_selection.api.viewDelegate.IconSelectionViewDelegate
import com.example.util.simpletimetracker.feature_icon_selection.api.viewDelegate.IconSelectionViewDelegateProvider
import javax.inject.Inject

class IconSelectionViewDelegateProviderImpl @Inject constructor() : IconSelectionViewDelegateProvider {

    override fun provide(
        viewModel: IconSelectionViewModelDelegate,
        binding: IconSelectionLayoutBinding,
    ): IconSelectionViewDelegate {
        return IconSelectionViewDelegateImpl(viewModel, binding)
    }

    override fun provideIconSelectionAdapterDelegate(
        onIconItemClick: (IconSelectionViewData) -> Unit,
    ): RecyclerAdapterDelegate {
        return createIconSelectionAdapterDelegate(onIconItemClick)
    }
}