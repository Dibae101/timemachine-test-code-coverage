package com.example.util.simpletimetracker.feature_shortcuts.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.core.utils.InsetConfiguration
import com.example.util.simpletimetracker.core.utils.doOnApplyWindowInsetsListener
import com.example.util.simpletimetracker.core.utils.getNavBarInsetsBottom
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.button.createButtonAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.empty.createEmptyAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.emptySpace.createEmptySpaceAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.loader.createLoaderAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordShortcut.createRecordShortcutAdapterDelegate
import com.example.util.simpletimetracker.feature_shortcuts.viewModel.ShortcutsViewModel
import com.example.util.simpletimetracker.feature_views.extension.pxToDp
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import dagger.hilt.android.AndroidEntryPoint
import com.example.util.simpletimetracker.feature_shortcuts.databinding.ShortcutsFragmentBinding as Binding

@AndroidEntryPoint
class ShortcutsFragment : BaseFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    override var insetConfiguration: InsetConfiguration =
        InsetConfiguration.DoNotApply

    private val viewModel: ShortcutsViewModel by viewModels()

    private val adapter: BaseRecyclerAdapter by lazy {
        BaseRecyclerAdapter(
            createEmptySpaceAdapterDelegate(),
            createLoaderAdapterDelegate(),
            createEmptyAdapterDelegate(),
            createButtonAdapterDelegate(
                onClick = throttle(viewModel::onItemButtonClick),
            ),
            createRecordShortcutAdapterDelegate(
                onClickWithTransition = throttle(viewModel::onShortcutClick),
            ),
        )
    }

    override fun initUi(): Unit = with(binding) {
        rvShortcutsList.apply {
            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.CENTER
                flexWrap = FlexWrap.WRAP
            }
            adapter = this@ShortcutsFragment.adapter
        }

        view?.doOnApplyWindowInsetsListener {
            viewModel.onChangeInsets(navBarHeight = it.getNavBarInsetsBottom().pxToDp())
        }
    }

    override fun initViewModel(): Unit = with(viewModel) {
        viewData.observe(adapter::replace)
    }
}
