package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalSheet(
    sheetState: AppBottomSheetState,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onDismissRequest: suspend () -> Unit = {},
    dragHandle: @Composable (() -> Unit)? = { DragHandle() },
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    tonalElevation: Dp = 0.dp,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
) {
    val scope = rememberCoroutineScope()
    if (sheetState.isVisible) {
        ModalBottomSheet(
            modifier = modifier,
            sheetState = sheetState.sheetState,
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    onDismissRequest()
                }
            },
            dragHandle = dragHandle?.let { { it() } },
            containerColor = containerColor,
            tonalElevation = tonalElevation,
            shape = shape
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                title?.let {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing
                                )
                            ) {
                                append(title)
                            }
                            subtitle?.let {
                                append("\n")
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                        letterSpacing = MaterialTheme.typography.bodyMedium.letterSpacing
                                    )
                                ) {
                                    append(it)
                                }
                            }
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    )
                }

                content()

            }
        }
    }
}