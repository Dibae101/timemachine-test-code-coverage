package com.example.util.simpletimetracker.feature_date_selection.viewDelegate

import androidx.viewbinding.ViewBinding
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.feature_date_selection.api.DateSelectorViewModelDelegate
import com.example.util.simpletimetracker.feature_date_selection.api.databinding.DateSelectorLayoutBinding
import com.example.util.simpletimetracker.feature_date_selection.api.viewDelegate.DateSelectorViewDelegate
import com.example.util.simpletimetracker.feature_date_selection.api.viewDelegate.DateSelectorViewDelegateProvider
import javax.inject.Inject

class DateSelectorViewDelegateProviderImpl @Inject constructor() : DateSelectorViewDelegateProvider {

    override fun <T : ViewBinding> provide(
        viewModel: DateSelectorViewModelDelegate,
        binding: DateSelectorLayoutBinding,
        fragment: BaseFragment<T>,
    ): DateSelectorViewDelegate {
        return DateSelectorViewDelegateImpl(viewModel, binding, fragment)
    }
}