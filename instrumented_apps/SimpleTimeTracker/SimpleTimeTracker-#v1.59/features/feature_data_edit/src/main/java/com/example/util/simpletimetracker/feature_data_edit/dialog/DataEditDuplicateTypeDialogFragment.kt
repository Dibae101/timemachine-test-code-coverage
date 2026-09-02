package com.example.util.simpletimetracker.feature_data_edit.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.util.simpletimetracker.core.base.BaseBottomSheetFragment
import com.example.util.simpletimetracker.feature_dialogs.api.StandardDialogListener
import com.example.util.simpletimetracker.core.extension.blockContentScroll
import com.example.util.simpletimetracker.core.extension.setFullScreen
import com.example.util.simpletimetracker.core.extension.setSkipCollapsed
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.hint.createHintAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.loader.createLoaderAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordType.createRecordTypeAdapterDelegate
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import dagger.hilt.android.AndroidEntryPoint
import com.example.util.simpletimetracker.feature_data_edit.databinding.DataEditDuplicateTypeDialogFragmentBinding as Binding

@AndroidEntryPoint
class DataEditDuplicateTypeDialogFragment :
    BaseBottomSheetFragment<Binding>(),
    StandardDialogListener {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    private val viewModel: DataEditDuplicateTypeViewModel by viewModels()

    private val adapter: BaseRecyclerAdapter by lazy {
        BaseRecyclerAdapter(
            createLoaderAdapterDelegate(),
            createHintAdapterDelegate(),
            createRecordTypeAdapterDelegate(viewModel::onItemClick),
        )
    }

    override fun initDialog() {
        setSkipCollapsed()
        setFullScreen()
        blockContentScroll(binding.rvDataEditDuplicateTypeContainer)
    }

    override fun initUi(): Unit = with(binding) {
        rvDataEditDuplicateTypeContainer.apply {
            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.CENTER
                flexWrap = FlexWrap.WRAP
            }
            adapter = this@DataEditDuplicateTypeDialogFragment.adapter
        }
    }

    override fun initViewModel(): Unit = with(viewModel) {
        viewData.observe(adapter::replace)
    }

    override fun onPositiveClick(tag: String?, data: Any?) {
        viewModel.onPositiveDialogClick(tag, data)
    }
}