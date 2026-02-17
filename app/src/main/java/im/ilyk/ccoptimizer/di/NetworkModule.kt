package im.ilyk.ccoptimizer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.ilyk.ccoptimizer.data.api.PlaidApi
import im.ilyk.ccoptimizer.data.api.RetrofitClient
import javax.inject.Singleton

/**
 * Hilt module for network dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providePlaidApi(): PlaidApi {
        return RetrofitClient.plaidApi
    }
}
