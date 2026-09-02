package com.example.util.simpletimetracker.feature_views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.example.util.simpletimetracker.feature_views.databinding.ItemFilterLayoutBinding
import com.example.util.simpletimetracker.feature_views.extension.dpToPx
import com.example.util.simpletimetracker.feature_views.extension.layoutInflater

class ActivityFilterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(
    context,
    attrs,
    defStyleAttr,
) {

    private val binding = ItemFilterLayoutBinding.inflate(layoutInflater, this, true)

    init {
        context
            .withStyledAttributes(attrs, R.styleable.ActivityFilterView, defStyleAttr, 0) {
                if (hasValue(R.styleable.ActivityFilterView_itemName)) {
                    itemName = getString(R.styleable.ActivityFilterView_itemName).orEmpty()
                }

                if (hasValue(R.styleable.ActivityFilterView_itemColor)) {
                    itemColor = getColor(R.styleable.ActivityFilterView_itemColor, Color.BLACK)
                }
            }

        binding.ivFilterItemButton.isVisible = false
        binding.tvFilterItemName.textSize = 12f
        val paddingVertical = 6.dpToPx()
        binding.tvFilterItemName.updatePadding(top = paddingVertical, bottom = paddingVertical)
    }

    var itemName: String = ""
        set(value) {
            binding.tvFilterItemName.text = value
            field = value
        }

    var itemColor: Int = 0
        set(value) {
            binding.containerFilter.setCardBackgroundColor(value)
            field = value
        }

    var itemBackgroundColor: Int = 0
        set(value) {
            binding.cardFilterBackground.setCardBackgroundColor(value)
            field = value
        }

    var itemBackgroundVisible: Boolean = false
        set(value) {
            binding.cardFilterBackground.isVisible = value
            field = value
        }
}