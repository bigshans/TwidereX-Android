package com.twidere.twiderex.di

import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.twidere.twiderex.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ApplicationComponent::class)
object AndroidModule {
    @Provides
    fun provideAccountManager(@ApplicationContext context: Context): AccountManager =
        AccountManager.get(context)

    @Provides
    fun provideSharedPreference(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("twiderex", Context.MODE_PRIVATE)

    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "twiderex-db")
            .build()
}