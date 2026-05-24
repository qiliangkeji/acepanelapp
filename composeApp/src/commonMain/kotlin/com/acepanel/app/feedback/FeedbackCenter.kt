package com.acepanel.app.feedback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class FeedbackTone {
    Info,
    Success,
    Warning,
    Error
}

data class FeedbackMessage(
    val id: Long,
    val tone: FeedbackTone,
    val title: String,
    val detail: String? = null
)

data class FeedbackOperation(
    val id: Long,
    val title: String,
    val detail: String? = null
)

data class FeedbackUiState(
    val activeOperations: List<FeedbackOperation> = emptyList(),
    val messages: List<FeedbackMessage> = emptyList()
)

object FeedbackCenter {
    private const val MaxMessages = 3

    private var nextId = 1L
    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state

    fun begin(title: String, detail: String? = null): Long {
        val id = newId()
        _state.update { current ->
            current.copy(
                activeOperations = current.activeOperations + FeedbackOperation(
                    id = id,
                    title = title,
                    detail = detail
                )
            )
        }
        return id
    }

    fun succeed(operationId: Long?, title: String, detail: String? = null) {
        finish(operationId)
        show(FeedbackTone.Success, title, detail)
    }

    fun fail(operationId: Long?, title: String, detail: String? = null) {
        finish(operationId)
        show(FeedbackTone.Error, title, detail)
    }

    fun finish(operationId: Long?) {
        if (operationId == null) return
        _state.update { current ->
            current.copy(activeOperations = current.activeOperations.filterNot { it.id == operationId })
        }
    }

    fun info(title: String, detail: String? = null) {
        show(FeedbackTone.Info, title, detail)
    }

    fun success(title: String, detail: String? = null) {
        show(FeedbackTone.Success, title, detail)
    }

    fun warning(title: String, detail: String? = null) {
        show(FeedbackTone.Warning, title, detail)
    }

    fun error(title: String, detail: String? = null) {
        show(FeedbackTone.Error, title, detail)
    }

    fun dismissMessage(id: Long) {
        _state.update { current ->
            current.copy(messages = current.messages.filterNot { it.id == id })
        }
    }

    private fun show(tone: FeedbackTone, title: String, detail: String?) {
        val message = FeedbackMessage(
            id = newId(),
            tone = tone,
            title = title,
            detail = detail?.takeIf { it.isNotBlank() }
        )
        _state.update { current ->
            val nextMessages = (current.messages + message).takeLast(MaxMessages)
            current.copy(messages = nextMessages)
        }
    }

    private fun newId(): Long = nextId++
}
