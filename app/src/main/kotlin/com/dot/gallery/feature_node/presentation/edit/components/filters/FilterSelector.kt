package com.dot.gallery.feature_node.presentation.edit.components.filters

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.feature_node.domain.model.ImageFilter
import com.dot.gallery.feature_node.presentation.edit.EditViewModel
import com.dot.gallery.ui.theme.Shapes

@Composable
fun FilterSelector(
    viewModel: EditViewModel
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
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
                viewModel.applyFilter(it)
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
    val isSelected = currentFilter.value == imageFilter ||
            (currentFilter.value == null && imageFilter.name == "None")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val selectedModifier =
            if (isSelected) {
                Modifier.border(
                    width = 4.dp,
                    shape = Shapes.large,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else Modifier
        Image(
            modifier = Modifier
                .size(92.dp)
                .clip(Shapes.large)
                .then(selectedModifier)
                .clickable(
                    enabled = !isSelected,
                    onClick = onFilterSelect
                ),
            bitmap = imageFilter.filterPreview.asImageBitmap(),
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