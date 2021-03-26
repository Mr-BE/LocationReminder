package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test
import java.io.IOException
import kotlin.jvm.Throws

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
//     Add testing implementation to the RemindersDao.kt

    private lateinit var reminderDao : RemindersDao
    private lateinit var database : RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(context,
            RemindersDatabase::class.java).allowMainThreadQueries().build()

        reminderDao = database.reminderDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun getReminders() = runBlockingTest {
        val reminder1 = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)

        reminderDao.saveReminder(reminder1)

        val reminderList = reminderDao.getReminders()
        assertThat(reminderList, Matchers.notNullValue())
        assertThat(reminderList.size, Matchers.`is`(1))
    }

    @Test
    fun saveReminder() = runBlockingTest {
        val reminder1 = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)

        reminderDao.saveReminder(reminder1)

        val reminderSaved = reminderDao.getReminderById(reminder1.id)
        assertThat(reminderSaved, Matchers.notNullValue())
        assertThat(reminderSaved?.id, `is`(reminderSaved?.id))
        assertThat(reminderSaved?.title, `is`(reminderSaved?.title))
        assertThat(reminderSaved?.location, `is`(reminderSaved?.location))
    }

    @Test
    fun getRemindersById() = runBlockingTest {
        val reminder1 = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)
        val reminder2 = ReminderDTO("Reminder 2", "Description2",
            "Multichoice", 4.989161, 8.344403 )

        reminderDao.saveReminder(reminder1)
        reminderDao.saveReminder(reminder2)

        val reminder = reminderDao.getReminderById(reminder2.id)
        assertThat(reminder?.id, `is`(reminder2.id))
    }

    @Test
    fun deleteAllReminders() = runBlockingTest {
        val reminder1 = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)
        val reminder2 = ReminderDTO("Reminder 2", "Description2",
            "Multichoice", 4.989161, 8.344403 )

        reminderDao.saveReminder(reminder1)
        reminderDao.saveReminder(reminder2)

        reminderDao.deleteAllReminders()
        val deletedList = reminderDao.getReminders()
        assertThat(deletedList.size, Matchers.`is`(0))

    }
}