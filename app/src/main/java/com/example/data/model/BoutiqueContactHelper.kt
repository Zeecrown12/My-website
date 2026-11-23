package com.example.data.model

import java.net.URLEncoder

object BoutiqueContactHelper {
    const val OFFICIAL_WHATSAPP_NUMBER_DISPLAY = "+234 816 989 8763"
    const val OFFICIAL_WHATSAPP_DIGITS = "2348169898763"
    const val OFFICIAL_WHATSAPP_URL = "https://wa.me/2348169898763"

    fun resolveWhatsAppDisplayNumber(configuredNumber: String): String =
        configuredNumber.trim().ifBlank { OFFICIAL_WHATSAPP_NUMBER_DISPLAY }

    fun normalizeWhatsAppDigits(rawNumber: String): String {
        val digitsOnly = rawNumber.filter { it.isDigit() }
        if (digitsOnly.length < 7) return OFFICIAL_WHATSAPP_DIGITS
        return if (digitsOnly.startsWith("0") && digitsOnly.length == 11) {
            "234" + digitsOnly.drop(1)
        } else {
            digitsOnly
        }
    }

    fun resolveWhatsAppUrl(
        configuredNumber: String = "",
        prefilledMessage: String = "",
    ): String {
        val digits = normalizeWhatsAppDigits(configuredNumber)
        val baseUrl = "https://wa.me/$digits"
        val cleanMsg = prefilledMessage.trim()
        return if (cleanMsg.isBlank()) {
            baseUrl
        } else {
            "$baseUrl?text=${URLEncoder.encode(cleanMsg, "UTF-8").replace("+", "%20")}"
        }
    }
}
