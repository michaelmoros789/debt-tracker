package com.michaelmoros.debttracker

import com.michaelmoros.debttracker.ui.toDaysAgo
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `toDaysAgo should return Today for current time`() {
        val now = System.currentTimeMillis()
        assertEquals("Today", now.toDaysAgo())
    }

    @Test
    fun `toDaysAgo should return Yesterday for 24 hours ago`() {
        val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        assertEquals("Yesterday", yesterday.toDaysAgo())
    }

    @Test
    fun `toDaysAgo should return days ago for multiple days`() {
        val fiveDaysAgo = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000)
        assertEquals("5 days ago", fiveDaysAgo.toDaysAgo())
    }
}
