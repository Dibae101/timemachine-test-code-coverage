package com.example.util.simpletimetracker.feature_dialogs.optionsDialog

import androidx.lifecycle.LiveData
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.extension.lazySuspend
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OptionsListViewModel @Inject constructor(
    private val optionsListViewDataInteractor: OptionsListViewDataInteractor,
) : BaseViewModel() {

    lateinit var extra: OptionsListParams

    val state: LiveData<List<ViewHolderType>> by lazySuspend { loadState() }

    private fun loadState(): List<ViewHolderType> {
        return optionsListViewDataInteractor.getViewData(extra)
    }
}
