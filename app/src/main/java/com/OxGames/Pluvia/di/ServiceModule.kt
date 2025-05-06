package com.OxGames.Pluvia.di

import android.content.Context
import com.OxGames.Pluvia.service.ServiceConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun provideServiceConnectionManager(@ApplicationContext context: Context): ServiceConnectionManager = ServiceConnectionManager(context)
}
