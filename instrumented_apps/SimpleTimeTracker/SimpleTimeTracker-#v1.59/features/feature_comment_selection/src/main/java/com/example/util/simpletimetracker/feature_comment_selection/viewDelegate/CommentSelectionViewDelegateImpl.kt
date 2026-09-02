package com.example.util.simpletimetracker.feature_comment_selection.viewDelegate

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.commentChangeField.createChangeRecordCommentFieldAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.emptySpace.createEmptySpaceAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.hint.createHintAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordComment.createRecordCommentAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.createFilterAdapterDelegate
import com.example.util.simpletimetracker.feature_comment_selection.api.CommentSelectionViewDelegate
import com.example.util.simpletimetracker.feature_comment_selection.api.CommentSelectionViewModelDelegate
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

class CommentSelectionViewDelegateImpl(
    private val viewModel: CommentSelectionViewModelDelegate,
) : CommentSelectionViewDelegate {

    private val commentsAdapter: BaseRecyclerAdapter by lazy {
        BaseRecyclerAdapter(
            createHintAdapterDelegate(),
            createEmptySpaceAdapterDelegate(),
            createChangeRecordCommentFieldAdapterDelegate(
                afterTextChange = viewModel::onCommentChange,
                onFavouriteClick = viewModel::onFavouriteCommentClick,
            ),
            createRecordCommentAdapterDelegate(
                onItemClick = viewModel::onCommentClick,
            ),
            createFilterAdapterDelegate(
                onClick = viewModel::onCommentFilterClick,
                onButtonClick = viewModel::onCommentFilterClick,
            ),
        )
    }

    override fun initUi(
        recycler: RecyclerView,
    ) {
        recycler.apply {
            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.CENTER
                flexWrap = FlexWrap.WRAP
            }
            adapter = commentsAdapter
        }
    }

    override fun <T : ViewBinding> initViewModel(
        fragment: BaseFragment<T>,
    ) = with(viewModel) {
        with(fragment) {
            comments.observe(commentsAdapter::replace)
        }
    }
}