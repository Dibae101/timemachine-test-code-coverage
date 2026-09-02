package com.example.util.simpletimetracker.feature_icon_selection.viewDelegate

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.text.TextWatcher
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewbinding.ViewBinding
import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.core.repo.DeviceRepo
import com.example.util.simpletimetracker.domain.icon.IconEmojiType
import com.example.util.simpletimetracker.domain.icon.IconImageState
import com.example.util.simpletimetracker.domain.icon.IconType
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.buttonsRow.view.ButtonsRowViewData
import com.example.util.simpletimetracker.feature_base_adapter.emoji.createEmojiAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.feature_base_adapter.loader.createLoaderAdapterDelegate
import com.example.util.simpletimetracker.feature_icon_selection.adapter.createIconSelectionAdapterDelegate
import com.example.util.simpletimetracker.feature_icon_selection.adapter.createIconSelectionCategoryAdapterDelegate
import com.example.util.simpletimetracker.feature_icon_selection.adapter.createIconSelectionCategoryInfoAdapterDelegate
import com.example.util.simpletimetracker.feature_icon_selection.api.IconSelectionViewModelDelegate
import com.example.util.simpletimetracker.feature_icon_selection.api.databinding.IconSelectionLayoutBinding
import com.example.util.simpletimetracker.feature_icon_selection.api.viewData.IconSelectionScrollViewData
import com.example.util.simpletimetracker.feature_icon_selection.api.viewData.IconSelectionSelectorStateViewData
import com.example.util.simpletimetracker.feature_icon_selection.api.viewData.IconSelectionStateViewData
import com.example.util.simpletimetracker.feature_icon_selection.api.viewDelegate.IconSelectionViewDelegate
import com.example.util.simpletimetracker.feature_icon_selection.viewData.IconSelectionCategoryInfoViewData
import com.example.util.simpletimetracker.feature_icon_selection.viewData.IconSelectionSwitchViewData
import com.example.util.simpletimetracker.feature_views.extension.addOnScrollListenerAdapter
import com.example.util.simpletimetracker.feature_views.extension.dpToPx
import com.example.util.simpletimetracker.feature_views.extension.setOnClick
import com.example.util.simpletimetracker.feature_views.extension.setSpanSizeLookup
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.max

