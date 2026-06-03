package com.hire.smartcompress.di

import com.hire.smartcompress.data.repository.CompressionRepositoryImpl
import com.hire.smartcompress.domain.repository.ICompressionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCompressionRepository(
        impl: CompressionRepositoryImpl
    ): ICompressionRepository
}
