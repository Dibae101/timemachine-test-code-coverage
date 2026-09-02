package com.example.util.simpletimetracker.feature_base_adapter.recordsFilter

import androidx.annotation.ColorInt
import androidx.core.view.isInvisible
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemFavouriteRecordsFilterLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.recordsFilter.FavouriteRecordsFilterViewData as ViewData

fun createFavouriteRecordsFilterAdapterDelegate(
    onItemClick: ((ViewData) -> Unit),
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        containerFavouriteRecordsFilterItem.setCardBackgroundColor(item.backgroundColor)
        tvFavouriteRecordsFilterTitle.text = item.text
        viewFavouriteRecordsFilterClick.isEnabled = item.isEnabled
        // Invisible to preserve root height.
        btnFavouriteRecordsFilterDelete.isInvisible = !item.isDeleteVisible

        viewFavouriteRecordsFilterClick.setOnClickWith(item, onItemClick)
    }
}

data class FavouriteRecordsFilterViewData(
    val id: Long,
    val text: CharSequence,
    @ColorInt val backgroundColor: Int,
    val isEnabled: Boolean,
    val isDeleteVisible: Boolean,
) : ViewHolderType {

    override fun getUniqueId(): Long = id

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}
