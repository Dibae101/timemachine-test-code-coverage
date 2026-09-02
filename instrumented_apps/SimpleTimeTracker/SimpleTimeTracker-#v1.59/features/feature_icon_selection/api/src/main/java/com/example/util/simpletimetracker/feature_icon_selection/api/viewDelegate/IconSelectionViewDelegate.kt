package com.example.util.simpletimetracker.feature_icon_selection.api.viewDelegate

import android.content.Context
import android.content.res.Resources
import android.text.TextWatcher
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewbinding.ViewBinding
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.core.repo.DeviceRepo
import com.example.util.simpletimetracker.feature_icon_selection.api.IconSelectionViewModelDelegate
import com.example.util.simpletimetracker.feature_icon_selection.api.databinding.IconSelectionLayoutBinding
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon

interface IconSelectionViewDelegate {
    fun initUi(
        context: Context,
        resources: Resources,
        deviceRepo: DeviceRepo,
        layout: IconSelectionLayoutBinding,
    ): GridLayoutManager

    fun initUx(
        viewModel: IconSelectionViewModelDelegate,
        layout: IconSelectionLayoutBinding,
        iconsLayoutManager: GridLayoutManager?,
    )

    fun <T : ViewBinding> initViewModel(
        fragment: BaseFragment<T>,
        viewModel: IconSelectionViewModelDelegate,
        layout: IconSelectionLayoutBinding,
        iconsLayoutManager: GridLayoutManager?,
    )

    fun onDestroyView(
        textWatcher: TextWatcher?,
        layout: IconSelectionLayoutBinding,
    )

    fun updateUi(
        icon: RecordTypeIcon?,
        viewModel: IconSelectionViewModelDelegate,
        layout: IconSelectionLayoutBinding,
    ): TextWatcher
}