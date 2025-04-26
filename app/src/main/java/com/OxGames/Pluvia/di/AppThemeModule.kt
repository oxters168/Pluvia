package com.OxGames.Pluvia.di

import com.OxGames.Pluvia.utils.application.AppThemeImpl
import com.OxGames.Pluvia.utils.application.IAppTheme
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppThemeModule {
    @Provides
    @Singleton
    fun provideAppTheme(): IAppTheme = AppThemeImpl()
}
