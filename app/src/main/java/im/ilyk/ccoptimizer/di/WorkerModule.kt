package im.ilyk.ccoptimizer.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for WorkManager dependencies
 * 
 * Note: @HiltWorker annotation on workers handles injection automatically
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    // No explicit providers needed - Hilt handles worker injection via @HiltWorker
}
