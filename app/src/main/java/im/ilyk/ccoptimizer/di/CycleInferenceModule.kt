package im.ilyk.ccoptimizer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.ilyk.ccoptimizer.core.billing.CycleInferenceEngine
import im.ilyk.ccoptimizer.core.billing.CycleInferenceEngineImpl
import im.ilyk.ccoptimizer.core.billing.CycleConfig
import im.ilyk.ccoptimizer.data.db.dao.CycleDao
import im.ilyk.ccoptimizer.data.repository.CycleRepositoryImpl
import javax.inject.Singleton

/**
 * Hilt module for cycle inference dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object CycleInferenceModule {
    
    @Provides
    @Singleton
    fun provideCycleConfig(): CycleConfig {
        return CycleConfig.DEFAULT
    }
    
    @Provides
    @Singleton
    fun provideCycleInferenceEngine(config: CycleConfig): CycleInferenceEngine {
        return CycleInferenceEngineImpl(config)
    }
    
    @Provides
    @Singleton
    fun provideCycleRepository(
        cycleDao: CycleDao,
        engine: CycleInferenceEngine
    ): CycleRepositoryImpl {
        return CycleRepositoryImpl(cycleDao, engine)
    }
}
