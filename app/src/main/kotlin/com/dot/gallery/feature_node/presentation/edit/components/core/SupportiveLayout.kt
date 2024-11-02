package com.dot.gallery.feature_node.presentation.edit.components.core

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SupportiveLayout(
    modifier: Modifier = Modifier,
    isSupportingPanel: Boolean,
    content: @Composable () -> Unit
) {
    if (isSupportingPanel) {
        Row(
            modifier = modifier
                .animateContentSize()
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                content()
            }
        )
    } else {
        Column(
            modifier = modifier
                .animateContentSize()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                content()
            }
        )
    }
}