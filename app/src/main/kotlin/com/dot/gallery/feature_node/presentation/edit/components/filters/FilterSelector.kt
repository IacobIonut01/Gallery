package com.dot.gallery.feature_node.presentation.edit.components.filters

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import com.dot.gallery.feature_node.domain.model.ImageFilter
import com.dot.gallery.feature_node.presentation.edit.EditViewModel
import com.dot.gallery.ui.theme.Shapes
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun FilterSelector(
    filters: List<ImageFilter>,
    viewModel: EditViewModel
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = filters,
            key = { it.name }
        ) {
            FilterItem(
                imageFilter = it,
                currentFilter = viewModel.currentFilter
            ) {
                viewModel.addFilter(it)
            }
        }
    }
}

@Composable
fun FilterItem(
    imageFilter: ImageFilter,
    currentFilter: MutableState<ImageFilter?>,
    onFilterSelect: () -> Unit
) {
    val isSelected = remember (currentFilter.value) {
        currentFilter.value?.name == imageFilter.name ||
                currentFilter.value == null && imageFilter.name == "None"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val widthAnimation by animateDpAsState(
            targetValue = if (isSelected) 4.dp else 0.dp,
            label = "widthAnimation"
        )
        val colorAnimation by animateColorAsState(
            targetValue = if (isSelected) MaterialTheme.colorScheme.tertiary
                else Color.Transparent, label = "colorAnimation"
        )
        Image(
            modifier = Modifier
                .size(92.dp)
                .clip(Shapes.large)
                .border(
                    width = widthAnimation,
                    shape = Shapes.large,
                    color = colorAnimation
                )
                .clickable(
                    enabled = !isSelected,
                    onClick = onFilterSelect
                ),
            painter = rememberDrawablePainter(imageFilter.filterPreview.toDrawable(LocalContext.current.resources)),
            contentScale = ContentScale.Crop,
            contentDescription = imageFilter.name
        )
        Text(
            text = imageFilter.name,
            fontWeight = if (isSelected) FontWeight.Bold
                else FontWeight.Normal
        )
    }
}