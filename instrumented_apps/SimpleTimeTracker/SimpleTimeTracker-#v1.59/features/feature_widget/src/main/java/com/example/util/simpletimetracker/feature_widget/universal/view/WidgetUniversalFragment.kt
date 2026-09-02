package com.example.util.simpletimetracker.feature_widget.universal.view

import com.example.util.simpletimetracker.feature_widget.databinding.WidgetUniversalFragmentBinding as Binding
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.feature_dialogs.api.OnTagSelectedListener
import com.example.util.simpletimetracker.core.utils.InsetConfiguration
import com.example.util.simpletimetracker.core.utils.doOnApplyWindowInsetsListener
import com.example.util.simpletimetracker.core.utils.getNavBarInsetsBottom
import com.example.util.simpletimetracker.core.viewData.RecordTypeSuggestionType
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.activityFilter.createActivityFilterAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.activityFilter.createActivityFilterAddAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.button.createButtonAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.divider.createDividerAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.empty.createEmptyAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.emptySpace.createEmptySpaceAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.hint.createHintAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.loader.createLoaderAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordShortcut.createRecordShortcutAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordType.createRecordTypeAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordTypeSpecial.createRunningRecordTypeSpecialAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordTypeSuggestion.createRecordTypeSuggestionAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.pxToDp
import com.example.util.simpletimetracker.feature_widget.universal.viewModel.WidgetUniversalViewModel
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WidgetUniversalFragment :
    BaseFragment<Binding>(),
    OnTagSelectedListener {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    override var insetConfiguration: InsetConfiguration =
        InsetConfiguration.DoNotApply

    private val viewModel: WidgetUniversalViewModel by viewModels()

    private val typesAdapter: BaseRecyclerAdapter by lazy {
        BaseRecyclerAdapter(
            createActivityFilterAdapterDelegate(viewModel::onActivityFilterClick),
            createActivityFilterAddAdapterDelegate(viewModel::onActivityFilterSpecialClick),
            createRecordTypeAdapterDelegate(viewModel::onRecordTypeClick),
            createRecordTypeSuggestionAdapterDelegate(RecordTypeSuggestionType, viewModel::onRecordTypeClick),
            createRunningRecordTypeSpecialAdapterDelegate(viewModel::onSpecialRecordTypeClick),
            createRecordShortcutAdapterDelegate(
                onClick = viewModel::onShortcutClick,
                onSpinnerPositionSelected = viewModel::onShortcutSpinnerPositionSelected,
                onButtonClicked = viewModel::onShortcutButtonClick,
            ),
            createButtonAdapterDelegate(viewModel::onButtonClick),
            createEmptySpaceAdapterDelegate(),
            createDividerAdapterDelegate(),
            createEmptyAdapterDelegate(),
            createLoaderAdapterDelegate(),
            createHintAdapterDelegate(),
        )
    }

    override fun initUi() {
        binding.rvWidgetUniversalRecordType.apply {
            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.CENTER
                flexWrap = FlexWrap.WRAP
            }
            adapter = typesAdapter
        }

        view?.doOnApplyWindowInsetsListener {
            viewModel.onChangeInsets(navBarHeight = it.getNavBarInsetsBottom().pxToDp())
        }
    }

    override fun initViewModel(): Unit = with(viewModel) {
        recordTypes.observe(typesAdapter::replace)
        exit.observe { exit() }
    }

    override fun onTagSelected() {
        viewModel.onTagSelected()
    }

    private fun exit() {
        Handler(Looper.getMainLooper()).postDelayed({ activity?.finish() }, EXIT_DELAY_MS)
    }

    companion object {
        private const val EXIT_DELAY_MS = 500L
    }
}