package com.dot.gallery.feature_node.presentation.support

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.feature_node.presentation.common.components.OptionItem
import com.dot.gallery.feature_node.presentation.common.components.OptionLayout
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSheet(
    state: AppBottomSheetState
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var showCryptoOptions by remember {
        mutableStateOf(false)
    }
    val clipboard = LocalClipboardManager.current
    val mainOptions = remember {
        listOf(
            OptionItem(
                text = "PayPal",
                onClick = {
                    uriHandler.openUri("https://www.paypal.com/paypalme/iacobionut01")
                }
            ),
            OptionItem(
                text = "Revolut",
                onClick = {
                    uriHandler.openUri("https://revolut.me/somaldoaca")
                }
            ),
            OptionItem(
                text = "Crypto",
                onClick = {
                    showCryptoOptions = true
                }
            )
        )
    }
    val cryptoOptions = remember {
        listOf(
            OptionItem(
                text = "ETH",
                summary = "0x707eF0E95e814E05efadFD3d3783401cfbE8D11E",
                onClick = {
                    clipboard.setText(AnnotatedString("0x707eF0E95e814E05efadFD3d3783401cfbE8D11E"))
                }
            ),
            OptionItem(
                text = "POLYGON",
                summary = "0x707eF0E95e814E05efadFD3d3783401cfbE8D11E",
                onClick = {
                    clipboard.setText(AnnotatedString("0x707eF0E95e814E05efadFD3d3783401cfbE8D11E"))
                }
            ),
            OptionItem(
                text = "AVALANCHE",
                summary = "0x707eF0E95e814E05efadFD3d3783401cfbE8D11E",
                onClick = {
                    clipboard.setText(AnnotatedString("0x707eF0E95e814E05efadFD3d3783401cfbE8D11E"))
                }
            ),
            OptionItem(
                text = "BNB",
                summary = "0x707eF0E95e814E05efadFD3d3783401cfbE8D11E",
                onClick = {
                    clipboard.setText(AnnotatedString("0x707eF0E95e814E05efadFD3d3783401cfbE8D11E"))
                }
            ),
            OptionItem(
                text = "XDC",
                summary = "0x707eF0E95e814E05efadFD3d3783401cfbE8D11E",
                onClick = {
                    clipboard.setText(AnnotatedString("0x707eF0E95e814E05efadFD3d3783401cfbE8D11E"))
                }
            ),
            OptionItem(
                text = "EPIC",
                summary = "esWaroB8AQXZuaEJWtraPomNH3Lg1JRP3EVxr6batoFuqvf3hrHP@epicbox.epic.tech",
                onClick = {
                    clipboard.setText(AnnotatedString("esWaroB8AQXZuaEJWtraPomNH3Lg1JRP3EVxr6batoFuqvf3hrHP@epicbox.epic.tech"))
                }
            ),
            OptionItem(
                text = "XRP",
                summary = "rPjYh6XMMra3zHFqDn6ZnYFCmPWPnbTHkc",
                onClick = {
                    clipboard.setText(AnnotatedString("rPjYh6XMMra3zHFqDn6ZnYFCmPWPnbTHkc"))
                }
            ),
            OptionItem(
                text = "ADA",
                summary = "addr1q978p8x80z2d4je5gutav0ypplgtpw0mhmwky8k5mscds9ru9nma366mryl0ln8ump7ysj5wa9sg20c4x7ywjyzvacxseapv0y",
                onClick = {
                    clipboard.setText(AnnotatedString("addr1q978p8x80z2d4je5gutav0ypplgtpw0mhmwky8k5mscds9ru9nma366mryl0ln8ump7ysj5wa9sg20c4x7ywjyzvacxseapv0y"))
                }
            ),
            OptionItem(
                text = "XLM",
                summary = "GAHXB7JI4QEZW2ZM4CA6PQ37Y3636UXWWXDHAZLHCJV5HGXYWQJBIE5Q",
                onClick = {
                    clipboard.setText(AnnotatedString("GAHXB7JI4QEZW2ZM4CA6PQ37Y3636UXWWXDHAZLHCJV5HGXYWQJBIE5Q"))
                }
            ),
            OptionItem(
                text = "INJ",
                summary = "inj1wpl0p627s98qtmadl57n0q6qrna735g72dner7",
                onClick = {
                    clipboard.setText(AnnotatedString("inj1wpl0p627s98qtmadl57n0q6qrna735g72dner7"))
                }
            ),
            OptionItem(
                text = "SUI",
                summary = "0xe135452c381f3298e0ddb17c3e1ede8e1d6aaefb3bc3734219dd6d14ce2177ce",
                onClick = {
                    clipboard.setText(AnnotatedString("0xe135452c381f3298e0ddb17c3e1ede8e1d6aaefb3bc3734219dd6d14ce2177ce"))
                }
            ),
            OptionItem(
                text = "SEI",
                summary = "sei1t5c9dmjdempk7hklw0mm4wkxwzauncvqsgs3l4",
                onClick = {
                    clipboard.setText(AnnotatedString("sei1t5c9dmjdempk7hklw0mm4wkxwzauncvqsgs3l4"))
                }
            ),
            OptionItem(
                text = "HBAR",
                summary = "0.0.4688681-szsjz",
                onClick = {
                    clipboard.setText(AnnotatedString("0.0.4688681-szsjz"))
                }
            )
        )
    }
    if (state.isVisible) {
        BackHandler(showCryptoOptions) {
            showCryptoOptions = false
        }
        ModalBottomSheet(
            sheetState = state.sheetState,
            onDismissRequest = {
                scope.launch {
                    showCryptoOptions = false
                    state.hide()
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle() },
            windowInsets = WindowInsets(0, 0, 0, 0)
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
                            append("Support the project")
                        }
                        if (showCryptoOptions) {
                            append("\n")
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                    letterSpacing = MaterialTheme.typography.bodyMedium.letterSpacing
                                )
                            ) {
                                append("Click to copy")
                            }
                        }
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )
                OptionLayout(
                    modifier = Modifier.fillMaxWidth(),
                    optionList = remember(showCryptoOptions) {
                        if (showCryptoOptions) cryptoOptions else mainOptions
                    }
                )
            }
        }
    }
}