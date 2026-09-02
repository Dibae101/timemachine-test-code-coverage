package com.example.util.simpletimetracker.feature_date_selection.api.viewDelegate

import androidx.viewbinding.ViewBinding
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.feature_date_selection.api.DateSelectorViewModelDelegate
import com.example.util.simpletimetracker.feature_date_selection.api.databinding.DateSelectorLayoutBinding

interface DateSelectorViewDelegateProvider {
    fun <T : ViewBinding> provide(
        viewModel: DateSelectorViewModelDelegate,
        binding: DateSelectorLayoutBinding,
        fragment: BaseFragment<T>,
    ): DateSelectorViewDelegate
}