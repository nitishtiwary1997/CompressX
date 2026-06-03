package com.hire.smartcompress.di

import android.content.Context
import androidx.room.Room
import com.hire.smartcompress.data.database.AppDatabase
import com.hire.smartcompress.data.database.dao.CompressionHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideCompressionHistoryDao(db: AppDatabase): CompressionHistoryDao =
        db.compressionHistoryDao()
}
