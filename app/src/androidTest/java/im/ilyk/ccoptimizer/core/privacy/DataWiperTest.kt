package im.ilyk.ccoptimizer.core.privacy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.*
import im.ilyk.ccoptimizer.data.db.AppDatabase
import im.ilyk.ccoptimizer.data.db.entities.CardEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Instrumented tests for DataWiper to ensure complete data deletion.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DataWiperTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dataWiper: DataWiper

    @Inject
    lateinit var database: AppDatabase

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        hiltRule.inject()
    }

    @Test
    fun testDataWipeDeletesAllCards() = runBlocking {
        // Given: Add test cards
        val card1 = CardEntity(
            cardId = "test_card_1",
            issuer = "Test Bank",
            last4 = "1234",
            displayName = "Test Card 1",
            openedAt = null,
            isActive = true
        )
        val card2 = CardEntity(
            cardId = "test_card_2",
            issuer = "Test Bank 2",
            last4 = "5678",
            displayName = "Test Card 2",
            openedAt = null,
            isActive = true
        )
        database.cardDao().upsert(card1)
        database.cardDao().upsert(card2)

        // Verify cards are inserted
        val cardsBefore = database.cardDao().getAllActive()
        assertEquals(2, cardsBefore.count())

        // When: Wipe all data
        val result = dataWiper.wipeAll()

        // Then: All cards should be deleted
        assertTrue(result.isSuccess)
        val cardsAfter = database.cardDao().getAllCards()
        assertEquals(0, cardsAfter.count())
    }

    @Test
    fun testDataWipeVerification() = runBlocking {
        // Given: Add test data
        val card = CardEntity(
            cardId = "test_card",
            issuer = "Test Bank",
            last4 = "1234",
            displayName = "Test Card",
            openedAt = null,
            isActive = true
        )
        database.cardDao().upsert(card)

        // When: Wipe data
        dataWiper.wipeAll()

        // Then: Verification should pass
        assertTrue(dataWiper.verifyWipe())
    }

    @Test
    fun testDataWipeClearsCaches() = runBlocking {
        // Given: Some data in cache
        context.cacheDir.mkdirs()
        val testFile = context.cacheDir.resolve("test_cache_file.tmp")
        testFile.writeText("test data")
        assertTrue(testFile.exists())

        // When: Wipe all data
        val result = dataWiper.wipeAll()

        // Then: Cache should be cleared
        assertTrue(result.isSuccess)
        assertFalse(testFile.exists())
    }
}

