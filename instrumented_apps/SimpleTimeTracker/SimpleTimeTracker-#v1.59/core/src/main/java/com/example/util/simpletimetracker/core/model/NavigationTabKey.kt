package com.example.util.simpletimetracker.core.model

import dagger.MapKey
import kotlin.reflect.KClass

@MapKey
@Retention(AnnotationRetention.RUNTIME)
annotation class NavigationTabKey(val value: KClass<out NavigationTab>)