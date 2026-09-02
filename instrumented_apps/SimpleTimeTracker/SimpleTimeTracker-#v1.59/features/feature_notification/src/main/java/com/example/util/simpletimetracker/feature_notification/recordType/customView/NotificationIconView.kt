package com.example.util.simpletimetracker.feature_notification.recordType.customView

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import com.example.util.simpletimetracker.feature_notification.R
import com.example.util.simpletimetracker.feature_notification.databinding.NotificationIconViewLayoutBinding
import com.example.util.simpletimetracker.feature_views.GoalCheckmarkView.CheckState
import com.example.util.simpletimetracker.feature_views.extension.layoutInflater
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon

class NotificationIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(
    context,
    attrs,
    defStyleAttr,
) {

    private val binding = NotificationIconViewLayoutBinding.inflate(layoutInflater, this)

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.NotificationIconView, defStyleAttr, 0,
        ).run {
            if (hasValue(R.styleable.NotificationIconView_itemColor)) {
                itemColor = getColor(R.styleable.NotificationIconView_itemColor, Color.BLACK)
            }

            if (hasValue(R.styleable.NotificationIconView_itemIcon)) {
                itemIcon = getResourceId(R.styleable.NotificationIconView_itemIcon, R.drawable.unknown)
                    .let(RecordTypeIcon::Image)
            }

            if (hasValue(R.styleable.NotificationIconView_itemCheckState)) {
                itemCheckState = getInt(
                    R.styleable.NotificationIconView_itemCheckState,
                    CheckState.HIDDEN.value,
                ).let(CheckState.Companion::fromValue)
            }

            if (hasValue(R.styleable.NotificationIconView_itemIsComplete)) {
                itemIsComplete = getBoolean(R.styleable.NotificationIconView_itemIsComplete, false)
            }

            recycle()
        }
    }

    var itemColor: Int = 0
        set(value) {
            binding.ivNotificationIconBackground.background.colorFilter =
                PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
            field = value
        }

    var itemIcon: RecordTypeIcon = RecordTypeIcon.Image(0)
        set(value) {
            binding.ivNotificationIcon.itemIcon = value
            field = value
        }

    var itemCheckState: CheckState = CheckState.HIDDEN
        set(value) {
            binding.viewNotificationIconCheckmark.itemCheckState = value
            field = value
        }

    var itemIsComplete: Boolean = false
        set(value) {
            binding.viewNotificationIconComplete.isVisible = value
            field = value
        }
}