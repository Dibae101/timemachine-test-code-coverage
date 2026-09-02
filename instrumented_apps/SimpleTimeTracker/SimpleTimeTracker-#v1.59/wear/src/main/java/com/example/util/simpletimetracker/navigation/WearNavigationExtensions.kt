package com.example.util.simpletimetracker.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.navigation.composable

internal fun <DATA, ROUTE : WearNavigationRoute<DATA>> NavGraphBuilder.composable(
    route: ROUTE,
    content: @Composable (ROUTE, Bundle?) -> Unit,
) {
    composable(
        route = route.baseRoute,
        content = { navBackStackEntry ->
            content(route, navBackStackEntry.arguments)
        },
    )
}

internal fun <DATA> NavController.navigate(
    route: WearNavigationRoute<DATA>,
    data: DATA? = null,
) {
    navigate(route.build(data))
}

inline fun <reified DATA> WearNavigationRoute<DATA>.get(bundle: Bundle?): DATA? {
    val data = bundle?.getString(key)
    return when (DATA::class) {
        // Add more types when necessary.
        Int::class -> data?.toIntOrNull() as? DATA
        Long::class -> data?.toLongOrNull() as? DATA
        String::class -> data as? DATA
        else -> null
    }
}

private fun <DATA> WearNavigationRoute<DATA>.build(data: DATA? = null): String {
    return data?.let { baseRoute.replace(wrappedKey(), data.toString()) } ?: baseRoute
}
