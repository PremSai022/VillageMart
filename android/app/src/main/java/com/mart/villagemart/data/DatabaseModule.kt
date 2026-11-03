package com.mart.villagemart.data

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
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
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VillageMartDatabase =
        Room.databaseBuilder(context, VillageMartDatabase::class.java, "villagemart.db")
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun providePendingSaleDao(database: VillageMartDatabase): PendingSaleDao = database.pendingSaleDao()
}
