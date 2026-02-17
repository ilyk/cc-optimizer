package im.ilyk.ccoptimizer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import im.ilyk.ccoptimizer.data.db.AppDatabase
import im.ilyk.ccoptimizer.data.db.dao.BalanceDao
import im.ilyk.ccoptimizer.data.db.dao.CardDao
import im.ilyk.ccoptimizer.data.db.dao.CycleDao
import im.ilyk.ccoptimizer.data.db.dao.PaymentDao
import im.ilyk.ccoptimizer.data.local.CacheManager
import im.ilyk.ccoptimizer.data.repository.PaymentRepository
import im.ilyk.ccoptimizer.data.sync.SyncProvider
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    fun provideCardDao(database: AppDatabase): CardDao {
        return database.cardDao()
    }
    
    @Provides
    fun provideBalanceDao(database: AppDatabase): BalanceDao {
        return database.balanceDao()
    }
    
    @Provides
    fun provideCycleDao(database: AppDatabase): CycleDao {
        return database.cycleDao()
    }
    
    @Provides
    fun providePaymentDao(database: AppDatabase): PaymentDao {
        return database.paymentDao()
    }
    
    @Provides
    @Singleton
    fun providePaymentRepository(
        paymentDao: PaymentDao,
        syncProvider: SyncProvider
    ): PaymentRepository {
        return PaymentRepository(paymentDao, syncProvider)
    }
    
    @Provides
    @Singleton
    fun provideCacheManager(@ApplicationContext context: Context): CacheManager {
        return CacheManager.getInstance(context)
    }
}
