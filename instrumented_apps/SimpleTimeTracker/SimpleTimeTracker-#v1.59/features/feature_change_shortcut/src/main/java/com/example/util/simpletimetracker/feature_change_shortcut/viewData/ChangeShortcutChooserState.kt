package com.example.util.simpletimetracker.feature_change_shortcut.viewData

import com.example.util.simpletimetracker.core.view.ViewChooserStateDelegate

sealed interface ChangeShortcutChooserState : ViewChooserStateDelegate.State {
    object Closed : ChangeShortcutChooserState, ViewChooserStateDelegate.State.Closed
    object Activity : ChangeShortcutChooserState
    object Tag : ChangeShortcutChooserState
    object Comment : ChangeShortcutChooserState
    object SettingAction : ChangeShortcutChooserState
}
