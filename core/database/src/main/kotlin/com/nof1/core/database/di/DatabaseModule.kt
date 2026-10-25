package com.nof1.core.database.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nof1.core.database.Nof1Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesNof1Database(@ApplicationContext context: Context): Nof1Database =
        Room.databaseBuilder<Nof1Database>(context, Nof1Database.FILE_NAME)
            // The bundled SQLite is the same version on every device and in tests.
            .setDriver(BundledSQLiteDriver())
            .build()
}
