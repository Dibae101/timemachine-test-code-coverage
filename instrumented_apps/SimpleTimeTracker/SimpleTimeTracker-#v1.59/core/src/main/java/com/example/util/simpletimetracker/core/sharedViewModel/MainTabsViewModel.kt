package com.example.util.simpletimetracker.core.sharedViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.extension.lazySuspend
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.model.NavigationTab
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordsContainerMultiselectInteractor
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainTabsViewModel @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val recordsContainerMultiselectInteractor: RecordsContainerMultiselectInteractor,
) : BaseViewModel() {

    val tabReselected: LiveData<NavigationTab?> = MutableLiveData()
    val isScrolling: LiveData<Boolean> = MutableLiveData(false)
    val isNavBatAtTheBottom: LiveData<Boolean> by lazySuspend { loadIsNavBatAtTheBottom() }

    fun onTabReselected(tab: NavigationTab) {
        tabReselected.set(tab)
    }

    fun onTabUnselected(tab: NavigationTab) = viewModelScope.launch {
        if (tab == NavigationTab.Records) {
            recordsContainerMultiselectInteractor.disable()
        }
    }

    fun onHandled() {
        tabReselected.set(null)
    }

    fun onScrollStateChanged(isScrolling: Boolean) {
        this.isScrolling.set(isScrolling)
    }

    private suspend fun loadIsNavBatAtTheBottom(): Boolean {
        return prefsInteractor.getIsNavBarAtTheBottom()
    }
}