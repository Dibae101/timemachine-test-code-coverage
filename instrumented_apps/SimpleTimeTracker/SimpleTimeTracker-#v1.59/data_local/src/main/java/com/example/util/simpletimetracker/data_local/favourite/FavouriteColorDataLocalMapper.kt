package com.example.util.simpletimetracker.data_local.favourite

import com.example.util.simpletimetracker.domain.favourite.model.FavouriteColor
import javax.inject.Inject

class FavouriteColorDataLocalMapper @Inject constructor() {

    fun map(dbo: FavouriteColorDBO): FavouriteColor {
        return FavouriteColor(
            id = dbo.id,
            colorInt = dbo.colorInt,
        )
    }

    fun map(domain: FavouriteColor): FavouriteColorDBO {
        return FavouriteColorDBO(
            id = domain.id,
            colorInt = domain.colorInt,
        )
    }
}