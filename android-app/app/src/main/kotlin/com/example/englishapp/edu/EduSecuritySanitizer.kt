package com.example.englishapp.edu

import java.net.URI

/**
 * Validación de seguridad, URLs y control de licencias en cliente Android.
 * Garantiza que contenido externo jamás ejecute scripts ni simule instrucciones de sistema.
 */
object EduSecuritySanitizer {

  private val htmlScriptBlock = Regex("<\\s*(script|style|iframe|object|embed)[^>]*>.*?<\\s*/\\s*\\1\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
  private val htmlTag = Regex("<[^>]+>")
  private val controlChars = Regex("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f]")
  private val multiWs = Regex("[ \\t]+")

  private val injectionPatterns = listOf(
    Regex("ignore\\s+(?:all\\s+|any\\s+)?previous\\s+instructions", RegexOption.IGNORE_CASE),
    Regex("disregard\\s+(?:the\\s+)?system\\s+prompt", RegexOption.IGNORE_CASE),
    Regex("you\\s+are\\s+now\\s+(?:a|an|in)\\s+developer\\s+mode", RegexOption.IGNORE_CASE),
    Regex("<\\|(?:im_start|im_end|system|assistant|user)\\|>", RegexOption.IGNORE_CASE),
    Regex("^\\s*###\\s*(?:system|instruction)\\s*:", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)),
    Regex("javascript\\s*:", RegexOption.IGNORE_CASE),
    Regex("data\\s*:\\s*text/html", RegexOption.IGNORE_CASE),
  )

  private val storableLicenses = setOf(
    "FLUENTVOICE-OWNED",
    "CC0-1.0",
    "PDM-1.0",
    "CC-BY-2.0-FR",
    "CC-BY-3.0",
    "CC-BY-4.0",
    "CC-BY-SA-3.0",
    "CC-BY-SA-4.0",
  )

  private val metadataOrLinkLicenses = setOf(
    "CC-BY-NC-SA-4.0",
    "FREE-API-NONCOMM",
    "ALL-RIGHTS-RESERVED-LINK-ONLY",
  )

  fun hasPromptInjection(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    return injectionPatterns.any { it.containsMatchIn(text) }
  }

  fun sanitizeText(raw: String?, maxLen: Int = 3500): String {
    if (raw.isNullOrBlank()) return ""
    var cleaned = htmlScriptBlock.replace(raw, " ")
    cleaned = htmlTag.replace(cleaned, " ")
      .replace("&amp;", "&")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&nbsp;", " ")
    cleaned = controlChars.replace(cleaned, "")
    for (p in injectionPatterns) {
      cleaned = p.replace(cleaned, "[filtrado]")
    }
    cleaned = multiWs.replace(cleaned, " ").trim()
    return if (cleaned.length > maxLen) cleaned.take(maxLen - 3).trimEnd() + "..." else cleaned
  }

  fun isSafeExternalUrl(url: String?, allowedDomains: Set<String> = emptySet()): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", ignoreCase = true)) return false
    val host = uri.host?.lowercase()?.trimEnd('.') ?: return false
    if (!host.contains('.')) return false
    if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0" || host.endsWith(".local") || host.endsWith(".internal")) {
      return false
    }
    if (host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("169.254.") || host.startsWith("127.")) {
      return false
    }
    if (host.startsWith("172.")) {
      val second = host.removePrefix("172.").substringBefore('.').toIntOrNull()
      if (second != null && second in 16..31) return false
    }
    if (allowedDomains.isNotEmpty()) {
      return allowedDomains.any { d -> host == d || host.endsWith(".$d") }
    }
    return true
  }

  fun normalizeLicenseId(raw: String?): String? {
    val norm = raw?.trim()?.uppercase() ?: return null
    if (norm in storableLicenses || norm in metadataOrLinkLicenses) return norm
    return when {
      norm.startsWith("CC0") -> "CC0-1.0"
      norm.contains("PUBLIC DOMAIN") || norm.startsWith("PD") -> "PDM-1.0"
      norm.startsWith("CC BY 2.0") || norm.startsWith("CC-BY-2.0") -> "CC-BY-2.0-FR"
      norm.startsWith("CC BY 3.0") || norm.startsWith("CC-BY-3.0") -> "CC-BY-3.0"
      norm.startsWith("CC BY 4.0") || norm.startsWith("CC-BY-4.0") -> "CC-BY-4.0"
      norm.startsWith("CC BY-SA 3.0") || norm.startsWith("CC-BY-SA-3.0") -> "CC-BY-SA-3.0"
      norm.startsWith("CC BY-SA 4.0") || norm.startsWith("CC-BY-SA-4.0") -> "CC-BY-SA-4.0"
      norm.startsWith("CC BY-NC-SA 4.0") || norm.startsWith("CC-BY-NC-SA-4.0") -> "CC-BY-NC-SA-4.0"
      else -> null
    }
  }

  fun isLicenseRecognized(licenseId: String?): Boolean {
    val norm = normalizeLicenseId(licenseId) ?: return false
    return norm in storableLicenses || norm in metadataOrLinkLicenses
  }

  fun canStoreFullBody(licenseId: String?): Boolean {
    val norm = normalizeLicenseId(licenseId) ?: return false
    return norm in storableLicenses
  }
}
