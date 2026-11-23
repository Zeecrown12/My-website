package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.BoutiqueContactHelper

fun launchWhatsAppChat(
    context: Context,
    configuredNumber: String = "",
    prefilledMessage: String = "",
) {
    val targetUrl = BoutiqueContactHelper.resolveWhatsAppUrl(
        configuredNumber = configuredNumber,
        prefilledMessage = prefilledMessage,
    )
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "Chat with us on WhatsApp: ${BoutiqueContactHelper.resolveWhatsAppDisplayNumber(configuredNumber)}",
            Toast.LENGTH_LONG,
        ).show()
    }
}
