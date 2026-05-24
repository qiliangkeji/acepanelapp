package com.acepanel.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

class TabBackStack internal constructor(
    initialIndex: Int,
    initialHistory: List<Int> = emptyList()
) {
    var current by mutableStateOf(initialIndex)
        private set

    private val history = mutableStateListOf<Int>().apply {
        addAll(initialHistory)
    }

    val canGoBack: Boolean
        get() = history.isNotEmpty()

    fun select(index: Int) {
        if (index == current) return
        history.add(current)
        current = index
    }

    fun replace(index: Int) {
        if (index == current) return
        current = index
    }

    fun back(): Boolean {
        if (history.isEmpty()) return false
        current = history.removeAt(history.lastIndex)
        return true
    }

    internal fun historySnapshot(): List<Int> = history.toList()
}

private fun tabBackStackSaver(initialIndex: Int): Saver<TabBackStack, Any> {
    return listSaver<TabBackStack, Int>(
        save = { stack -> listOf(stack.current) + stack.historySnapshot() },
        restore = { saved ->
            val current = saved.firstOrNull() ?: initialIndex
            TabBackStack(current, saved.drop(1))
        }
    )
}

@Composable
fun rememberTabBackStack(initialIndex: Int = 0): TabBackStack {
    return rememberSaveable(saver = tabBackStackSaver(initialIndex)) {
        TabBackStack(initialIndex)
    }
}
