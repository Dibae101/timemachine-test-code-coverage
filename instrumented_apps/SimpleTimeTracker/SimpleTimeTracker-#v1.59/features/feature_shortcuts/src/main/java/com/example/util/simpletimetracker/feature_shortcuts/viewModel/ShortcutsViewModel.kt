package com.example.util.simpletimetracker.feature_shortcuts.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.extension.lazySuspend
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.button.ButtonViewData
import com.example.util.simpletimetracker.feature_base_adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordShortcut.RecordShortcutViewData
import com.example.util.simpletimetracker.feature_shortcuts.interactor.ShortcutsViewDataInteractor
import com.example.util.simpletimetracker.domain.recordShortcut.interactor.ShortcutsDataUpdateInteractor
import com.example.util.simpletimetracker.feature_shortcuts.viewData.ShortcutsButtonViewData
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.ChangeShortcutParams
import com.example.util.simpletimetracker.core.extension.toPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    private val router: Router,
    private val shortcutsViewDataInteractor: ShortcutsViewDataInteractor,
    private val shortcutsDataUpdateInteractor: ShortcutsDataUpdateInteractor,
) : BaseViewModel() {

    val viewData: LiveData<List<ViewHolderType>> by lazySuspend {
        listOf(LoaderViewData()).also { updateViewData() }
    }

    private var navBarHeightDp: Int = 0

    init {
        viewModelScope.launch {
            shortcutsDataUpdateInteractor.dataUpdated.collect {
                updateViewData()
            }
        }
    }

    fun onChangeInsets(navBarHeight: Int) {
        if (navBarHeightDp != navBarHeight) {
            navBarHeightDp = navBarHeight
            updateViewData()
        }
    }

    fun onItemButtonClick(viewData: ButtonViewData) {
        val id = viewData.id as? ShortcutsButtonViewData ?: return
        when (id.block) {
            ShortcutsButtonViewData.Block.ADD -> {
                router.navigate(data = ChangeShortcutParams.New)
            }
        }
    }

    fun onShortcutClick(
        item: RecordShortcutViewData,
        sharedElements: Pair<Any, String>? = null,
    ) {
        router.navigate(
            data = ChangeShortcutParams.Change(
                id = item.id,
                transitionName = sharedElements?.second.orEmpty(),
                preview = item.toPreview(),
            ),
            sharedElements = sharedElements?.let { mapOf(it) },
        )
    }

    private fun updateViewData() = viewModelScope.launch {
        val data = shortcutsViewDataInteractor.getViewData(navBarHeightDp)
        delayLoad()
        viewData.set(data)
    }
}
