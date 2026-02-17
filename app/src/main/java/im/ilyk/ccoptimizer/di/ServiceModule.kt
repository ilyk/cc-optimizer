package im.ilyk.ccoptimizer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import im.ilyk.ccoptimizer.business.GreedyOptimizer
import im.ilyk.ccoptimizer.business.Optimizer
import im.ilyk.ccoptimizer.data.repository.CardsRepository
import im.ilyk.ccoptimizer.data.repository.PaymentsRepository
import im.ilyk.ccoptimizer.data.services.*
import im.ilyk.ccoptimizer.data.network.BackendApiService
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Hilt module for service dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideBackendApiService(
        httpClient: OkHttpClient
    ): BackendApiService {
        return BackendApiService(httpClient)
    }
    
    @Provides
    @Singleton
    fun providePlaidService(
        backendApi: BackendApiService,
        secureStore: SecureStore
    ): PlaidService {
        return PlaidServiceImpl(backendApi, secureStore)
    }
    
    @Provides
    @Singleton
    fun providePaymentsService(): PaymentsService {
        return PaymentsServiceImpl()
    }
    
    @Provides
    @Singleton
    fun provideCashflowService(): CashflowService {
        return CashflowServiceImpl()
    }
    
    @Provides
    @Singleton
    fun provideSecureStore(@ApplicationContext context: Context): SecureStore {
        return SecureStoreImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideOptimizer(): Optimizer {
        return GreedyOptimizer()
    }
    
    @Provides
    @Singleton
    fun provideCardsRepository(
        plaidService: PlaidService,
        creditCardDao: im.ilyk.ccoptimizer.data.db.dao.CreditCardDao,
        transactionDao: im.ilyk.ccoptimizer.data.db.dao.TransactionDao
    ): CardsRepository {
        return CardsRepository(plaidService, creditCardDao, transactionDao)
    }
    
    @Provides
    @Singleton
    fun providePaymentsRepository(
        paymentsService: PaymentsService
    ): PaymentsRepository {
        return PaymentsRepository(paymentsService)
    }
}
