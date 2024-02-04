package com.dot.gallery.feature_node.presentation.edit.components.adjustments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dot.gallery.feature_node.presentation.edit.EditViewModel
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

@Composable
fun AdjustmentSelector(
    selectedFilter: MutableState<Pair<AdjustmentFilter, Float>?>,
    viewModel: EditViewModel
) {
    val adjustmentFilters = EditViewModel.adjustmentFilters
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            items = adjustmentFilters,
            key = { it.tag }
        ) { filter ->
            val isSelected = remember(selectedFilter.value) { selectedFilter.value?.first == filter }
            val isApplied = remember(viewModel.modifications) { viewModel.modifications.any { it.adjustment?.first == filter } }
            AdjustmentItem(
                filter = filter,
                isSelected = isSelected,
                isApplied = isApplied,
                onClick = {
                    if (isSelected) {
                        selectedFilter.value = null
                        viewModel.addAdjustment(isScrolling = false, filter to filter.defaultValue)
                    } else {
                        selectedFilter.value = filter to filter.defaultValue
                    }
                }
            )
        }
    }
}

@Composable
fun AdjustmentItem(
    filter: AdjustmentFilter,
    isSelected: Boolean,
    isApplied: Boolean,
    onClick: () -> Unit = { }
) {
    val backgroundColor: Color = if (isApplied && isSelected) {
        MaterialTheme.colorScheme.primary
    } else if (isApplied) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor: Color = if (isApplied && isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else if (isApplied) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else if (isSelected) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = backgroundColor,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                imageVector = filter.icon,
                contentDescription = filter.name,
                tint = contentColor
            )
        }
        Text(text = filter.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
    }
}

@Immutable
data class AdjustmentFilter(
    val tag: Adjustment,
    val name: String,
    val icon: ImageVector,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float,
    val filter: (Float) -> GPUImageFilter
)

enum class Adjustment {
    CONTRAST,
    BRIGHTNESS,
    SATURATION
}