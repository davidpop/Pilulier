package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MedicationWithSchedules
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.TimePickerDialog
import com.example.ui.viewmodel.PilulierViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditMedicationScreen(
    viewModel: PilulierViewModel,
    editingMedication: MedicationWithSchedules?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    // Initialisation des champs
    var name by remember { mutableStateOf(editingMedication?.medication?.name ?: "") }
    var dosage by remember { mutableStateOf(editingMedication?.medication?.dosage ?: "") }
    var form by remember { mutableStateOf(editingMedication?.medication?.form ?: "") }
    var quantityPerDose by remember { mutableStateOf(editingMedication?.medication?.quantityPerDose ?: "") }
    var instructions by remember { mutableStateOf(editingMedication?.medication?.instructions ?: "") }

    var startDate by remember {
        mutableStateOf(
            editingMedication?.medication?.let {
                Instant.ofEpochMilli(it.startDate).atZone(zone).toLocalDate()
            } ?: LocalDate.now()
        )
    }

    var isContinuousTreatment by remember {
        mutableStateOf(editingMedication?.medication?.let { it.endDate == null } ?: false)
    }

    var endDate by remember {
        mutableStateOf<LocalDate?>(
            editingMedication?.medication?.endDate?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            } ?: LocalDate.now().plusDays(6) // 7 jours par défaut si limité
        )
    }

    val schedules = remember {
        mutableStateListOf<Pair<Int, Int>>().apply {
            if (editingMedication != null && editingMedication.schedules.isNotEmpty()) {
                addAll(editingMedication.schedules.map { Pair(it.hour, it.minute) }.sortedWith(compareBy({ it.first }, { it.second })))
            } else {
                add(Pair(8, 0)) // 08h00 par défaut
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingScheduleIndex by remember { mutableStateOf<Int?>(null) }

    // Dropdown pour la forme galénique
    val formOptions = listOf(
        "Comprimé", "Gélule", "Sirop", "Gouttes", "Sachet",
        "Solution buvable", "Injection", "Pommade", "Inhalateur", "Autre"
    )
    var formDropdownExpanded by remember { mutableStateOf(false) }

    // Calcul de la durée en jours
    val treatmentDays = remember(startDate, endDate, isContinuousTreatment) {
        if (isContinuousTreatment || endDate == null) {
            null
        } else if (!endDate!!.isBefore(startDate)) {
            ChronoUnit.DAYS.between(startDate, endDate!!) + 1
        } else {
            0L
        }
    }

    // DatePicker pour la date de début
    if (showStartDatePicker) {
        AppDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { selected ->
                startDate = selected
                if (!isContinuousTreatment && endDate != null && endDate!!.isBefore(startDate)) {
                    endDate = startDate
                }
                showStartDatePicker = false
            }
        )
    }

    // DatePicker pour la date de fin
    if (showEndDatePicker) {
        AppDatePickerDialog(
            initialDate = endDate ?: startDate.plusDays(6),
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { selected ->
                if (selected.isBefore(startDate)) {
                    errorMessage = "La date de fin ne peut pas être antérieure à la date de début."
                } else {
                    endDate = selected
                    errorMessage = null
                }
                showEndDatePicker = false
            }
        )
    }

    // TimePicker pour les horaires
    if (showTimePicker) {
        val initialHour = editingScheduleIndex?.let { schedules.getOrNull(it)?.first } ?: 8
        val initialMinute = editingScheduleIndex?.let { schedules.getOrNull(it)?.second } ?: 0

        TimePickerDialog(
            initialHour = initialHour,
            initialMinute = initialMinute,
            onDismiss = {
                showTimePicker = false
                editingScheduleIndex = null
            },
            onConfirm = { hour, minute ->
                val newSchedule = Pair(hour, minute)
                val idx = editingScheduleIndex
                if (idx != null && idx in schedules.indices) {
                    schedules[idx] = newSchedule
                } else {
                    if (!schedules.contains(newSchedule)) {
                        schedules.add(newSchedule)
                    }
                }
                schedules.sortWith(compareBy({ it.first }, { it.second }))
                showTimePicker = false
                editingScheduleIndex = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("add_edit_screen")
    ) {
        // En-tête avec bouton retour
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (editingMedication != null) "Modifier le traitement" else "Nouveau traitement",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Message d'erreur éventuel
        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section 1 : Informations sur le médicament
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "1. Informations sur le médicament",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Nom du médicament (obligatoire)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Nom du médicament *") },
                    placeholder = { Text("Ex : Paracétamol, Amoxicilline") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medication_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dosage / concentration
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage ou concentration (facultatif)") },
                    placeholder = { Text("Ex : 500 mg, 1000 UI, 10 mg/ml") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medication_dosage_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Forme / présentation (avec menu déroulant)
                ExposedDropdownMenuBox(
                    expanded = formDropdownExpanded,
                    onExpandedChange = { formDropdownExpanded = !formDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = form,
                        onValueChange = { form = it },
                        label = { Text("Forme ou présentation (facultatif)") },
                        placeholder = { Text("Ex : Comprimé, Gélule, Sirop") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("medication_form_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = formDropdownExpanded,
                        onDismissRequest = { formDropdownExpanded = false }
                    ) {
                        formOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    form = option
                                    formDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quantité à prendre à chaque prise
                OutlinedTextField(
                    value = quantityPerDose,
                    onValueChange = { quantityPerDose = it },
                    label = { Text("Quantité à chaque prise (facultatif)") },
                    placeholder = { Text("Ex : 1 comprimé, 2 gélules, 5 ml") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medication_quantity_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Instructions personnelles
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions personnelles (facultatif)") },
                    placeholder = { Text("Ex : À prendre pendant le repas, avec un grand verre d'eau") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medication_instructions_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 2 : Durée du traitement
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "2. Durée du traitement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date de début
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable { showStartDatePicker = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Date de début",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = startDate.format(dateFormatter),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Changer date de début",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option : Traitement continu / durée indéterminée
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Traitement continu",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Durée illimitée (pas de date de fin programmée)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isContinuousTreatment,
                        onCheckedChange = { checked ->
                            isContinuousTreatment = checked
                            if (!checked && endDate == null) {
                                endDate = startDate.plusDays(6)
                            }
                            if (checked) {
                                errorMessage = null
                            }
                        },
                        modifier = Modifier.testTag("continuous_treatment_switch")
                    )
                }

                if (!isContinuousTreatment) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Date de fin
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { showEndDatePicker = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Date de fin",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = endDate?.format(dateFormatter) ?: "Sélectionner une date",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Changer date de fin",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calcul dynamique de la durée
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isContinuousTreatment) {
                            "Durée du traitement : Traitement continu (illimité)"
                        } else if (treatmentDays != null && treatmentDays > 0) {
                            "Durée du traitement : $treatmentDays jour(s)"
                        } else {
                            "Période invalide"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 3 : Horaires des prises
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "3. Horaires des prises quotidiennes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Définissez un ou plusieurs horaires de rappel chaque jour.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Raccourcis fréquents
                Text(
                    text = "Raccourcis rapides :",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            schedules.clear()
                            schedules.add(Pair(8, 0))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("1x / jour (08h00)", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            schedules.clear()
                            schedules.addAll(listOf(Pair(8, 0), Pair(20, 0)))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Matin & Soir (08h, 20h)", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            schedules.clear()
                            schedules.addAll(listOf(Pair(8, 0), Pair(12, 30), Pair(20, 0)))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("3x / jour (08h, 12h30, 20h)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Liste des horaires configurés
                Text(
                    text = "Horaires enregistrés (${schedules.size}) :",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                schedules.forEachIndexed { index, (hour, minute) ->
                    val timeStr = "%02dh%02d".format(hour, minute)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                editingScheduleIndex = index
                                showTimePicker = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                if (schedules.size > 1) {
                                    schedules.removeAt(index)
                                } else {
                                    errorMessage = "Il faut au minimum un horaire de prise."
                                }
                            },
                            modifier = Modifier.testTag("delete_schedule_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Supprimer l'horaire",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bouton ajouter un horaire
                Button(
                    onClick = {
                        editingScheduleIndex = null
                        showTimePicker = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_schedule_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un horaire de prise")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Récapitulatif avant validation
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Récapitulatif du traitement",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))

                val medSummaryName = if (name.isNotBlank()) name else "(Nom non renseigné)"
                val schedulesSummary = schedules.joinToString(", ") { "%02dh%02d".format(it.first, it.second) }
                val periodSummary = if (isContinuousTreatment) {
                    "À partir du ${startDate.format(dateFormatter)} (Traitement continu illimité)"
                } else {
                    "du ${startDate.format(dateFormatter)} au ${endDate?.format(dateFormatter)} ($treatmentDays jours)"
                }

                Text(
                    text = "• Médicament : $medSummaryName" + if (dosage.isNotBlank()) " ($dosage)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Période : $periodSummary",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Prises quotidiennes : ${schedules.size} par jour ($schedulesSummary)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isContinuousTreatment) {
                        "• Programmation : Rappels continus quotidiens"
                    } else {
                        val totalDosesTreatment = (treatmentDays ?: 0L) * schedules.size
                        "• Total estimé : $totalDosesTreatment rappels programmés"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bouton final « Enregistrer le traitement »
        Button(
            onClick = {
                // Validation des données
                if (name.isBlank()) {
                    errorMessage = "Veuillez renseigner le nom du médicament."
                    return@Button
                }
                if (!isContinuousTreatment && endDate != null && endDate!!.isBefore(startDate)) {
                    errorMessage = "La date de fin doit être postérieure ou égale à la date de début."
                    return@Button
                }
                if (schedules.isEmpty()) {
                    errorMessage = "Veuillez ajouter au moins un horaire de prise."
                    return@Button
                }

                val finalEndDate = if (isContinuousTreatment) null else endDate

                viewModel.saveTreatment(
                    id = editingMedication?.medication?.id ?: 0L,
                    name = name,
                    dosage = dosage,
                    form = form,
                    quantityPerDose = quantityPerDose,
                    instructions = instructions,
                    startDate = startDate,
                    endDate = finalEndDate,
                    schedules = schedules.toList()
                )
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_treatment_button")
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enregistrer le traitement",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
