package com.dot.gallery.feature.viewer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.ModalSheet
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.domain.model.Media
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.rename_collection
import com.dot.gallery.feature.viewer.generated.resources.rename_media_description
import com.dot.gallery.feature.viewer.generated.resources.rename_media_name_label


/**
 * Rename button with its label-entry sheet. The actual rename (handler call,
 * MediaStore write request, media scan) happens app-side inside [onRename].
 */
@Composable
fun <T : Media> RenameButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onRename: (T, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberAppBottomSheetState()
    var newLabel by rememberSaveable(media) { mutableStateOf(media.label) }
    val focusRequester = remember { FocusRequester() }

    val confirmRename: () -> Unit = {
        if (newLabel.isNotBlank() && newLabel != media.label) {
            scope.launch {
                sheetState.hide()
                onRename(media, newLabel)
            }
        }
    }

    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.DriveFileRenameOutline,
        followTheme = followTheme,
        title = stringResource(Res.string.rename_collection),
        enabled = enabled
    ) {
        newLabel = it.label
        scope.launch { sheetState.show() }
    }

    ModalSheet(
        sheetState = sheetState,
        title = stringResource(Res.string.rename_collection),
        subtitle = stringResource(Res.string.rename_media_description),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = {
            OutlinedTextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                label = { Text(stringResource(Res.string.rename_media_name_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .imePadding(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirmRename() })
            )
            Spacer(Modifier.height(16.dp))
            SetupButton(
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                enabled = newLabel.isNotBlank() && newLabel != media.label,
                text = stringResource(Res.string.rename_collection),
                onClick = confirmRename
            )
            LaunchedEffect(sheetState.isVisible) {
                if (sheetState.isVisible) focusRequester.requestFocus()
            }
        }
    )
}
