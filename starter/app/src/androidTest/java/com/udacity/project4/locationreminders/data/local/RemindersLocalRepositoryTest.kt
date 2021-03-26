package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.jvm.Throws

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

//     Add testing implementation to the RemindersLocalRepository.kt

    private lateinit var database : RemindersDatabase
    private lateinit var reminderDao : RemindersDao
    private lateinit var fakeRemindersLocalRepository: RemindersLocalRepository
    private var shouldReturnError = false

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // In-memory version of the database
        database = Room.inMemoryDatabaseBuilder(context,
            RemindersDatabase::class.java).allowMainThreadQueries().build()

        reminderDao = database.reminderDao()
        fakeRemindersLocalRepository = RemindersLocalRepository(reminderDao, Dispatchers.Main)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun givenReminders_WhenGettingList_ReturnSuccess() = runBlocking {
        val reminder1 = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)
        val reminder2 = ReminderDTO("Reminder 2", "Description2",
            "Multichoice", 4.989161, 8.344403 )

        reminderDao.saveReminder(reminder1)
        reminderDao.saveReminder(reminder2)

        val remindersList =
            fakeRemindersLocalRepository.getReminders() as Result.Success
        ViewMatchers.assertThat(remindersList.data.size, Matchers.`is`(2))
        ViewMatchers.assertThat(remindersList.data, Matchers.notNullValue())
    }

    @Test
    fun givenReminders_WhenGettingList_ReturnEmpty() = runBlocking {
        // GIVEN no data
        reminderDao.deleteAllReminders()

        // WHEN getting reminders
        val remindersList =
            fakeRemindersLocalRepository.getReminders() as Result.Success

        // THEN return error
        ViewMatchers.assertThat(remindersList.data.size, Matchers.`is`(0))
    }

    @Test
    fun givenReminders_WhenSaving_ReturnSuccess() = runBlocking {
        val reminder = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)

        fakeRemindersLocalRepository.saveReminder(reminder)
        val result =
            fakeRemindersLocalRepository.getReminder(reminder.id) as Result.Success

        ViewMatchers.assertThat(result.data.id, `is`(reminder.id))
        ViewMatchers.assertThat(result.data.title, `is`(reminder.title))
        ViewMatchers.assertThat(result.data.description, `is`(reminder.description))
        ViewMatchers.assertThat(result.data.location, `is`(reminder.location))
        ViewMatchers.assertThat(result.data.latitude, `is`(reminder.latitude))
        ViewMatchers.assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun givenReminders_WhenSaving_ReturnError() = runBlocking {
        val reminder = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)

        fakeRemindersLocalRepository.saveReminder(reminder)
        val result =
            fakeRemindersLocalRepository.getReminder("2") as Result.Error

        ViewMatchers.assertThat(result.message, Matchers.`is`("Reminder not found!"))
    }

    @Test
    fun givenReminders_WhenGettingReminder_ReturnSuccess() = runBlocking {
        val reminder1 = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)
        val reminder2 = ReminderDTO("Reminder 2", "Description2",
            "Multichoice", 4.989161, 8.344403 )

        reminderDao.saveReminder(reminder1)
        reminderDao.saveReminder(reminder2)

        val result =
            fakeRemindersLocalRepository.getReminder(reminder1.id) as Result.Success

        ViewMatchers.assertThat(result.data.id, `is`(reminder1.id))
        ViewMatchers.assertThat(result.data.title, `is`(reminder1.title))
        ViewMatchers.assertThat(result.data.description, `is`(reminder1.description))
        ViewMatchers.assertThat(result.data.location, `is`(reminder1.location))
        ViewMatchers.assertThat(result.data.latitude, `is`(reminder1.latitude))
        ViewMatchers.assertThat(result.data.longitude, `is`(reminder1.longitude))
    }

    @Test
    fun givenReminders_WhenGettingReminder_ReturnError() = runBlocking {
        val reminder = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)


        reminderDao.saveReminder(reminder)

        val result =
            fakeRemindersLocalRepository.getReminder("2") as Result.Error
        ViewMatchers.assertThat(result.message, Matchers.`is`("Reminder not found!"))
    }

    @Test
    fun givenReminders_WhenDeletingAll_ReturnSuccess() = runBlocking {
        val reminder1 = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)
        val reminder2 = ReminderDTO("Reminder 2", "Description2",
            "Multichoice", 4.989161, 8.344403 )

        reminderDao.saveReminder(reminder1)
        reminderDao.saveReminder(reminder2)

        val resultAfterSaved =
            fakeRemindersLocalRepository.getReminders() as Result.Success

        ViewMatchers.assertThat(resultAfterSaved.data.size, Matchers.`is`(2))

        fakeRemindersLocalRepository.deleteAllReminders()

        val resultAfterDeleted =
            fakeRemindersLocalRepository.getReminders() as Result.Success

        ViewMatchers.assertThat(resultAfterDeleted.data.size, Matchers.`is`(0))
    }

}