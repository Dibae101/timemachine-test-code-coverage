package com.example.util.simpletimetracker.di

import com.example.util.simpletimetracker.navigation.params.screen.ScreenParams
import dagger.MapKey
import kotlin.reflect.KClass

@MapKey
@Retention(AnnotationRetention.RUNTIME)
annotation class ScreenKey(val value: KClass<out ScreenParams>)
