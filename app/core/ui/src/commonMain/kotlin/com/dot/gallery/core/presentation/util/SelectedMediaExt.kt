/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.util.fastFilter
import com.dot.gallery.core.ui.util.rememberedDerivedState
import com.dot.gallery.domain.model.Media

@Composable
fun <T : Media> selectedMedia(
    media: List<T>,
    selectedSet: State<Set<Long>>
): State<SnapshotStateList<T>> = rememberedDerivedState(media, selectedSet.value) {
    media.fastFilter { selectedSet.value.contains(it.id) }.toMutableStateList()
}

@Composable
fun <T : Media> List<T>.selectedMedia(selectedSet: State<Set<Long>>) =
    remember(this, selectedSet.value) { filter { selectedSet.value.contains(it.id) }.toMutableStateList() }

val <T> MutableState<Set<T>>.size get() = value.size

fun <T> MutableState<Set<T>>.clear() {
    value = emptySet()
}

fun <T> MutableState<Set<T>>.add(item: T) {
    value = value.plus(item)
}

fun <T> MutableState<Set<T>>.add(items: Array<out T>) {
    value = value.plus(items)
}

fun <T> MutableState<Set<T>>.add(items: Collection<T>) {
    value = value.plus(items.toSet())
}

fun <T> MutableState<Set<T>>.remove(item: T) {
    value = value.minus(item)
}

fun <T> MutableState<Set<T>>.remove(items: Array<out T>) {
    value = value.minus(items.toSet())
}

fun <T> MutableState<Set<T>>.remove(items: Collection<T>) {
    value = value.minus(items.toSet())
}

fun <T> MutableState<T>.update(newState: T) {
    value = newState
}
