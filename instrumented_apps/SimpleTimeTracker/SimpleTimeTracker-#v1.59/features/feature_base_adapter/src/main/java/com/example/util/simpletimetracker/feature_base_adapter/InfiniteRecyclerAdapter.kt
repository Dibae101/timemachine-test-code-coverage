package com.example.util.simpletimetracker.feature_base_adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter

class InfiniteRecyclerAdapter(
    private val dataProvider: DataProvider,
    vararg delegatesList: RecyclerAdapterDelegate,
    diffUtilCallback: DiffUtilCallback = DiffUtilCallback(),
) : ListAdapter<ViewHolderType, BaseRecyclerViewHolder>(diffUtilCallback) {

    private val delegates: List<RecyclerAdapterDelegate> = delegatesList.toList()

    // "Infinite" recycler.
    override fun getItemCount(): Int {
        if (!dataProvider.isInitialized()) return 0

        return when (dataProvider.getCount()) {
            is DataProvider.Count.Infinite -> Int.MAX_VALUE
            is DataProvider.Count.One -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRecyclerViewHolder {
        return delegates.getOrNull(viewType)?.onCreateViewHolder(parent) ?: run {
            BaseRecyclerAdapter.createDefaultItem(parent)
        }
    }

    override fun onBindViewHolder(
        holder: BaseRecyclerViewHolder,
        position: Int,
    ) = holder.bind(dataProvider.getItem(toShift(position)), emptyList())

    override fun onBindViewHolder(
        holder: BaseRecyclerViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) = holder.bind(dataProvider.getItem(toShift(position)), payloads)

    override fun getItemViewType(position: Int): Int =
        delegates.indexOfFirst { it.isForValidType(dataProvider.getCurrentItem()) }

    interface DataProvider {
        fun isInitialized(): Boolean
        fun getCount(): Count
        fun getItem(position: Int): Data
        fun getCurrentItem(): Data

        sealed interface Count {
            data object Infinite : Count
            data object One : Count
        }
    }

    interface Data : ViewHolderType {
        val position: Int

        override fun getUniqueId(): Long = position.toLong()

        // Update always.
        override fun areItemsTheSame(other: ViewHolderType): Boolean = false
        override fun areContentsTheSame(other: ViewHolderType): Boolean = false
    }

    fun toShift(position: Int): Int {
        return when (dataProvider.getCount()) {
            is DataProvider.Count.Infinite -> position - FIRST
            is DataProvider.Count.One -> 0
        }
    }

    fun toPosition(shift: Int): Int {
        return shift + FIRST
    }

    companion object {
        // First page is at the center of range.
        private const val FIRST = Int.MAX_VALUE / 2
        const val TEST_TAG = "InfiniteRecyclerAdapter"
    }
}