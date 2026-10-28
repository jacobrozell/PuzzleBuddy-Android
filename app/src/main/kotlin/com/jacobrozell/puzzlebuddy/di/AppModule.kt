package com.jacobrozell.puzzlebuddy.di

import android.content.Context
import com.jacobrozell.puzzlebuddy.BuildConfig
import com.jacobrozell.puzzlebuddy.data.local.PuzzleBuddyDatabase
import com.jacobrozell.puzzlebuddy.data.local.PuzzleBuddyDatabaseFactory
import com.jacobrozell.puzzlebuddy.data.local.dao.FriendDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleCompletionDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzlePhotoDao
import com.jacobrozell.puzzlebuddy.data.photo.PhotoFileStore
import com.jacobrozell.puzzlebuddy.data.photo.PuzzlePhotoFileStore
import com.jacobrozell.puzzlebuddy.data.repository.RoomTransactionRunner
import com.jacobrozell.puzzlebuddy.data.repository.TransactionRunner
import com.jacobrozell.puzzlebuddy.support.FirebaseBootstrap
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsKeyValueStore
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.DefaultAppLogger
import com.jacobrozell.puzzlebuddy.support.logging.SharedPreferencesAnalyticsStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.time.Clock
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
        store: AnalyticsKeyValueStore,
    ): PuzzleBuddyDatabase = PuzzleBuddyDatabaseFactory.create(context, logger, store)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun providePuzzleDao(db: PuzzleBuddyDatabase): PuzzleDao = db.puzzleDao()

    @Provides
    fun providePuzzlePhotoDao(db: PuzzleBuddyDatabase): PuzzlePhotoDao = db.puzzlePhotoDao()

    @Provides
    fun providePuzzleCompletionDao(db: PuzzleBuddyDatabase): PuzzleCompletionDao = db.puzzleCompletionDao()

    @Provides
    fun provideFriendDao(db: PuzzleBuddyDatabase): FriendDao = db.friendDao()

    @Provides
    @Singleton
    fun provideAppLogger(firebaseBootstrap: FirebaseBootstrap): AppLogger =
        DefaultAppLogger.create(firebaseBootstrap, BuildConfig.VERSION_NAME)

    @Provides
    @Singleton
    fun provideAnalyticsKeyValueStore(
        @ApplicationContext context: Context,
    ): AnalyticsKeyValueStore = SharedPreferencesAnalyticsStore(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds
    @Singleton
    abstract fun bindTransactions(impl: RoomTransactionRunner): TransactionRunner

    @Binds
    @Singleton
    abstract fun bindPhotoStore(impl: PuzzlePhotoFileStore): PhotoFileStore
}
