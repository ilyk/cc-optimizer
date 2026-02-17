package im.ilyk.ccoptimizer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import im.ilyk.ccoptimizer.data.db.dao.CardDao
import im.ilyk.ccoptimizer.data.db.dao.CycleDao
import im.ilyk.ccoptimizer.notifications.*
import java.time.ZoneId
import javax.inject.Singleton

/**
 * Hilt module for notification dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    
    @Provides
    @Singleton
    fun provideNotifPrefs(@ApplicationContext context: Context): NotifPrefs {
        return NotifPrefs(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationPlanner(
        cardDao: CardDao,
        cycleDao: CycleDao,
        notifPrefs: NotifPrefs
    ): NotificationPlanner {
        return NotificationPlannerImpl(cardDao, cycleDao, notifPrefs)
    }
    
    @Provides
    @Singleton
    fun provideNotificationScheduler(
        @ApplicationContext context: Context
    ): NotificationScheduler {
        return NotificationSchedulerImpl(context)
    }
    
    @Provides
    fun provideZoneId(): ZoneId {
        return ZoneId.systemDefault()
    }
}
