package im.ilyk.ccoptimizer

import im.ilyk.ccoptimizer.business.PaymentCalculator
import im.ilyk.ccoptimizer.data.models.BankInstitution
import im.ilyk.ccoptimizer.data.models.CreditCard
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PaymentCalculatorTest {
    
    @Test
    fun testAmexInstantProcessing() {
        val creditCard = CreditCard(
            id = "1",
            name = "Amex Platinum",
            lastFourDigits = "1234",
            bankName = "American Express",
            statementGenerationDay = 18,
            currentBalance = 1500.00,
            availableCredit = 8500.00,
            creditLimit = 10000.00,
            institution = BankInstitution.AMEX
        )
        
        val recommendation = PaymentCalculator.calculateRecommendation(creditCard)
        
        // Recommended amount should be the current balance
        assertEquals(1500.00, recommendation.recommendedAmount, 0.01)
        
        // Institution should be AMEX with 0 processing days
        assertEquals(BankInstitution.AMEX, recommendation.creditCard.institution)
        assertEquals(0, creditCard.institution.processingDays)
    }
    
    @Test
    fun testBankOfAmericaProcessingTime() {
        val creditCard = CreditCard(
            id = "2",
            name = "BofA Cash Rewards",
            lastFourDigits = "5678",
            bankName = "Bank of America",
            statementGenerationDay = 15,
            currentBalance = 2000.00,
            availableCredit = 3000.00,
            creditLimit = 5000.00,
            institution = BankInstitution.BANK_OF_AMERICA
        )
        
        val recommendation = PaymentCalculator.calculateRecommendation(creditCard)
        
        // BoA takes 3 days to process
        assertEquals(3, creditCard.institution.processingDays)
        
        // Recommended amount should be current balance
        assertEquals(2000.00, recommendation.recommendedAmount, 0.01)
    }
    
    @Test
    fun testBankInstitutionMapping() {
        assertEquals(BankInstitution.AMEX, BankInstitution.fromBankName("American Express"))
        assertEquals(BankInstitution.AMEX, BankInstitution.fromBankName("Amex Platinum"))
        assertEquals(BankInstitution.BANK_OF_AMERICA, BankInstitution.fromBankName("Bank of America"))
        assertEquals(BankInstitution.CHASE, BankInstitution.fromBankName("Chase Sapphire"))
        assertEquals(BankInstitution.CITI, BankInstitution.fromBankName("Citi Double Cash"))
        assertEquals(BankInstitution.HOME_DEPOT, BankInstitution.fromBankName("Home Depot"))
        assertEquals(BankInstitution.HOME_DEPOT, BankInstitution.fromBankName("HomeDepot Credit Card"))
        assertEquals(BankInstitution.CAPITAL_ONE, BankInstitution.fromBankName("Capital One Venture"))
        assertEquals(BankInstitution.DISCOVER, BankInstitution.fromBankName("Discover It"))
        assertEquals(BankInstitution.WELLS_FARGO, BankInstitution.fromBankName("Wells Fargo"))
        assertEquals(BankInstitution.US_BANK, BankInstitution.fromBankName("US Bank"))
        assertEquals(BankInstitution.GENERIC, BankInstitution.fromBankName("Unknown Bank"))
    }
    
    @Test
    fun testHomeDepotProcessingTime() {
        val creditCard = CreditCard(
            id = "3",
            name = "Home Depot Consumer Credit Card",
            lastFourDigits = "9999",
            bankName = "Home Depot",
            statementGenerationDay = 20,
            currentBalance = 850.00,
            availableCredit = 4150.00,
            creditLimit = 5000.00,
            institution = BankInstitution.HOME_DEPOT
        )
        
        val recommendation = PaymentCalculator.calculateRecommendation(creditCard)
        
        // Home Depot (issued by Citi) takes 2 days to process
        assertEquals(2, creditCard.institution.processingDays)
        
        // Recommended amount should be current balance
        assertEquals(850.00, recommendation.recommendedAmount, 0.01)
        
        // Display name should indicate it's issued by Citi
        assertEquals("Home Depot (Citi)", creditCard.institution.displayName)
    }
    
    @Test
    fun testMultipleCardsRecommendations() {
        val cards = listOf(
            CreditCard(
                id = "1",
                name = "Amex",
                lastFourDigits = "1234",
                bankName = "American Express",
                statementGenerationDay = 15,
                currentBalance = 1000.00,
                availableCredit = 9000.00,
                creditLimit = 10000.00,
                institution = BankInstitution.AMEX
            ),
            CreditCard(
                id = "2",
                name = "Chase",
                lastFourDigits = "5678",
                bankName = "Chase",
                statementGenerationDay = 20,
                currentBalance = 500.00,
                availableCredit = 4500.00,
                creditLimit = 5000.00,
                institution = BankInstitution.CHASE
            )
        )
        
        val recommendations = PaymentCalculator.calculateRecommendations(cards)
        
        // Should have recommendations for both cards
        assertEquals(2, recommendations.size)
        
        // Recommendations should be sorted by payment date
        assertTrue(
            recommendations[0].recommendedPaymentDate <= recommendations[1].recommendedPaymentDate
        )
    }
    
    @Test
    fun testRecommendationReasoning() {
        val creditCard = CreditCard(
            id = "1",
            name = "Chase Card",
            lastFourDigits = "1234",
            bankName = "Chase",
            statementGenerationDay = 18,
            currentBalance = 750.00,
            availableCredit = 4250.00,
            creditLimit = 5000.00,
            institution = BankInstitution.CHASE
        )
        
        val recommendation = PaymentCalculator.calculateRecommendation(creditCard)
        
        // Reasoning should contain key information
        assertTrue(recommendation.reasoning.contains("Statement closes"))
        assertTrue(recommendation.reasoning.contains("Chase"))
        assertTrue(recommendation.reasoning.contains("business day"))
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testNullStatementDayThrowsException() {
        val creditCard = CreditCard(
            id = "1",
            name = "Unknown Card",
            lastFourDigits = "9999",
            bankName = "Unknown Bank",
            statementGenerationDay = null, // Statement day unknown - needs user input
            currentBalance = 1000.00,
            availableCredit = 4000.00,
            creditLimit = 5000.00,
            institution = BankInstitution.GENERIC,
            needsStatementDateInput = true
        )
        
        // This should throw IllegalArgumentException
        PaymentCalculator.calculateRecommendation(creditCard)
    }
    
    @Test
    fun testGetCardsNeedingInput() {
        val cards = listOf(
            CreditCard(
                id = "1",
                name = "Chase",
                lastFourDigits = "1234",
                bankName = "Chase",
                statementGenerationDay = 18,
                currentBalance = 1000.00,
                availableCredit = 9000.00,
                creditLimit = 10000.00,
                institution = BankInstitution.CHASE
            ),
            CreditCard(
                id = "2",
                name = "Unknown Card",
                lastFourDigits = "5678",
                bankName = "Unknown",
                statementGenerationDay = null,
                currentBalance = 500.00,
                availableCredit = 4500.00,
                creditLimit = 5000.00,
                institution = BankInstitution.GENERIC,
                needsStatementDateInput = true
            )
        )
        
        val needingInput = PaymentCalculator.getCardsNeedingInput(cards)
        
        assertEquals(1, needingInput.size)
        assertEquals("Unknown Card", needingInput[0].name)
        assertTrue(needingInput[0].needsStatementDateInput)
    }
    
    @Test
    fun testMonthEndEdgeCase_February() {
        // Statement day 31, but February only has 28 days
        val creditCard = CreditCard(
            id = "1",
            name = "Test Card",
            lastFourDigits = "1234",
            bankName = "Test Bank",
            statementGenerationDay = 31,
            currentBalance = 1000.00,
            availableCredit = 9000.00,
            creditLimit = 10000.00,
            institution = BankInstitution.GENERIC
        )
        
        // Should not crash, should use Feb 28/29 instead
        val recommendation = PaymentCalculator.calculateRecommendation(creditCard)
        
        // Should successfully create recommendation
        assertNotNull(recommendation)
        assertEquals(creditCard.id, recommendation.creditCard.id)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testInvalidStatementDay_TooLow() {
        val creditCard = CreditCard(
            id = "1",
            name = "Test Card",
            lastFourDigits = "1234",
            bankName = "Test Bank",
            statementGenerationDay = 0, // Invalid: too low
            currentBalance = 1000.00,
            availableCredit = 9000.00,
            creditLimit = 10000.00,
            institution = BankInstitution.GENERIC
        )
        
        // Should throw IllegalArgumentException
        PaymentCalculator.calculateRecommendation(creditCard)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testInvalidStatementDay_TooHigh() {
        val creditCard = CreditCard(
            id = "1",
            name = "Test Card",
            lastFourDigits = "1234",
            bankName = "Test Bank",
            statementGenerationDay = 32, // Invalid: too high
            currentBalance = 1000.00,
            availableCredit = 9000.00,
            creditLimit = 10000.00,
            institution = BankInstitution.GENERIC
        )
        
        // Should throw IllegalArgumentException
        PaymentCalculator.calculateRecommendation(creditCard)
    }
}

