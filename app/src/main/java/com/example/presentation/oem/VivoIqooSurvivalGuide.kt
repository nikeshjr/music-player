package com.example.presentation.oem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.oem.OemHelper
import com.example.core.theme.AuraTheme
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassCard
import com.example.presentation.components.GlassSurface

/**
 * VivoIqooSurvivalGuide: Dedicated 5-step survival and permission configuration guide
 * tailored for OriginOS / Funtouch OS on vivo and iQOO devices. Ensures the Dynamic Island
 * overlay and MediaSession background service remain active without OEM process killing.
 */
@Composable
fun VivoIqooSurvivalGuide(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = AuraTheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(theme.primaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "OEM Guide",
                    tint = theme.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "vivo / iQOO Setup Guide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColorPrimary
                )
                Text(
                    text = "5 steps to ensure Dynamic Island stays active",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textColorSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guide Steps
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            item {
                GuideStepItem(
                    stepNumber = 1,
                    icon = Icons.Default.Layers,
                    title = "Display over other apps",
                    description = "Enables drawing the floating Dynamic Island pill over system apps and home screen.",
                    actionLabel = "Open Overlay Settings",
                    onAction = { OemHelper.openOverlaySettings(context) }
                )
            }
            item {
                GuideStepItem(
                    stepNumber = 2,
                    icon = Icons.Default.Visibility,
                    title = "Display pop-ups in background",
                    description = "OriginOS / Funtouch OS special permission required to present overlay cards while Aura is in the background.",
                    actionLabel = "Open Pop-up Settings",
                    onAction = { OemHelper.openVivoBackgroundPopups(context) }
                )
            }
            item {
                GuideStepItem(
                    stepNumber = 3,
                    icon = Icons.Default.PlayArrow,
                    title = "Autostart / Background launch",
                    description = "Allows Aura Music to automatically resume the island after system restart or power cycles.",
                    actionLabel = "Open Autostart Settings",
                    onAction = { OemHelper.openAutostartSettings(context) }
                )
            }
            item {
                GuideStepItem(
                    stepNumber = 4,
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Allow high background power",
                    description = "Set battery behavior to 'Unrestricted' or 'High background power consumption' so audio playback never cuts off.",
                    actionLabel = "Disable Battery Limits",
                    onAction = { OemHelper.requestIgnoreBatteryOptimizations(context) }
                )
            }
            item {
                GuideStepItem(
                    stepNumber = 5,
                    icon = Icons.Default.Lock,
                    title = "Lock in Recent Apps",
                    description = "Swipe up to open Recents, long-press or tap the Aura app icon at the top, and select 'Lock' to prevent clearing.",
                    actionLabel = "Understood",
                    onAction = { /* Informational step */ },
                    isInformationalOnly = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Done & Continue",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun GuideStepItem(
    stepNumber: Int,
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    isInformationalOnly: Boolean = false
) {
    val theme = AuraTheme.current

    GlassSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stepNumber",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = theme.secondaryColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textColorPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = theme.textColorSecondary,
                lineHeight = 16.sp
            )

            if (!isInformationalOnly) {
                Spacer(modifier = Modifier.height(10.dp))
                GlassButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = actionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
