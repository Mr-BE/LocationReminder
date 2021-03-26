package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class RemindersListViewModelTest {
    //needed to run architecture related background jobs
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    //custom jUnit rule to set main coroutines dispatcher for testing
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()

    //test subject
    private lateinit var remindersListViewModel: RemindersListViewModel

    //vars
    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var context: Application

    //set up viewModel
    @Before
    fun setUpViewModel() = mainCoroutineRule.runBlockingTest {
        stopKoin()

        fakeDataSource = FakeDataSource()
        context = ApplicationProvider.getApplicationContext()
        remindersListViewModel = RemindersListViewModel(context, fakeDataSource)
    }

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    @Test
    fun givenReminder_whenRetrieving_returnNotNull() = mainCoroutineRule.runBlockingTest {
        //GIVEN - a reminder
        val reminder = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)

        //WHEN -
        fakeDataSource.saveReminder(reminder)

//        THEN -
        val result = remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(result, (not(nullValue())))
        MatcherAssert.assertThat(
            remindersListViewModel.remindersList.value?.size,
            equalTo(1)
        )
    }

    @Test
    fun givenNoReminderData_whenLoading_returnNull() = mainCoroutineRule.runBlockingTest {
        fakeDataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()

        MatcherAssert.assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue().isNotEmpty(),
            `is`(false)
        )
    }

    @Test
    fun givenReminderData_whenLoading_checkLoading() = mainCoroutineRule.runBlockingTest {
        //GIVEN- reminder
        val reminder = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)

        fakeDataSource.saveReminder(reminder)

        // Pause dispatcher so we can see the loading status
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()

        var showLoading = remindersListViewModel.showLoading.getOrAwaitValue()
        MatcherAssert.assertThat(showLoading, `is`(true))

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        showLoading = remindersListViewModel.showLoading.getOrAwaitValue()
        MatcherAssert.assertThat(showLoading, `is`(false))
    }

    @Test
    fun forceError_whenLoadingReminder_ShowError() = mainCoroutineRule.runBlockingTest {
        //force fail
        fakeDataSource.setForceError(true)

        remindersListViewModel.loadReminders()

        MatcherAssert.assertThat(
            remindersListViewModel.showSnackBar.getOrAwaitValue(),
            `is`("No reminders found")
        )
    }

}