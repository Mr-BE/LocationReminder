package com.udacity.project4.locationreminders.reminderslist

import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: KoinTest {

    private lateinit var database: RemindersDatabase
    private lateinit var reminderDataSource: ReminderDataSource
    private val fakeDataSource: FakeDataSource by inject()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // The Rule will make sure to launch the MainActivity directly
    @get:Rule
    val activityRule = ActivityTestRule(RemindersActivity::class.java)

    @get:Rule
    var permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @Before
    fun setup() {
        stopKoin()
        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(testModules))
        }
    }

    @Test
    fun givenReminderList_CheckDisplayedData() = runBlockingTest {

        fakeDataSource.deleteAllReminders()

        // GIVEN reminder
        val reminder1 = ReminderDTO("Reminder 1", "Description1",
            "Eme Inn", 4.986247, 8.349578)
        val reminder2 = ReminderDTO("Reminder 2", "Description2",
            "Multichoice", 4.989161, 8.344403 )
        fakeDataSource.saveReminder(reminder1)
        fakeDataSource.saveReminder(reminder2)

        // WHEN - ReminderListFragment launched to display reminders
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - list with reminders is displayed on the screen
        onView(ViewMatchers.withText("Reminder 1")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText("Reminder 2")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun givenEmptyReminderList_ConfirmNoDataDisplayed() = runBlockingTest {
        //GIVEN - no data
        fakeDataSource.deleteAllReminders()

        // WHEN - ReminderListFragment launched to display reminders
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - No data message is displayed on the screen
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun givenRemindersScreen_WhenClicked_GoToLocationReminder() {
        // GIVEN - reminders screen
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - Add button is clicked
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - navigate to Reminder detail screen
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun givenMessage_DisplayErrorMessage() {

        //GIVEN - error message
        val errorMessage = "No internet connection"

        // WHEN - reminders screen
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment {
            it.showErrorMessage(errorMessage)
        }

        // THEN - error message is displayed
        onView(ViewMatchers.withText(errorMessage)).
        inRoot(RootMatchers.withDecorView(Matchers.not(activityRule.activity.window.decorView))).
        check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun givenMessage_DisplayToastMessage() {
        //GIVEN - message
        val message = "No internet connection"

        // WHEN - reminders screen
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment {
            it.showToastMessage(message)
        }

        // THEN - toast message is displayed
        onView(ViewMatchers.withText(message)).
        inRoot(RootMatchers.withDecorView(Matchers.not(activityRule.activity.window.decorView))).
        check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun givenMessage_DisplaySnackBar() {

        //GIVEN - message
        val message = "No internet connection"

        // WHEN - reminders screen
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment {
            it.showSnackBar(message)
        }

        // THEN - snackbar message is displayed
        onView(ViewMatchers.withText(message)).
        inRoot(RootMatchers.withDecorView(Matchers.not(activityRule.activity.window.decorView))).
        check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private val testModules = module {
        viewModel {
            RemindersListViewModel(
                get(),
                get() as FakeDataSource
            )
        }

        single {
            database = Room.inMemoryDatabaseBuilder(
                getApplicationContext(),
                RemindersDatabase::class.java).allowMainThreadQueries().build()
        }

        single {
            RemindersLocalRepository(get()) as RemindersLocalRepository
        }

        /*  single {
              database.reminderDao() as ReminderDao
          }*/

        single {
            FakeDataSource()
        }
    }
}