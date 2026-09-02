package com.example.util.simpletimetracker.utils

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import org.hamcrest.CoreMatchers.`is`

fun recyclerItemCount(expectedCount: Int) =
    ViewAssertion { view, noViewFoundException ->
        if (noViewFoundException != null) {
            throw noViewFoundException
        }
        val itemCount = (view as RecyclerView).adapter!!.itemCount
        assertThat(itemCount, `is`(expectedCount))
    }

inline fun <reified T : ViewHolderType> recyclerViewHolderCount(expectedCount: Int) =
    ViewAssertion { view, noViewFoundException ->
        if (noViewFoundException != null) {
            throw noViewFoundException
        }
        val adapter = (view as RecyclerView).adapter as BaseRecyclerAdapter
        val itemCount = (0..adapter.itemCount)
            .mapNotNull(adapter::getItemByPosition)
            .filterIsInstance<T>().size
        assertThat(itemCount, `is`(expectedCount))
    }