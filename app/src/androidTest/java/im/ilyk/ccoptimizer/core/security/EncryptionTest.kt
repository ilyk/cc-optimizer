package im.ilyk.ccoptimizer.core.security

import android.content.Context
import android.content.SharedPreferences
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
import java.io.File
import javax.inject.Inject

/**
 * Instrumented tests for encryption (Room database and EncryptedSharedPreferences).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EncryptionTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    @Inject
    @UserPreferences
    lateinit var userPrefs: SharedPreferences

    @Inject
    @SecurityPreferences
    lateinit var securityPrefs: SharedPreferences

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        hiltRule.inject()
    }

    @Test
    fun testDatabaseEncryptionRoundtrip() = runBlocking {
        // Given: A card entity with sensitive data
        val card = CardEntity(
            cardId = "test_encrypted_card",
            issuer = "Secure Bank",
            last4 = "9999",
            displayName = "Encrypted Test Card",
            openedAt = null,
            isActive = true
        )

        // When: Insert into encrypted database
        database.cardDao().upsert(card)

        // Then: Can retrieve the same data
        val retrieved = database.cardDao().get("test_encrypted_card")
        assertNotNull(retrieved)
        assertEquals(card.displayName, retrieved?.displayName)
        assertEquals(card.last4, retrieved?.last4)
        assertEquals(card.issuer, retrieved?.issuer)

        // Clean up
        database.cardDao().delete(card)
    }

    @Test
    fun testDatabaseFileIsEncrypted() {
        // Given: Database exists
        val dbPath = context.getDatabasePath("credit_card_optimizer.db")
        assertTrue("Database file should exist", dbPath.exists())

        // When: Read raw bytes from database file
        val bytes = dbPath.readBytes()

        // Then: Should NOT contain plaintext "Secure Bank" or "Test Card"
        val content = String(bytes)
        assertFalse(
            "Database file should not contain plaintext data",
            content.contains("Secure Bank") || content.contains("Test Card")
        )
    }

    @Test
    fun testEncryptedPreferencesRoundtrip() {
        // Given: Sensitive data to store
        val testToken = "test_jwt_token_abc123"
        val testKey = "test_secure_key"

        // When: Store in encrypted preferences
        securityPrefs.edit()
            .putString(testKey, testToken)
            .apply()

        // Then: Can retrieve the same value
        val retrieved = securityPrefs.getString(testKey, null)
        assertEquals(testToken, retrieved)

        // Clean up
        securityPrefs.edit().remove(testKey).apply()
    }

    @Test
    fun testEncryptedPreferencesFileIsEncrypted() {
        // Given: Store some data
        securityPrefs.edit()
            .putString("auth_token", "super_secret_token_xyz")
            .apply()

        // When: Read the preferences file directly
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val prefsFile = File(prefsDir, "secure_token_prefs.xml")

        if (prefsFile.exists()) {
            val content = prefsFile.readText()

            // Then: Should NOT contain plaintext token
            assertFalse(
                "Preferences file should not contain plaintext secrets",
                content.contains("super_secret_token_xyz")
            )
        }

        // Clean up
        securityPrefs.edit().clear().apply()
    }

    @Test
    fun testUserPreferencesRoundtrip() {
        // Given: User settings
        val testValue = "dark_mode"

        // When: Store in encrypted user preferences
        userPrefs.edit()
            .putString("theme", testValue)
            .apply()

        // Then: Can retrieve the same value
        val retrieved = userPrefs.getString("theme", null)
        assertEquals(testValue, retrieved)

        // Clean up
        userPrefs.edit().remove("theme").apply()
    }
}

