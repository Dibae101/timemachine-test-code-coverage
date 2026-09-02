package com.example.util.simpletimetracker.feature_base_adapter.commentField

import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setMargins
import com.example.util.simpletimetracker.feature_base_adapter.commentField.CommentFieldViewData as ViewData
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemCommentFieldBinding as Binding
import java.util.WeakHashMap

fun createCommentFieldAdapterDelegate(
    afterTextChange: (String) -> Unit = {},
    afterTextChangeWithViewData: (ViewData, String) -> Unit = { _, _ -> },
    onKeyboardButtonClick: (() -> Unit)? = null,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        textWatchers.remove(etCommentItemField)
            ?.let(etCommentItemField::removeTextChangedListener)

        etCommentItemField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onKeyboardButtonClick?.invoke()
            }
            false
        }
        etCommentItemField.inputType = when (item.valueType) {
            is ViewData.ValueType.TextSingleLine -> {
                InputType.TYPE_CLASS_TEXT
            }
            is ViewData.ValueType.TextMultiLine -> {
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            is ViewData.ValueType.NumberDecimal -> {
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            }
        }
        if (item.text != null &&
            item.text != etCommentItemField.text.toString()
        ) {
            etCommentItemField.setText(item.text)
            etCommentItemField.setSelection(item.text.length)
        }
        inputCommentField.hint = item.hint
        inputCommentField.setMargins(
            top = item.marginTopDp,
            start = item.marginHorizontal,
            end = item.marginHorizontal,
        )
        textWatchers[etCommentItemField] = etCommentItemField.doAfterTextChanged {
            afterTextChange(it.toString())
            afterTextChangeWithViewData(item, it.toString())
        }
    }
}

data class CommentFieldViewData(
    val id: Long,
    val text: String?,
    val marginTopDp: Int,
    val marginHorizontal: Int,
    val hint: String,
    val valueType: ValueType,
    val type: Type = Type.Default,
) : ViewHolderType {

    override fun getUniqueId(): Long = id

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ViewData

    sealed interface ValueType {
        data object TextMultiLine : ValueType
        data object TextSingleLine : ValueType
        data object NumberDecimal : ValueType
    }

    interface Type {
        object Default : Type
    }
}

// Avoids setting several watchers and calling onChange several times.
private val textWatchers: MutableMap<EditText, TextWatcher> = WeakHashMap()
