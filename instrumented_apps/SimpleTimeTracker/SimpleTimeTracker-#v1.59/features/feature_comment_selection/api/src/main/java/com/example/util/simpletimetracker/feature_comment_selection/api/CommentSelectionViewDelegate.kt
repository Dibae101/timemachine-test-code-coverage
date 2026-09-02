package com.example.util.simpletimetracker.feature_comment_selection.api

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.util.simpletimetracker.core.base.BaseFragment

interface CommentSelectionViewDelegate {
    fun initUi(recycler: RecyclerView)
    fun <T : ViewBinding> initViewModel(fragment: BaseFragment<T>)
}