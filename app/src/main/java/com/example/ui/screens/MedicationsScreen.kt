package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MedicationWithSchedules
import com.example.ui.theme.StatusOverdueAmber
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusTakenBg
import com.example.ui.theme.StatusTakenGreen
import com.example.ui.theme.StatusUpcomingBg
import com.example.ui.theme.StatusUpcomingBlue
import com.example.ui.viewmodel.PilulierViewModel
import com.example.ui.viewmodel.TreatmentFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class TreatmentStatus(val label: String) {
    ACTIVE("En cours"),
    UPCOMING("À venir"),
    PAUSED("Suspendu"),
    FINISHED("Terminé")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationsScreen(
    viewModel: PilulierViewModel,
    modifier: Modifier = Modifier
) {
    val treatments by viewModel.allTreatments.collectAsStateWithLifecycle()
    val activeFilter by viewModel.treatmentFilter.collectAsStateWithLifecycle()

    var treatmentToDelete by remember { mutableStateOf<MedicationWithSchedules?>(null) }
    var treatmentToViewDetails by remember { mutableStateOf<MedicationWithSchedules?>(null) }
    var deleteHistoryChecked by remember { mutableStateOf(false) }

    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    // Dialogue de consultation détaillée
    if (treatmentToViewDetails != null) {
        val target = treatmentToViewDetails!!
        val med = target.medication
        val start = Instant.ofEpochMilli(med.startDate).atZone(zone).toLocalDate()
        val end = med.endDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        val daysCount = end?.let { ChronoUnit.DAYS.between(start, it) + 1 }

        val status = when {
            med.isPaused -> TreatmentStatus.PAUSED
            today.isBefore(start) -> TreatmentStatus.UPCOMING
            end != null && today.isAfter(end) -> TreatmentStatus.FINISHED
            else -> TreatmentStatus.ACTIVE
        }

        AlertDialog(
            onDismissRequest = { treatmentToViewDetails = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = med.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    if (med.dosage.isNotBlank()) {
                        Text(
                            text = med.dosage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Statut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Statut actuel :",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = when (status) {
                                TreatmentStatus.ACTIVE -> StatusTakenGreen
                                TreatmentStatus.UPCOMING -> StatusUpcomingBlue
                                TreatmentStatus.PAUSED -> StatusOverdueAmber
                                TreatmentStatus.FINISHED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Forme & Quantité
                    if (med.form.isNotBlank() || med.quantityPerDose.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Présentation :",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = buildString {
                                    if (med.form.isNotBlank()) append(med.form)
                                    if (med.quantityPerDose.isNotBlank()) {
                                        if (isNotEmpty()) append(" (")
                                        append(med.quantityPerDose)
                                        if (isNotEmpty()) append(")")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Période
                    Column {
                        Text(
                            text = "Période du traitement :",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (end != null) {
                                "Du ${start.format(dateFormatter)} au ${end.format(dateFormatter)} ($daysCount jours)"
                            } else {
                                "À partir du ${start.format(dateFormatter)} (Traitement continu)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Horaires quotidiens
                    Column {
                        Text(
                            text = "Horaires quotidiens (${target.schedules.size}) :",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            target.sortedSchedules().forEach { sched ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = sched.formattedTime(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Instructions
                    if (med.instructions.isNotBlank()) {
                        Column {
                            Text(
                                text = "Instructions :",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = med.instructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Raccourci modification rapide
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.toggleMedicationPaused(med.id, !med.isPaused)
                                treatmentToViewDetails = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (med.isPaused) "Réactiver" else "Suspendre",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.openEditMedication(target)
                                treatmentToViewDetails = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Modifier", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { treatmentToViewDetails = null }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Dialogue de confirmation de suppression
    if (treatmentToDelete != null) {
        val target = treatmentToDelete!!
        AlertDialog(
            onDismissRequest = {
                treatmentToDelete = null
                deleteHistoryChecked = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Supprimer le traitement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Êtes-vous sûr de vouloir supprimer définitivement le traitement « ${target.medication.name} » ? Tous ses rappels futurs seront annulés.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = deleteHistoryChecked,
                            onCheckedChange = { deleteHistoryChecked = it },
                            modifier = Modifier.testTag("delete_history_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Supprimer également l'historique des prises passées de ce médicament",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMedication(target, deleteHistoryChecked)
                        treatmentToDelete = null
                        deleteHistoryChecked = false
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        treatmentToDelete = null
                        deleteHistoryChecked = false
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // Filtrage des traitements
    val filteredTreatments = remember(treatments, activeFilter, today) {
        treatments.filter { item ->
            val start = Instant.ofEpochMilli(item.medication.startDate).atZone(zone).toLocalDate()
            val end = item.medication.endDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            val isPaused = item.medication.isPaused

            val status = when {
                isPaused -> TreatmentStatus.PAUSED
                today.isBefore(start) -> TreatmentStatus.UPCOMING
                end != null && today.isAfter(end) -> TreatmentStatus.FINISHED
                else -> TreatmentStatus.ACTIVE
            }

            when (activeFilter) {
                TreatmentFilter.ALL -> true
                TreatmentFilter.ACTIVE -> status == TreatmentStatus.ACTIVE
                TreatmentFilter.UPCOMING -> status == TreatmentStatus.UPCOMING
                TreatmentFilter.PAUSED -> status == TreatmentStatus.PAUSED
                TreatmentFilter.FINISHED -> status == TreatmentStatus.FINISHED
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("medications_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Titre d'écran
            Text(
                text = "Mes traitements",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Gérez vos médicaments, périodes et horaires quotidiens.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Onglets / Chips de filtre
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TreatmentFilter.values()) { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { viewModel.setTreatmentFilter(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Liste des traitements
            if (filteredTreatments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (treatments.isEmpty()) "Aucun traitement enregistré" else "Aucun traitement dans cette catégorie",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (treatments.isEmpty())
                                "Ajoutez votre premier traitement pour programmer vos prises et recevoir vos rappels."
                            else
                                "Sélectionnez un autre filtre pour voir vos autres traitements.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredTreatments, key = { it.medication.id }) { item ->
                        val med = item.medication
                        val start = Instant.ofEpochMilli(med.startDate).atZone(zone).toLocalDate()
                        val end = med.endDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                        val daysCount = end?.let { ChronoUnit.DAYS.between(start, it) + 1 }

                        val status = when {
                            med.isPaused -> TreatmentStatus.PAUSED
                            today.isBefore(start) -> TreatmentStatus.UPCOMING
                            end != null && today.isAfter(end) -> TreatmentStatus.FINISHED
                            else -> TreatmentStatus.ACTIVE
                        }

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { treatmentToViewDetails = item }
                                .testTag("treatment_card_${med.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Ligne d'en-tête du traitement
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = med.name,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (med.dosage.isNotBlank() || med.form.isNotBlank()) {
                                            val desc = buildString {
                                                if (med.dosage.isNotBlank()) append(med.dosage)
                                                if (med.form.isNotBlank()) {
                                                    if (isNotEmpty()) append(" • ")
                                                    append(med.form)
                                                }
                                            }
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Badge de statut
                                    val (badgeBg, badgeText, badgeLabel) = when (status) {
                                        TreatmentStatus.ACTIVE -> Triple(StatusTakenBg, StatusTakenGreen, "En cours")
                                        TreatmentStatus.UPCOMING -> Triple(StatusUpcomingBg, StatusUpcomingBlue, "À venir")
                                        TreatmentStatus.PAUSED -> Triple(StatusOverdueBg, StatusOverdueAmber, "Suspendu")
                                        TreatmentStatus.FINISHED -> Triple(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                            "Terminé"
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = badgeBg
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = badgeText,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quantité et instructions si disponibles
                                if (med.quantityPerDose.isNotBlank() || med.instructions.isNotBlank()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(10.dp)
                                    ) {
                                        if (med.quantityPerDose.isNotBlank()) {
                                            Text(
                                                text = "Quantité : ${med.quantityPerDose}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (med.instructions.isNotBlank()) {
                                            Text(
                                                text = "Instructions : ${med.instructions}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Période de traitement (avec calcul en jours)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (end != null) {
                                            "Du ${start.format(dateFormatter)} au ${end.format(dateFormatter)} ($daysCount jours)"
                                        } else {
                                            "À partir du ${start.format(dateFormatter)} (Traitement continu)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Horaires quotidiens
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Horaires quotidiens : ",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item.sortedSchedules().forEach { schedule ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = schedule.formattedTime(),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Barre d'actions : Suspendre / Détails / Modifier / Supprimer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Switch suspension des rappels
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    ) {
                                        Switch(
                                            checked = !med.isPaused,
                                            onCheckedChange = { isEnabled ->
                                                viewModel.toggleMedicationPaused(med.id, !isEnabled)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.testTag("pause_toggle_${med.id}")
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (med.isPaused) "Rappels suspendus" else "Rappels actifs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (med.isPaused) StatusOverdueAmber else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { treatmentToViewDetails = item },
                                            modifier = Modifier.testTag("view_details_${med.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Consulter les détails",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.openEditMedication(item) },
                                            modifier = Modifier.testTag("edit_treatment_${med.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Modifier",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = { treatmentToDelete = item },
                                            modifier = Modifier.testTag("delete_treatment_${med.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Supprimer",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }

        // FAB d'ajout
        ExtendedFloatingActionButton(
            onClick = { viewModel.openAddMedication() },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Ajouter un médicament", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_treatment_fab")
        )
    }
}
