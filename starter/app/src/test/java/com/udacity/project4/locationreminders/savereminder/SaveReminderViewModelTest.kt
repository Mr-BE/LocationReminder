package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.*
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.IsNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

//For Roboelectric Java 9 error
@Config(sdk = [Build.VERSION_CODES.O_MR1])

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    //needed to run architecture related background jobs
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    //custom jUnit rule to set main coroutines dispatcher for testing
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()

    //test subject
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var context: Application

    //provide testing to the SaveReminderView and its live data objects

    //set up viewmodel before test
    @Before
    fun setUpViewModel() = mainCoroutineRule.runBlockingTest {
        stopKoin()
        fakeDataSource = FakeDataSource()
        context = ApplicationProvider.getApplicationContext()

        saveReminderViewModel = SaveReminderViewModel(
                ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    //Input and retrieve reminder as expected
    @Test
    fun givenReminder_whenValidateAndSave_returnSuccess() = runBlocking{

        //GIVEN -  generic reminder data
        val reminder = ReminderDataItem("Reminder 1", "Description1",
        "Eme Inn", 4.986247, 8.349578)

        //WHEN - saving reminder data
        saveReminderViewModel.validateAndSaveReminder(reminder)
        val result = fakeDataSource.getReminder(reminder.id) as Success<ReminderDTO>

        //THEN - return right data
        assertThat(result.data.title, `is`("Reminder 1"))
        //show toast onSuccess
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue() ,
                `is` (context.getString(R.string.reminder_saved)))
    }

    //Input null location details and retrieved error as expected
    @Test
    fun givenNullLocation_whenValidateAndSave_returnError(){

        //Given - Reminder with null location value
        val nullLocationReminder = ReminderDataItem("My Reminder ", "My Description",
        null, null, null,)

        //WHEN - saving null location reminder
        val result = saveReminderViewModel.validateEnteredData(nullLocationReminder)

        //THEN- show error message snackbar: select location
        assertThat(result, `is` (false))
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),`is` (R.string.err_select_location) )
    }

    //Input null reminder title and retrieve error
    @Test
    fun givenNullTitle_whenValidate_returnError() {

        //GIVEN - Reminder with null title
        val reminder = ReminderDataItem(null, "Description1",
                "Eme Inn", 4.986247, 8.349578)

        //WHEN - validating reminder
        val result = saveReminderViewModel.validateEnteredData(reminder)

        //THEN - return error (Snackbar with error message)
        assertThat(result, `is` (false))
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is` (R.string.err_enter_title))
    }
    @Test
    fun givenData_WhenSaving_CheckLoading() = mainCoroutineRule.runBlockingTest {
        //GIVEN a reminder
        val reminder = ReminderDataItem("Reminder 1", "Description1",
                "Eme Inn", 4.986247, 8.349578)

        // Pause dispatcher cos of loading status
        mainCoroutineRule.pauseDispatcher()

        // WhEN - saving reminder
        saveReminderViewModel.saveReminder(reminder)

        var showLoading = saveReminderViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading, `is`(true))

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        showLoading = saveReminderViewModel.showLoading.getOrAwaitValue()

        //TheN -
        assertThat(showLoading, `is`(false))

        assertThat(saveReminderViewModel.showToast.getOrAwaitValue() ,
                `is` (context.getString(R.string.reminder_saved)))
    }

    //Check that view model resources are cleaned up
    @Test
    fun givenLiveData_whenCleaningUp_returnNull() {
        saveReminderViewModel.onClear()

        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(IsNull.nullValue()))
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(IsNull.nullValue()))
        assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(IsNull.nullValue()))
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(IsNull.nullValue()))
        assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), `is`(IsNull.nullValue()))
        assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), `is`(IsNull.nullValue()))
    }



}