package com.example.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

data class PasswordStrength(
    val score: Int,
    val label: String,
    val meetsMinimum: Boolean,
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigitOrSymbol: Boolean,
)

object AuthValidation {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val NG_LOCAL_PHONE = Regex("^0[789][01]\\d{8}$")
    private val NG_INTL_PHONE = Regex("^\\+?234[789][01]\\d{8}$")
    private val E164_PHONE = Regex("^\\+[1-9]\\d{6,14}$")

    val NIGERIAN_STATES: List<String> = listOf(
        "Lagos",
        "Abuja (FCT)",
        "Abia",
        "Adamawa",
        "Akwa Ibom",
        "Anambra",
        "Bauchi",
        "Bayelsa",
        "Benue",
        "Borno",
        "Cross River",
        "Delta",
        "Ebonyi",
        "Edo",
        "Ekiti",
        "Enugu",
        "Gombe",
        "Imo",
        "Jigawa",
        "Kaduna",
        "Kano",
        "Katsina",
        "Kebbi",
        "Kogi",
        "Kwara",
        "Nasarawa",
        "Niger",
        "Ogun",
        "Ondo",
        "Osun",
        "Oyo",
        "Plateau",
        "Rivers",
        "Sokoto",
        "Taraba",
        "Yobe",
        "Zamfara",
    )

    val SUPPORTED_COUNTRIES: List<String> = listOf(
        "Nigeria",
        "United Kingdom",
        "United States",
        "Canada",
        "Ghana",
        "South Africa",
        "United Arab Emirates",
        "Other International",
    )

    fun validateName(raw: String, fieldName: String): String? {
        val clean = raw.trim()
        return when {
            clean.isEmpty() -> "$fieldName is required."
            clean.length < 2 -> "$fieldName must be at least 2 characters."
            clean.length > 40 -> "$fieldName must be 40 characters or fewer."
            else -> null
        }
    }

    fun validateEmail(raw: String): String? {
        val clean = raw.trim()
        return when {
            clean.isEmpty() -> "Email address is required."
            clean.length > 200 -> "Email address is too long."
            !EMAIL_REGEX.matches(clean) -> "Enter a valid email address (e.g., amaka@example.com)."
            else -> null
        }
    }

    fun normalizePhone(raw: String): String {
        val stripped = raw.trim().replace(Regex("[\\s\\-()]"), "")
        if (NG_LOCAL_PHONE.matches(stripped)) {
            return "+234${stripped.drop(1)}"
        }
        if (stripped.startsWith("234") && NG_INTL_PHONE.matches("+$stripped")) {
            return "+$stripped"
        }
        return stripped
    }

    fun validatePhone(raw: String): String? {
        val normalized = normalizePhone(raw)
        if (normalized.isEmpty()) return "Phone number is required."
        if (normalized.length > 30) return "Phone number is too long."
        val isValidNg = NG_LOCAL_PHONE.matches(normalized) || NG_INTL_PHONE.matches(normalized)
        val isValidIntl = E164_PHONE.matches(normalized)
        return if (isValidNg || isValidIntl) {
            null
        } else {
            "Enter a valid Nigerian number (e.g., 08012345678 or +2348012345678) or international E.164 number (+44...)."
        }
    }

    fun evaluatePasswordStrength(password: String): PasswordStrength {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigitOrSymbol = password.any { !it.isLetterOrDigit() || it.isDigit() }
        val score = listOf(hasMinLength, hasUppercase, hasLowercase, hasDigitOrSymbol).count { it }
        val label = when {
            password.isEmpty() -> "Enter a password"
            score <= 1 -> "Weak"
            score == 2 -> "Fair"
            score == 3 -> "Good"
            else -> "Strong"
        }
        return PasswordStrength(
            score = score,
            label = label,
            meetsMinimum = hasMinLength && hasLowercase && (hasUppercase || hasDigitOrSymbol),
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigitOrSymbol = hasDigitOrSymbol,
        )
    }

    fun validatePasswordConfirmation(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Please confirm your password."
        password != confirm -> "Passwords do not match."
        else -> null
    }

    fun validateDeliveryAddress(
        label: String,
        recipientName: String,
        phone: String,
        country: String,
        state: String,
        city: String,
        streetAddress: String,
    ): String? {
        if (label.trim().isEmpty()) return "Address label is required (e.g., Home or Office)."
        validateName(recipientName, "Recipient name")?.let { return it }
        validatePhone(phone)?.let { return it }
        if (country.trim().length < 2) return "Please select a country."
        if (state.trim().isEmpty()) return "State or province is required."
        if (city.trim().isEmpty()) return "City or town is required."
        if (streetAddress.trim().length < 3) return "Please enter your full street address."
        return null
    }

    fun mapFirebaseAuthError(error: Throwable, isSignInFlow: Boolean = false): String {
        if (error is FirebaseNetworkException) {
            return "Network connection unavailable. Check your internet and try again."
        }
        if (error is FirebaseAuthUserCollisionException) {
            return "An account with this email already exists. Please sign in or reset your password."
        }
        if (error is FirebaseAuthWeakPasswordException) {
            return "Password is too weak. Use at least 8 characters with uppercase, lowercase, and numbers or symbols."
        }
        if (error is FirebaseAuthInvalidCredentialsException || error is FirebaseAuthInvalidUserException) {
            return if (isSignInFlow) {
                "Invalid email or password. Please verify your details and try again."
            } else {
                error.localizedMessage ?: "Authentication credentials are invalid or expired."
            }
        }
        if (error is FirebaseAuthException) {
            return when (error.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Email/Password sign-in is not enabled yet in Firebase Console. Sign in with Google or enable Email/Password under Authentication > Sign-in method."
                "ERROR_TOO_MANY_REQUESTS" ->
                    "Too many attempts. Please wait a minute before trying again."
                "ERROR_USER_DISABLED" ->
                    "This account has been disabled. Contact Zeecrown concierge support."
                "ERROR_REQUIRES_RECENT_LOGIN" ->
                    "For security, please re-enter your current password to continue."
                else -> error.localizedMessage ?: "Authentication error (${error.errorCode})."
            }
        }
        return error.localizedMessage ?: "Authentication failed. Please try again."
    }
}
