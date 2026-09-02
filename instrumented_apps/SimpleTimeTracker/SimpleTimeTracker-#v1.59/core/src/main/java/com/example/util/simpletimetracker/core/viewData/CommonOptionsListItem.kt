package com.example.util.simpletimetracker.core.viewData

sealed interface CommonOptionsListItem {

    interface EnabledSearch : CommonOptionsListItem
    interface Filter : CommonOptionsListItem
    interface Share : CommonOptionsListItem
    interface SelectDate : CommonOptionsListItem
    interface SelectRange : CommonOptionsListItem
    interface BackToToday : CommonOptionsListItem
}