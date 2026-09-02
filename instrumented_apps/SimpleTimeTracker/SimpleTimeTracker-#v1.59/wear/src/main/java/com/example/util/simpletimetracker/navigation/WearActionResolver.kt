/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.navigation

import android.app.RemoteInput
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearActionResolver @Inject constructor(
    private val resultContainer: WearResultContainer,
) {

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null

    fun registerResultListeners(activity: ComponentActivity) {
        requestPermissionLauncher = activity.registerForRequestPermission(REQUEST_PERMISSION)
        activityResultLauncher = activity.registerForActivityResult(ACTIVITY_RESULT)
    }

    fun requestPermission(
        permissionId: String,
        onGranted: (isGranted: Boolean) -> Unit,
    ) {
        resultContainer.setResultListener(
            key = REQUEST_PERMISSION,
            listener = { onGranted(it as? Boolean == true) },
        )
        requestPermissionLauncher?.launch(permissionId)
    }

    // TODO add comments to wear?
    @Suppress("unused")
    fun openKeyboard(
        label: String,
        onNewText: (String?) -> Unit,
    ) {
        val inputTextKey = "input_text"

        val remoteInputs = RemoteInput.Builder(inputTextKey)
            .setLabel(label)
            .wearableExtender { setEmojisAllowed(false) }
            .build()

        val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInputs))

        resultContainer.setResultListener(
            key = ACTIVITY_RESULT,
            listener = { data ->
                val newText: String? = (data as? ActivityResult)?.data
                    ?.let(RemoteInput::getResultsFromIntent)
                    ?.getCharSequence(inputTextKey)
                    ?.toString()
                onNewText(newText)
            },
        )
        activityResultLauncher?.launch(intent)
    }

    private fun ComponentActivity.registerForRequestPermission(key: String): ActivityResultLauncher<String> {
        return registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            resultContainer.sendResult(key, result)
        }
    }

    private fun ComponentActivity.registerForActivityResult(key: String): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            resultContainer.sendResult(key, result)
        }
    }

    companion object {
        private const val REQUEST_PERMISSION = "REQUEST_PERMISSION"
        private const val ACTIVITY_RESULT = "ACTIVITY_RESULT"
    }
}