class IconSelectionViewDelegateImpl(
    viewModel: IconSelectionViewModelDelegate,
    binding: IconSelectionLayoutBinding,
) : IconSelectionViewDelegate {

    private val iconsAdapter: BaseRecyclerAdapter by lazy {
        BaseRecyclerAdapter(
            createLoaderAdapterDelegate(),
            createIconSelectionAdapterDelegate(viewModel::onIconClick),
            createEmojiAdapterDelegate(viewModel::onEmojiClick),
            createIconSelectionCategoryInfoAdapterDelegate(),
        )
    }
    private val iconCategoriesAdapter: BaseRecyclerAdapter by lazy {
        BaseRecyclerAdapter(
            createIconSelectionCategoryAdapterDelegate {
                viewModel.onIconCategoryClick(it)
                binding.rvIconSelection.stopScroll()
            },
        )
    }

    override fun initUi(
        context: Context,
        resources: Resources,
        deviceRepo: DeviceRepo,
        layout: IconSelectionLayoutBinding,
    ): GridLayoutManager = with(layout) {
        val columnCount = getIconsColumnCount(
            resources,
            deviceRepo,
            layout,
        )
        val manager = GridLayoutManager(context, columnCount)

        rvIconSelection.apply {
            layoutManager = manager
            adapter = iconsAdapter
            itemAnimator = null
            setIconsSpanSize(
                iconsLayoutManager = manager,
                iconsAdapter = iconsAdapter,
            )
        }

        rvIconSelectionCategory.apply {
            layoutManager = GridLayoutManager(context, IconEmojiType.entries.size)
            adapter = iconCategoriesAdapter
            itemAnimator = null
        }

        return@with manager
    }

    override fun initUx(
        viewModel: IconSelectionViewModelDelegate,
        layout: IconSelectionLayoutBinding,
        iconsLayoutManager: GridLayoutManager?,
    ) = with(layout) {
        btnIconSelectionSwitch.listener = {
            updateIconContainerScroll(it, layout)
            viewModel.onIconTypeClick(it)
        }
        etIconSelectionSearch.doAfterTextChanged { viewModel.onIconImageSearch(it.toString()) }
        btnIconSelectionSearch.setOnClick(viewModel::onIconImageSearchClicked)
        btnIconSelectionFavourite.setOnClick(viewModel::onIconImageFavouriteClicked)
        rvIconSelection.addOnScrollListenerAdapter(
            onScrolled = { _, _, _ ->
                iconsLayoutManager?.let {
                    viewModel.onIconsScrolled(
                        firstVisiblePosition = it.findFirstCompletelyVisibleItemPosition(),
                        lastVisiblePosition = it.findLastCompletelyVisibleItemPosition(),
                    )
                }
            },
        )
    }

    override fun <T : ViewBinding> initViewModel(
        fragment: BaseFragment<T>,
        viewModel: IconSelectionViewModelDelegate,
        layout: IconSelectionLayoutBinding,
        iconsLayoutManager: GridLayoutManager?,
    ) {
        fragment.initIconSelectionViewModel(
            viewModel = viewModel,
            layout = layout,
            iconsAdapter = iconsAdapter,
            iconCategoriesAdapter = iconCategoriesAdapter,
            iconsLayoutManager = iconsLayoutManager,
        )
    }

    private fun <T : ViewBinding> BaseFragment<T>.initIconSelectionViewModel(
        viewModel: IconSelectionViewModelDelegate,
        layout: IconSelectionLayoutBinding,
        iconsAdapter: BaseRecyclerAdapter,
        iconCategoriesAdapter: BaseRecyclerAdapter,
        iconsLayoutManager: GridLayoutManager?,
    ) = with(viewModel) {
        icons.observe {
            updateIconsState(
                state = it,
                layout = layout,
                iconsAdapter = iconsAdapter,
            )
        }
        iconCategories.observe {
            updateIconCategories(
                data = it,
                iconCategoriesAdapter = iconCategoriesAdapter,
            )
        }
        iconsTypeViewData.observe {
            updateIconsTypeViewData(
                data = it,
                layout = layout,
            )
        }
        iconSelectorViewData.observe {
            updateIconSelectorViewData(
                data = it,
                layout = layout,
            )
        }
        expandIconTypeSwitch.observe {
            updateBarExpanded(layout)
        }
        iconsScrollPosition.observe {
            if (it is IconSelectionScrollViewData.ScrollTo) {
                iconsLayoutManager?.scrollToPositionWithOffset(it.position, 0)
                onScrolled()
            }
        }
    }

    override fun onDestroyView(
        textWatcher: TextWatcher?,
        layout: IconSelectionLayoutBinding,
    ): Unit = with(layout) {
        // Remove textWatcher because it will be set again on init ViewModel,
        // to avoid several watcher being set on screen navigation forward and backward.
        textWatcher?.let(etIconSelectionText::removeTextChangedListener)
    }

    override fun updateUi(
        icon: RecordTypeIcon?,
        viewModel: IconSelectionViewModelDelegate,
        layout: IconSelectionLayoutBinding,
    ): TextWatcher = with(layout) {
        (icon as? RecordTypeIcon.Text)?.text?.let {
            etIconSelectionText.setText(it)
        }
        // Set listener only after text is set to avoid trigger on screen return.
        etIconSelectionText.doAfterTextChanged { viewModel.onIconTextChange(it.toString()) }
    }

    private fun updateBarExpanded(
        layout: IconSelectionLayoutBinding,
    ) = with(layout) {
        appBarIconSelection.setExpanded(true)
    }

    private fun updateIconsTypeViewData(
        data: List<ViewHolderType>,
        layout: IconSelectionLayoutBinding,
    ) = with(layout) {
        btnIconSelectionSwitch.replace(data)
    }

    private fun updateIconsState(
        state: IconSelectionStateViewData,
        layout: IconSelectionLayoutBinding,
        iconsAdapter: BaseRecyclerAdapter,
    ) = with(layout) {
        when (state) {
            is IconSelectionStateViewData.Icons -> {
                rvIconSelection.isVisible = true
                inputIconSelectionText.isVisible = false
                iconsAdapter.replaceAsNew(state.items)
            }
            is IconSelectionStateViewData.Text -> {
                rvIconSelection.isVisible = false
                inputIconSelectionText.isVisible = true
            }
        }
    }

    private fun updateIconCategories(
        data: List<ViewHolderType>,
        iconCategoriesAdapter: BaseRecyclerAdapter,
    ) {
        iconCategoriesAdapter.replaceAsNew(data)
    }

    private fun getIconsColumnCount(
        resources: Resources,
        deviceRepo: DeviceRepo,
        layout: IconSelectionLayoutBinding,
    ): Int = with(layout) {
        val screenWidth = deviceRepo.getScreenWidthInDp().dpToPx()
        val recyclerWidth = screenWidth -
            2 * resources.getDimensionPixelOffset(R.dimen.color_icon_recycler_margin)
        val elementWidth = resources.getDimensionPixelOffset(R.dimen.color_icon_item_width) +
            2 * resources.getDimensionPixelOffset(R.dimen.color_icon_item_margin)
        val columnCount = max(recyclerWidth / elementWidth, 1)

        val rowWidth = elementWidth * columnCount
        val recyclerPadding = (recyclerWidth - rowWidth) / 2
        rvIconSelection.updatePadding(left = recyclerPadding, right = recyclerPadding)

        return columnCount
    }

    private fun setIconsSpanSize(
        iconsLayoutManager: GridLayoutManager?,
        iconsAdapter: BaseRecyclerAdapter,
    ) {
        iconsLayoutManager?.setSpanSizeLookup { position ->
            when (iconsAdapter.getItemByPosition(position)) {
                is IconSelectionCategoryInfoViewData,
                is LoaderViewData,
                -> iconsLayoutManager.spanCount
                else -> 1
            }
        }
    }

    private fun updateIconContainerScroll(
        item: ButtonsRowViewData,
        layout: IconSelectionLayoutBinding,
    ) = with(layout) {
        if (item !is IconSelectionSwitchViewData) return

        val scrollFlags = if (item.iconType == IconType.TEXT) {
            0
        } else {
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
        }

        (btnIconSelectionSwitch.layoutParams as? AppBarLayout.LayoutParams)
            ?.scrollFlags = scrollFlags
    }

    private fun updateIconSelectorViewData(
        data: IconSelectionSelectorStateViewData,
        layout: IconSelectionLayoutBinding,
    ) = with(layout) {
        if (data is IconSelectionSelectorStateViewData.Available) {
            btnIconSelectionSearch.isVisible = true
            ivIconSelectionSearch.backgroundTintList = ColorStateList.valueOf(data.searchButtonColor)
            btnIconSelectionFavourite.isVisible = true
            ivIconSelectionFavourite.backgroundTintList = ColorStateList.valueOf(data.favouriteButtonColor)
            rvIconSelectionCategory.isVisible = data.state is IconImageState.Chooser
            inputIconSelectionSearch.isVisible = data.state is IconImageState.Search
        } else {
            btnIconSelectionSearch.isVisible = false
            btnIconSelectionFavourite.isVisible = false
            rvIconSelectionCategory.isVisible = false
            inputIconSelectionSearch.isVisible = false
        }
    }
}