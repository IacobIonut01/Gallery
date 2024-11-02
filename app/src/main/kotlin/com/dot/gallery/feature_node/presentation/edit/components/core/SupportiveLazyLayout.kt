package com.dot.gallery.feature_node.presentation.edit.components.core

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SupportiveLazyLayout(
    modifier: Modifier = Modifier,
    isSupportingPanel: Boolean,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: LazyListScope.() -> Unit
) {
    if (!isSupportingPanel) {
        LazyRow(
            modifier = modifier
                .animateContentSize()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            contentPadding = contentPadding,
            content = content
        )
    } else {
        LazyColumn(
            modifier = modifier
                .animateContentSize()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
            contentPadding = contentPadding,
            content = content
        )
    }
}