package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.PilulierDatabase
import com.example.data.model.DoseSchedule
import com.example.data.model.DoseStatus
import com.example.data.model.Medication
import com.example.data.model.MedicationWithSchedules
import com.example.data.repository.PilulierRepository
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import com.example.receiver.BootReceiver
import com.example.receiver.NotificationActionReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: PilulierDatabase
    private lateinit var repository: PilulierRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PilulierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        PilulierDatabase.setTestInstance(database)
        repository = PilulierRepository(context, database)
    }

    @After
    fun tearDown() {
        PilulierDatabase.setTestInstance(null)
        database.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Pilulier", appName)
    }

    @Test
    fun testCreateTreatmentSingleSchedule() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(3).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(
            name = "Doliprane",
            dosage = "1000 mg",
            form = "Comprimé",
            quantityPerDose = "1 comprimé",
            instructions = "Pendant le repas",
            startDate = startMillis,
            endDate = endMillis
        )
        val schedules = listOf(DoseSchedule(medicationId = 0, hour = 8, minute = 0))

        val medId = repository.saveMedication(med, schedules)
        assertTrue(medId > 0)

        val retrieved = repository.getMedicationById(medId).first()
        assertNotNull(retrieved)
        assertEquals("Doliprane", retrieved?.medication?.name)
        assertEquals(1, retrieved?.schedules?.size)
        assertEquals(8, retrieved?.schedules?.first()?.hour)
    }

    @Test
    fun testCreateTreatmentMultipleSchedules() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(5).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(
            name = "Amoxicilline",
            dosage = "500 mg",
            form = "Gélule",
            quantityPerDose = "1 gélule",
            startDate = startMillis,
            endDate = endMillis
        )
        val schedules = listOf(
            DoseSchedule(medicationId = 0, hour = 8, minute = 0),
            DoseSchedule(medicationId = 0, hour = 14, minute = 0),
            DoseSchedule(medicationId = 0, hour = 20, minute = 0)
        )

        val medId = repository.saveMedication(med, schedules)
        val retrieved = repository.getMedicationById(medId).first()
        assertNotNull(retrieved)
        assertEquals(3, retrieved?.schedules?.size)
    }

    @Test
    fun testTreatmentSingleDay() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(
            name = "Vitamine D",
            dosage = "1 dose",
            form = "Ampoule",
            startDate = startMillis,
            endDate = endMillis
        )
        val schedules = listOf(DoseSchedule(medicationId = 0, hour = 10, minute = 0))

        val medId = repository.saveMedication(med, schedules)
        val retrieved = repository.getMedicationById(medId).first()
        assertNotNull(retrieved)

        val todayDoses = repository.getDosesForDate(today).first()
        assertEquals(1, todayDoses.size)
        assertEquals("Vitamine D", todayDoses.first().medicationName)

        val tomorrowDoses = repository.getDosesForDate(today.plusDays(1)).first()
        assertEquals(0, tomorrowDoses.size)
    }

    @Test
    fun testCreateTreatmentUnlimitedDuration() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()

        val med = Medication(
            name = "Lévothyrox",
            dosage = "75 µg",
            form = "Comprimé",
            startDate = startMillis,
            endDate = null // Traitement continu / durée illimitée
        )
        val schedules = listOf(DoseSchedule(medicationId = 0, hour = 7, minute = 30))

        val medId = repository.saveMedication(med, schedules)
        assertTrue(medId > 0)

        val retrieved = repository.getMedicationById(medId).first()
        assertNotNull(retrieved)
        assertNull(retrieved?.medication?.endDate)

        // Doit générer des prises aujourd'hui et dans le futur
        val todayDoses = repository.getDosesForDate(today).first()
        assertEquals(1, todayDoses.size)
        assertEquals("Lévothyrox", todayDoses.first().medicationName)

        val futureDoses = repository.getDosesForDate(today.plusDays(60)).first()
        assertEquals(1, futureDoses.size)
        assertEquals("Lévothyrox", futureDoses.first().medicationName)
    }

    @Test
    fun testTreatmentMultipleDays() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(6).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(
            name = "Antibiotique",
            dosage = "100 mg",
            startDate = startMillis,
            endDate = endMillis
        )
        val schedules = listOf(
            DoseSchedule(medicationId = 0, hour = 9, minute = 0),
            DoseSchedule(medicationId = 0, hour = 21, minute = 0)
        )

        val medId = repository.saveMedication(med, schedules)
        assertTrue(medId > 0)

        // Doit générer 2 prises chaque jour pendant 7 jours
        for (i in 0..6) {
            val doses = repository.getDosesForDate(today.plusDays(i.toLong())).first()
            assertEquals(2, doses.size)
        }

        // Le 8ème jour ne doit avoir aucune prise
        val outsideDoses = repository.getDosesForDate(today.plusDays(7)).first()
        assertEquals(0, outsideDoses.size)
    }

    @Test
    fun testModifySchedule() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(2).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(
            name = "Traitement Modifiable",
            startDate = startMillis,
            endDate = endMillis
        )
        val initialSchedules = listOf(DoseSchedule(medicationId = 0, hour = 8, minute = 0))

        val medId = repository.saveMedication(med, initialSchedules)
        val initial = repository.getMedicationById(medId).first()
        assertEquals(1, initial?.schedules?.size)

        // Modification : 2 horaires au lieu d'1
        val updatedMed = initial!!.medication.copy(dosage = "Nouvelle dose 200mg")
        val updatedSchedules = listOf(
            DoseSchedule(medicationId = medId, hour = 9, minute = 0),
            DoseSchedule(medicationId = medId, hour = 18, minute = 0)
        )
        repository.saveMedication(updatedMed, updatedSchedules)

        val modified = repository.getMedicationById(medId).first()
        assertEquals("Nouvelle dose 200mg", modified?.medication?.dosage)
        assertEquals(2, modified?.schedules?.size)
    }

    @Test
    fun testDeleteTreatmentPreservingHistory() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(2).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(
            name = "Traitement À Supprimer",
            startDate = startMillis,
            endDate = endMillis
        )
        val schedules = listOf(DoseSchedule(medicationId = 0, hour = 8, minute = 0))
        val medId = repository.saveMedication(med, schedules)
        val item = repository.getMedicationById(medId).first()!!

        // Simuler une prise confirmée
        val doseTime = today.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        repository.markDoseTaken(medId, doseTime, "Traitement À Supprimer", "")

        // Suppression sans détruire l'historique
        repository.deleteMedication(item, deleteHistory = false)

        val retrievedMed = repository.getMedicationById(medId).first()
        assertNull(retrievedMed)

        // L'historique pour ce jour contient toujours la prise confirmée
        val historyDoses = repository.getDosesForDate(today).first()
        assertEquals(1, historyDoses.size)
        assertEquals("Traitement À Supprimer", historyDoses.first().medicationName)
        assertEquals(DoseStatus.CONFIRMED, historyDoses.first().status)
    }

    @Test
    fun testPauseAndResumeTreatment() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(2).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(
            name = "Traitement Pause",
            startDate = startMillis,
            endDate = endMillis,
            isPaused = false
        )
        val medId = repository.saveMedication(med, listOf(DoseSchedule(medicationId = 0, hour = 8, minute = 0)))

        // Suspendre
        repository.setMedicationPaused(medId, true)
        val paused = repository.getMedicationById(medId).first()
        assertTrue(paused?.medication?.isPaused == true)

        // Réactiver
        repository.setMedicationPaused(medId, false)
        val resumed = repository.getMedicationById(medId).first()
        assertTrue(resumed?.medication?.isPaused == false)
    }

    @Test
    fun testConfirmDoseFromApp() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(name = "Aspirine", startDate = startMillis, endDate = endMillis)
        val medId = repository.saveMedication(med, listOf(DoseSchedule(medicationId = 0, hour = 12, minute = 0)))

        val doseTime = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        repository.markDoseTaken(medId, doseTime, "Aspirine", "500 mg")

        val doses = repository.getDosesForDate(today).first()
        assertEquals(1, doses.size)
        assertEquals(DoseStatus.CONFIRMED, doses.first().status)
        assertNotNull(doses.first().confirmedAt)

        // Annulation de la confirmation
        repository.unmarkDoseTaken(medId, doseTime)
        val unconfirmedDoses = repository.getDosesForDate(today).first()
        assertEquals(1, unconfirmedDoses.size)
        assertTrue(unconfirmedDoses.first().status != DoseStatus.CONFIRMED)
    }

    @Test
    fun testNotificationActionReceiverMarkTaken() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(name = "Kardegic", startDate = startMillis, endDate = endMillis)
        val medId = repository.saveMedication(med, listOf(DoseSchedule(medicationId = 0, hour = 8, minute = 0)))
        val scheduledTime = today.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()

        // Simuler la réception de l'action "Pris" depuis la notification
        val receiver = NotificationActionReceiver()
        val intent = Intent(NotificationHelper.ACTION_MARK_TAKEN).apply {
            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medId)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 123)
        }
        receiver.onReceive(context, intent)

        // Attendre l'exécution asynchrone
        var log = database.doseLogDao().getLogForDose(medId, scheduledTime)
        var attempts = 0
        while (log?.status != "CONFIRMED" && attempts < 20) {
            kotlinx.coroutines.delay(100)
            log = database.doseLogDao().getLogForDose(medId, scheduledTime)
            attempts++
        }

        assertNotNull(log)
        assertEquals("CONFIRMED", log?.status)
        assertNotNull(log?.confirmedAt)
    }

    @Test
    fun testSnoozeDose() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val med = Medication(name = "Spasfon", startDate = startMillis, endDate = endMillis)
        val medId = repository.saveMedication(med, listOf(DoseSchedule(medicationId = 0, hour = 14, minute = 0)))
        val scheduledTime = today.atTime(14, 0).atZone(zone).toInstant().toEpochMilli()

        repository.snoozeDose(medId, scheduledTime, "Spasfon", "80 mg", 10)

        val log = database.doseLogDao().getLogForDose(medId, scheduledTime)
        assertNotNull(log)
        assertTrue(log?.isSnoozed == true)
        assertNotNull(log?.snoozedUntil)
    }

    @Test
    fun testBootReceiverDoesNotCrash() {
        val bootReceiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceive(context, intent)
    }
}
