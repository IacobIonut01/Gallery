package com.dot.gallery.feature_node.presentation.edit.components

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.dot.gallery.feature_node.presentation.edit.components.EditId.ADJUST
import com.dot.gallery.feature_node.presentation.edit.components.EditId.CROP
import com.dot.gallery.feature_node.presentation.edit.components.EditId.FILTERS
import com.dot.gallery.feature_node.presentation.edit.components.EditId.MARKUP
import com.dot.gallery.feature_node.presentation.edit.components.EditId.MORE
import com.dot.gallery.ui.theme.GalleryTheme
import kotlinx.parcelize.Parcelize

@Composable
fun EditOptions(
    modifier: Modifier = Modifier,
    selectedOption: MutableState<EditOption>,
    options: SnapshotStateList<EditOption>
) {
    val filteredOptions = remember(options.size) {
        options.filter { it.isEnabled }
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        userScrollEnabled = true,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        items(
            items = filteredOptions,
        ) {
            InputChip(
                colors = InputChipDefaults.inputChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null,
                shape = CircleShape,
                selected = selectedOption.value == it,
                onClick = {
                    selectedOption.value = it
                },
                label = {
                    Text(text = it.title)
                }
            )
        }
    }
}

@Preview(showBackground = true, wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE)
@Composable
private fun Preview() {
    GalleryTheme(
        darkTheme = true
    ) {
        Surface(
            color = Color.Black
        ) {
            val options = remember {
                listOf(
                    EditOption(
                        title = "Crop",
                        id = CROP
                    ),
                    EditOption(
                        title = "Adjust",
                        id = ADJUST
                    ),
                    EditOption(
                        title = "Filters",
                        id = FILTERS
                    ),
                    EditOption(
                        title = "Markup",
                        id = MARKUP
                    ),
                    EditOption(
                        title = "More",
                        id = MORE
                    ),
                ).toMutableStateList()
            }
            EditOptions(
                selectedOption = remember { mutableStateOf(options.first()) },
                options = options
            )
        }
    }
}

@Parcelize
data class EditOption(
    val id: EditId,
    val title: String,
    val isSelected: Boolean = false,
    val isEnabled: Boolean = true,
) : Parcelable

@Parcelize
enum class EditId : Parcelable {
    CROP, ADJUST, FILTERS, MARKUP, MORE
}