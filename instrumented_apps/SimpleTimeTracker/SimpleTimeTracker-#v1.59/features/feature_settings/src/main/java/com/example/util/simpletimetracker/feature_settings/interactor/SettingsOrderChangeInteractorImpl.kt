package com.example.util.simpletimetracker.feature_settings.interactor

import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordTag.model.CardTagOrder
import com.example.util.simpletimetracker.domain.recordType.model.CardOrder
import com.example.util.simpletimetracker.feature_settings.api.SettingsOrderChangeInteractor
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.CardOrderDialogParams
import javax.inject.Inject

class SettingsOrderChangeInteractorImpl @Inject constructor(
    private val router: Router,
    private val prefsInteractor: PrefsInteractor,
) : SettingsOrderChangeInteractor {

    override suspend fun onOrderSelected(type: CardOrderDialogParams.Type) {
        when (type) {
            is CardOrderDialogParams.Type.RecordType -> {
                val currentOrder = prefsInteractor.getCardOrder()
                val newOrder = type.order
                if (newOrder == currentOrder) return
                if (newOrder == CardOrder.MANUAL) openOrderDialog(type.copy(order = currentOrder))
                prefsInteractor.setCardOrder(newOrder)
            }
            is CardOrderDialogParams.Type.Category -> {
                val currentOrder = prefsInteractor.getCategoryOrder()
                val newOrder = type.order
                if (newOrder == currentOrder) return
                if (newOrder == CardOrder.MANUAL) openOrderDialog(type.copy(order = currentOrder))
                prefsInteractor.setCategoryOrder(newOrder)
            }
            is CardOrderDialogParams.Type.Tag -> {
                val currentOrder = prefsInteractor.getTagOrder()
                val newOrder = type.order
                if (newOrder == currentOrder) return
                if (newOrder == CardTagOrder.MANUAL) openOrderDialog(type.copy(order = currentOrder))
                prefsInteractor.setTagOrder(newOrder)
            }
        }
    }

    override fun openOrderDialog(type: CardOrderDialogParams.Type) {
        router.navigate(CardOrderDialogParams(type))
    }
}