package com.jacobrozell.puzzlebuddy.di

import android.content.Context
import com.jacobrozell.puzzlebuddy.BuildConfig
import com.jacobrozell.puzzlebuddy.data.local.PuzzleBuddyDatabase
import com.jacobrozell.puzzlebuddy.data.local.PuzzleBuddyDatabaseFactory
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleDao
import com.jacobrozell.puzzlebuddy.support.FirebaseBootstrap
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.DefaultAppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        logger: AppLogger,
    ): PuzzleBuddyDatabase = PuzzleBuddyDatabaseFactory.create(context, logger)

    @Provides
    fun providePuzzleDao(db: PuzzleBuddyDatabase): PuzzleDao = db.puzzleDao()

    @Provides
    @Singleton
    fun provideAppLogger(firebaseBootstrap: FirebaseBootstrap): AppLogger =
        DefaultAppLogger.create(firebaseBootstrap, BuildConfig.VERSION_NAME)
}
