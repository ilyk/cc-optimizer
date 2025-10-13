package im.ilyk.ccoptimizer.utils

/**
 * Client-side validation utilities
 * Validates data before sending to backend to provide better error messages
 */
object Validators {
    
    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
    
    /**
     * Validate password strength
     * Minimum 8 characters, at least one letter and one number
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }
    
    /**
     * Validate statement day (1-31)
     */
    fun isValidStatementDay(day: Int?): Boolean {
        return day != null && day in 1..31
    }
    
    /**
     * Validate statement day string input
     */
    fun isValidStatementDayString(dayString: String): ValidationResult {
        if (dayString.isBlank()) {
            return ValidationResult.Error("Statement day cannot be empty")
        }
        
        val day = dayString.toIntOrNull()
        if (day == null) {
            return ValidationResult.Error("Please enter a valid number")
        }
        
        if (day < 1) {
            return ValidationResult.Error("Day must be at least 1")
        }
        
        if (day > 31) {
            return ValidationResult.Error("Day cannot be greater than 31")
        }
        
        return ValidationResult.Success(day)
    }
    
    /**
     * Get password strength description
     */
    fun getPasswordStrength(password: String): PasswordStrength {
        return when {
            password.length < 8 -> PasswordStrength.WEAK
            password.length < 12 -> {
                val hasLetter = password.any { it.isLetter() }
                val hasDigit = password.any { it.isDigit() }
                val hasSpecial = password.any { !it.isLetterOrDigit() }
                
                when {
                    hasLetter && hasDigit && hasSpecial -> PasswordStrength.STRONG
                    hasLetter && hasDigit -> PasswordStrength.MEDIUM
                    else -> PasswordStrength.WEAK
                }
            }
            else -> {
                val hasUpper = password.any { it.isUpperCase() }
                val hasLower = password.any { it.isLowerCase() }
                val hasDigit = password.any { it.isDigit() }
                val hasSpecial = password.any { !it.isLetterOrDigit() }
                
                when {
                    hasUpper && hasLower && hasDigit && hasSpecial -> PasswordStrength.VERY_STRONG
                    hasLetter(password) && hasDigit -> PasswordStrength.STRONG
                    else -> PasswordStrength.MEDIUM
                }
            }
        }
    }
    
    private fun hasLetter(s: String) = s.any { it.isLetter() }
}

sealed class ValidationResult {
    data class Success(val value: Int) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

enum class PasswordStrength {
    WEAK, MEDIUM, STRONG, VERY_STRONG
}

