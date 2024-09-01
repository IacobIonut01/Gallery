package com.dot.gallery.feature_node.presentation.ignored.setup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.SetupWizard
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.ui.core.Icons
import com.dot.gallery.ui.core.icons.RegularExpression

@Composable
fun SetupTypeRegexScreen(
    onGoBack: () -> Unit,
    onNext: () -> Unit,
    initialRegex: String,
    ignoredAlbums: List<IgnoredAlbum>,
    onRegexChanged: (String) -> Unit,
) {
    var regex by remember { mutableStateOf(initialRegex) }
    var error by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val invalidRegex = stringResource(R.string.setup_type_regex_error)
    val alreadyUsedRegex = stringResource(R.string.setup_type_regex_error_second)
    LaunchedEffect(regex) {
        val validRegex = try {
            regex.toRegex()
            true
        } catch (e: Exception) {
            false
        }
        error = !validRegex
        if (error) errorMessage = invalidRegex
        error = ignoredAlbums.any { it.wildcard == regex }
        if (error) errorMessage = alreadyUsedRegex

        if (!error) {
            errorMessage = ""
            onRegexChanged(regex)
        }

    }

    SetupWizard(
        title = stringResource(R.string.setup_type_regex_title),
        subtitle = stringResource(R.string.setup_type_regex_subtitle),
        icon = Icons.RegularExpression,
        contentPadding = 0.dp,
        bottomBar = {
            OutlinedButton(
                onClick = onGoBack
            ) {
                Text(text = stringResource(id = R.string.go_back))
            }

            Button(
                onClick = onNext,
                enabled = regex.isNotEmpty()
            ) {
                Text(text = stringResource(R.string.continue_string))
            }
        },
        content = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = regex,
                onValueChange = { regex = it },
                label = { Text(stringResource(R.string.setup_type_regex_label)) },
                placeholder = { Text(stringResource(R.string.setup_type_regex_label)) },
                supportingText = if (error) {{
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }} else null,
                isError = error,
                singleLine = true,
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                text = stringResource(R.string.setup_type_regex_summary)
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                text = buildAnnotatedString {
                    withStyle(
                        style = MaterialTheme.typography.bodyMedium.toSpanStyle()
                    ) {
                        appendLine(stringResource(R.string.setup_type_regex_example_title))
                    }
                    withStyle(
                        style = MaterialTheme.typography.bodySmall.toSpanStyle()
                    ) {
                        appendLine(stringResource(R.string.setup_type_regex_example_first_title))
                    }

                    withStyle(
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        ).toSpanStyle()
                    ) {
                        appendLine(stringResource(R.string.setup_type_regex_first_subtitle))
                    }

                    appendLine()

                    withStyle(
                        style = MaterialTheme.typography.bodySmall.toSpanStyle()
                    ) {
                        appendLine(stringResource(R.string.setup_type_regex_example_second_title))
                    }

                    withStyle(
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        ).toSpanStyle()
                    ) {
                        appendLine(stringResource(R.string.setup_type_regex_example_second_subtitle))
                    }

                    appendLine()

                    withStyle(
                        style = MaterialTheme.typography.bodySmall.toSpanStyle()
                    ) {
                        appendLine(stringResource(R.string.setup_type_regex_example_third_title))
                    }

                    withStyle(
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        ).toSpanStyle()
                    ) {
                        appendLine(stringResource(R.string.setup_type_regex_example_third_subtitle))
                    }
                }
            )
        }
    )
}
