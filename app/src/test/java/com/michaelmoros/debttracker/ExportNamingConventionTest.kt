package com.michaelmoros.debttracker

import com.michaelmoros.debttracker.ui.settings.ExportNamingConvention
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportNamingConventionTest {

    @Test
    fun `default naming convention should be correct`() {
        val convention = ExportNamingConvention.DEFAULT
        assertEquals("DEFAULT", convention.name)
    }

    @Test
    fun `time first naming convention should be correct`() {
        val convention = ExportNamingConvention.TIME_FIRST
        assertEquals("TIME_FIRST", convention.name)
    }
}
