package com.dot.gallery.core.presentation.components.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

val LocalBatteryStatus = compositionLocalOf { BatteryStatus(false) }
private const val POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED"

@Composable
fun ProvideBatteryStatus(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val powerManager = remember(ctx) {
        ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    var batteryStatus by remember(powerManager) {
        mutableStateOf(
            BatteryStatus(
                isPowerSavingMode = powerManager.isPowerSaveMode
            )
        )
    }
    val receiver = remember {
        BatteryStatusReceiver {
            batteryStatus = BatteryStatus(
                isPowerSavingMode = powerManager.isPowerSaveMode
            )
        }
    }
    DisposableEffect(ctx) {
        ContextCompat.registerReceiver(
            ctx,
            receiver,
            IntentFilter(POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            ctx.unregisterReceiver(receiver)
        }
    }

    CompositionLocalProvider(LocalBatteryStatus provides batteryStatus, content = content)
}

class BatteryStatusReceiver(private val onReceive: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        onReceive()
    }

}

@Stable
data class BatteryStatus(
    val isPowerSavingMode: Boolean
)