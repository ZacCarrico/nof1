package com.nof1.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Duration
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** The dispatcher for CPU-heavy work, so tests can replace it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object CoreModule {
    /**
     * The current time, to the millisecond: the database stores timestamps in milliseconds, so
     * anything stamped with this clock reads back exactly as it was saved.
     */
    @Provides
    fun providesClock(): Clock = Clock.tick(Clock.systemUTC(), Duration.ofMillis(1))

    @Provides
    @DefaultDispatcher
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
