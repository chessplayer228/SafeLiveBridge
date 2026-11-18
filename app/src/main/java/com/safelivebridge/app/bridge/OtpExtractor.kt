package com.safelivebridge.app.bridge

object OtpExtractor {

    // Регулярные выражения для поиска OTP-кодов
    private val OTP_PATTERNS = listOf(
        // "код: 123456", "пароль: 1234", "code: 123456"
        Regex("""(?i)(?:код|code|passcode|пароль|pin|auth|подтвержд\w*)\D{1,10}(\b\d{4,8}\b)"""),
        // "123456 - ваш проверочный код"
        Regex("""(\b\d{4,8}\b)\s*[-–—:]\s*(?i)(?:ваш|your)?\s*(?:код|code)"""),
        // "G-123456"
        Regex("""(?i)\b[g]-(\d{6})\b"""),
        // Отдельный 4-6 значный блок цифр после слов "вход", "войти", "списание"
        Regex("""(?i)(?:для входа|for login|списание|оплата)\D{1,15}(\b\d{4,8}\b)""")
    )

    fun extractOtp(text: String?): String? {
        if (text.isNullOrBlank()) return null

        for (pattern in OTP_PATTERNS) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val candidate = match.groupValues[1]
                if (candidate.length in 4..8) {
                    return candidate
                }
            }
        }
        return null
    }
}
