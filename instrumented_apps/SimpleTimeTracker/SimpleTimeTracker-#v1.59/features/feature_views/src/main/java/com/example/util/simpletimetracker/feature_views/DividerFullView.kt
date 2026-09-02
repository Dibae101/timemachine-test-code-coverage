package com.example.util.simpletimetracker.feature_views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.util.simpletimetracker.feature_views.databinding.DividerFullViewBinding
import com.example.util.simpletimetracker.feature_views.extension.layoutInflater

class DividerFullView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(
    context,
    attrs,
    defStyleAttr,
) {

    init {
        DividerFullViewBinding.inflate(layoutInflater, this)
    }
}