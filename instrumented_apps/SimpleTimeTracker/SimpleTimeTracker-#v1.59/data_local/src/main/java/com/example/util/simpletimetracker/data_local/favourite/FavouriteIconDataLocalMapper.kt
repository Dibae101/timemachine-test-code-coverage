package com.example.util.simpletimetracker.data_local.favourite

import com.example.util.simpletimetracker.domain.favourite.model.FavouriteIcon
import javax.inject.Inject

class FavouriteIconDataLocalMapper @Inject constructor() {

    fun map(dbo: FavouriteIconDBO): FavouriteIcon {
        return FavouriteIcon(
            id = dbo.id,
            icon = dbo.icon,
        )
    }

    fun map(domain: FavouriteIcon): FavouriteIconDBO {
        return FavouriteIconDBO(
            id = domain.id,
            icon = domain.icon,
        )
    }
}