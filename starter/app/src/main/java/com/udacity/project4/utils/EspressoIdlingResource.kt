package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource

object EspressoIdlingResource {
    private const val RESOURCE = "GLOBAL"

    /*
   *@param countingIdlingResource keeps track of long running repository ops
   * by counting up if there's an active task, else counts down
    */
    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() = countingIdlingResource.increment()

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
    inline fun <T> wrapEspressoIdlingResource (function: () -> T): T{
        // Espresso does not work well with coroutines yet. See

        EspressoIdlingResource.increment() //set app as busy
        return try {
            function()
        }finally {
            EspressoIdlingResource.decrement()// set app as idle
        }
    }
}