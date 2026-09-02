package com.example.util.simpletimetracker.feature_icon_selection.api.mapper

import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.domain.favourite.model.FavouriteIcon
import com.example.util.simpletimetracker.domain.icon.IconEmoji
import com.example.util.simpletimetracker.domain.icon.IconImage
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

interface IconSelectionMapper {
    fun mapFavouriteIconImages(
        favourites: List<FavouriteIcon>,
    ): List<IconImage>

    fun mapImageViewData(
        iconName: String,
        iconResId: Int,
        @ColorInt newColor: Int,
    ): ViewHolderType

    fun mapFavouriteIconEmojis(
        favourites: List<FavouriteIcon>,
    ): List<IconEmoji>

    fun mapEmojiViewData(
        codes: String,
        @ColorInt newColor: Int,
    ): ViewHolderType
}