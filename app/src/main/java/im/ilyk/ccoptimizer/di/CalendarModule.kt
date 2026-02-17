package im.ilyk.ccoptimizer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import im.ilyk.ccoptimizer.calendar.IcsExportRepository
import im.ilyk.ccoptimizer.calendar.IcsGenerator
import im.ilyk.ccoptimizer.data.db.dao.CardDao
import im.ilyk.ccoptimizer.data.db.dao.CycleDao
import java.time.ZoneId
import javax.inject.Singleton

/**
 * Hilt module for calendar/ICS export dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object CalendarModule {
    
    @Provides
    @Singleton
    fun provideIcsGenerator(): IcsGenerator {
        return IcsGenerator()
    }
    
    @Provides
    @Singleton
    fun provideIcsExportRepository(
        @ApplicationContext context: Context,
        cardDao: CardDao,
        cycleDao: CycleDao,
        icsGenerator: IcsGenerator,
        zoneId: ZoneId
    ): IcsExportRepository {
        return IcsExportRepository(context, cardDao, cycleDao, icsGenerator, zoneId)
    }
}
