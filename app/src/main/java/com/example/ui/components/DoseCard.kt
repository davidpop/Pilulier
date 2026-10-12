package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DoseItem
import com.example.data.model.DoseStatus
import com.example.ui.theme.StatusOverdueAmber
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusTakenBg
import com.example.ui.theme.StatusTakenGreen
import com.example.ui.theme.StatusUpcomingBg
import com.example.ui.theme.StatusUpcomingBlue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DoseCard(
    dose: DoseItem,
    onMarkTaken: () -> Unit,
    onUnmarkTaken: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val scheduledTimeStr = remember(dose.scheduledDateTime) {
        val time = Instant.ofEpochMilli(dose.scheduledDateTime).atZone(zone).toLocalTime()
        time.format(DateTimeFormatter.ofPattern("HH'h'mm"))
    }

    val confirmedTimeStr = remember(dose.confirmedAt) {
        dose.confirmedAt?.let {
            val time = Instant.ofEpochMilli(it).atZone(zone).toLocalTime()
            time.format(DateTimeFormatter.ofPattern("HH'h'mm"))
        }
    }

    val cardColor = when (dose.status) {
        DoseStatus.CONFIRMED -> MaterialTheme.colorScheme.surface
        DoseStatus.OVERDUE -> MaterialTheme.colorScheme.surface
        DoseStatus.PENDING -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dose_card_${dose.medicationId}_${dose.scheduledDateTime}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Ligne supérieure : Heure prévue et Badge de statut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (dose.status) {
                                    DoseStatus.CONFIRMED -> StatusTakenBg
                                    DoseStatus.OVERDUE -> StatusOverdueBg
                                    DoseStatus.PENDING -> StatusUpcomingBg
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (dose.status) {
                                DoseStatus.CONFIRMED -> Icons.Default.Check
                                DoseStatus.OVERDUE -> Icons.Default.Schedule
                                DoseStatus.PENDING -> Icons.Default.AccessTime
                            },
                            contentDescription = null,
                            tint = when (dose.status) {
                                DoseStatus.CONFIRMED -> StatusTakenGreen
                                DoseStatus.OVERDUE -> StatusOverdueAmber
                                DoseStatus.PENDING -> StatusUpcomingBlue
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = scheduledTimeStr,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Badge de statut
                StatusBadge(status = dose.status, confirmedTime = confirmedTimeStr, isSnoozed = dose.isSnoozed)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nom du médicament et dosage
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dose.medicationName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (dose.dosage.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = dose.dosage,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Quantité & forme si renseigné
            val quantityForm = buildString {
                if (dose.quantityPerDose.isNotBlank()) append(dose.quantityPerDose)
                if (dose.form.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(dose.form)
                }
            }
            if (quantityForm.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = quantityForm,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Instructions de prise
            if (dose.instructions.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dose.instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Snooze notice
            if (dose.isSnoozed && dose.status != DoseStatus.CONFIRMED) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = StatusOverdueAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rappel différé actif",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusOverdueAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions disponibles selon le statut
            when (dose.status) {
                DoseStatus.CONFIRMED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusTakenGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Prise enregistrée avec succès",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = StatusTakenGreen
                            )
                        }
                        TextButton(
                            onClick = onUnmarkTaken,
                            modifier = Modifier.testTag("undo_taken_button_${dose.medicationId}")
                        ) {
                            Text(
                                text = "Annuler",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                DoseStatus.OVERDUE, DoseStatus.PENDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onMarkTaken,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("mark_taken_button_${dose.medicationId}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Marquer comme pris",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onSnooze,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("snooze_button_${dose.medicationId}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rappeler",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: DoseStatus,
    confirmedTime: String?,
    isSnoozed: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        DoseStatus.CONFIRMED -> {
            val text = if (confirmedTime != null) "Pris à $confirmedTime" else "Prise confirmée"
            Triple(StatusTakenBg, StatusTakenGreen, text)
        }
        DoseStatus.OVERDUE -> {
            Triple(StatusOverdueBg, StatusOverdueAmber, "Non confirmée")
        }
        DoseStatus.PENDING -> {
            Triple(StatusUpcomingBg, StatusUpcomingBlue, "À venir")
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
