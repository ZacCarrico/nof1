package com.nof1.core.database

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks that a database created with the oldest exported schema opens with the current code. Once
 * there's a version 2, add each migration to the database builder in DatabaseModule and here, plus
 * a test per migration that checks the data survives.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = context.getDatabasePath(TEST_DATABASE),
        driver = BundledSQLiteDriver(),
        databaseClass = Nof1Database::class,
    )

    @Before
    fun deleteOldDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun oldestSchemaOpensWithTheCurrentCode() = runTest {
        helper.createDatabase(1).close()

        val db = Room.databaseBuilder<Nof1Database>(context, TEST_DATABASE)
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            // Opening the database checks its schema against the current entities.
            assertEquals(emptyList(), db.projectDao().observeAll().first())
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
