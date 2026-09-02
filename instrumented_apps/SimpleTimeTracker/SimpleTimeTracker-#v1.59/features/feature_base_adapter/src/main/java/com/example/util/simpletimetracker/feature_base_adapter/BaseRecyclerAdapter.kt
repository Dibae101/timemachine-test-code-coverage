package com.example.util.simpletimetracker.feature_base_adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.example.util.simpletimetracker.feature_base_adapter.loader.createLoaderAdapterDelegate
import timber.log.Timber

class BaseRecyclerAdapter(
    vararg delegatesList: RecyclerAdapterDelegate,
    diffUtilCallback: DiffUtilCallback = DiffUtilCallback(),
) : ListAdapter<ViewHolderType, BaseRecyclerViewHolder>(diffUtilCallback) {

    private val delegates: List<RecyclerAdapterDelegate> = delegatesList.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRecyclerViewHolder {
        return delegates.getOrNull(viewType)?.onCreateViewHolder(parent) ?: run {
            Timber.e(getErrorMessage(viewType))
            createDefaultItem(parent)
        }
    }

    override fun onBindViewHolder(
        holder: BaseRecyclerViewHolder,
        position: Int,
    ) = holder.bind(currentList[position], emptyList())

    override fun onBindViewHolder(
        holder: BaseRecyclerViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) = holder.bind(currentList[position], payloads)

    override fun getItemViewType(position: Int): Int =
        delegates.indexOfFirst { it.isForValidType(currentList[position]) }

    fun getItemByPosition(position: Int): ViewHolderType? =
        currentList.getOrNull(position)

    fun replace(newItems: List<ViewHolderType>?) {
        submitList(newItems)
    }

    // Will reset scroll position.
    fun replaceFast(newItems: List<ViewHolderType>) {
        replace(null)
        replace(newItems)
    }

    // Replace without diff animations.
    fun replaceAsNew(newItems: List<ViewHolderType>) {
        submitList(emptyList())
        submitList(newItems)
    }

    private fun getErrorMessage(viewType: Int): String {
        val items = currentList.map { it::class.java.simpleName }.toSet()
        val delegates = delegates.map(RecyclerAdapterDelegate::getViewHolderTypeName)
        return "No delegate found for viewType: $viewType items: $items delegates: $delegates"
    }

    companion object {
        fun createDefaultItem(parent: ViewGroup): BaseRecyclerViewHolder {
            return createLoaderAdapterDelegate().onCreateViewHolder(parent)
        }
    }
}