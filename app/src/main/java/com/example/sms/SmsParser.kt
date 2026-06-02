package com.example.sms

import com.example.data.model.ParsedSms
import com.example.data.model.PaymentSource
import java.util.Locale
import java.util.regex.Pattern

object SmsParser {

    private val EXCLUDE_KEYWORDS = listOf("credited", "received", "refund", "reversed", "salary")
    private val INCLUDE_KEYWORDS = listOf("debited", "spent", "paid", "purchase of", "txn of", "sent")

    private val AMOUNT_PATTERN = Pattern.compile("(?i)(?:INR|Rs\\.?|₹)\\s?([0-9,]+(?:\\.[0-9]{1,2})?)")

    // Matcher for merchant extraction
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:at|to|towards|@)\\s+([A-Za-z0-9\\s\\.\\-\\@]{1,30})"),
        Pattern.compile(";\\s*([A-Za-z0-9\\s]{1,30})")
    )

    fun parse(smsBody: String, sender: String, timestamp: Long = System.currentTimeMillis()): ParsedSms? {
        val bodyLower = smsBody.lowercase(Locale.ROOT)

        // 1. Exclude check
        if (EXCLUDE_KEYWORDS.any { bodyLower.contains(it) }) {
            return null
        }

        // 2. Include check
        if (!INCLUDE_KEYWORDS.any { bodyLower.contains(it) }) {
            return null
        }

        // 3. Amount extraction
        val amountMatcher = AMOUNT_PATTERN.matcher(smsBody)
        if (!amountMatcher.find()) {
            return null
        }
        val amountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null

        // 4. Merchant extraction
        var merchant: String? = null
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(smsBody)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: ""
                var cleaned = candidate
                // Truncate at common terminators
                val terminators = listOf(
                    " on ", " via ", " from ", " using ", " through ", " UPI ", " Ref ", ";", ".", ",", " txn", " card", " with", " at ", " in "
                )
                for (term in terminators) {
                    val index = (" $cleaned ").lowercase(Locale.ROOT).indexOf(term.lowercase(Locale.ROOT))
                    if (index != -1) {
                        cleaned = cleaned.substring(0, Math.max(0, index)).trim()
                    }
                }
                
                // Trim additional prefixes like "PhonePe merchant", "VPA", etc.
                val lowerCleaned = cleaned.lowercase(Locale.ROOT)
                if (lowerCleaned.startsWith("phonepe merchant ")) {
                    cleaned = cleaned.substring("phonepe merchant ".length)
                } else if (lowerCleaned.startsWith("vpa ")) {
                    cleaned = cleaned.substring("vpa ".length)
                }

                cleaned = cleaned.trim().trim('.', ';', ',', '!')
                if (cleaned.isNotEmpty()) {
                    merchant = cleaned
                    break
                }
            }
        }

        // Fallback for merchant
        if (merchant == null || merchant.length < 2) {
            merchant = cleanSender(sender)
        }

        return ParsedSms(
            amount = amount,
            merchant = merchant,
            sender = sender,
            rawSms = smsBody,
            occurredAt = timestamp
        )
    }

    private fun cleanSender(sender: String): String {
        val dashIndex = sender.indexOf('-')
        val clean = if (dashIndex != -1) {
            sender.substring(dashIndex + 1)
        } else {
            sender
        }
        return clean.trim().uppercase(Locale.ROOT)
    }

    /**
     * Detects payment source by matching SMS body against smartKeywords from all PaymentSource entries.
     * @param smsBody The raw SMS text
     * @param sources All PaymentSource entries (from DB), each containing a comma-separated `smartKeywords` field
     * @return The matched PaymentSource name, or "UPI" as default fallback
     */
    fun detectPaymentSource(smsBody: String, sources: List<PaymentSource>): String {
        val bodyLower = smsBody.lowercase(Locale.ROOT)

        // Check custom sources first (user-defined take priority), then presets
        val sorted = sources.sortedByDescending { it.isCustom }

        for (source in sorted) {
            if (source.smartKeywords.isBlank()) continue
            val keywords = source.smartKeywords.lowercase(Locale.ROOT)
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (keywords.any { bodyLower.contains(it) }) {
                return source.name
            }
        }

        return "UPI" // Default fallback for most Indian bank SMS
    }
}
