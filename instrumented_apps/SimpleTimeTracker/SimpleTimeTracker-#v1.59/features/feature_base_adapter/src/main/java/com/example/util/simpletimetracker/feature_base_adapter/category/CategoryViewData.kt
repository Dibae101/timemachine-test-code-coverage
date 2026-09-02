package com.example.util.simpletimetracker.feature_base_adapter.category

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon

sealed class CategoryViewData : ViewHolderType {
    abstract val id: Long
    abstract val name: String
    abstract val iconColor: Int
    abstract val color: Int
    open val type: Type = Type.Default

    override fun getUniqueId(): Long = id

    data class Category(
        override val id: Long,
        override val name: String,
        @ColorInt override val iconColor: Int,
        @ColorInt override val color: Int,
        override val type: Type = Type.Default,
    ) : CategoryViewData() {

        override fun isValidType(other: ViewHolderType): Boolean = other is Category &&
            other.type == type
    }

    sealed class Record : CategoryViewData() {
        abstract val icon: RecordTypeIcon?
        abstract val iconAlpha: Float

        data class Tagged(
            override val id: Long,
            override val name: String,
            @ColorInt override val iconColor: Int,
            @ColorInt override val color: Int,
            override val icon: RecordTypeIcon?,
            override val iconAlpha: Float = 1.0f,
        ) : Record() {

            override fun isValidType(other: ViewHolderType): Boolean = other is Tagged &&
                other.type == type
        }

        data class Untagged(
            override val id: Long,
            override val name: String,
            @ColorInt override val iconColor: Int,
            @ColorInt override val color: Int,
            override val icon: RecordTypeIcon?,
            override val iconAlpha: Float = 1.0f,
        ) : Record() {

            override fun isValidType(other: ViewHolderType): Boolean = other is Untagged &&
                other.type == type
        }
    }

    interface Type {
        data object Default : Type
    }
}