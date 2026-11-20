package com.example.morseflash

/** One step of a transmission: light on or off for [units] time units. */
data class Pulse(val on: Boolean, val units: Int)

object Morse {
    private val table = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
        ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
        '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-.",
    )

    /** Words separated by " / ", letters by spaces. Unsupported characters are skipped. */
    fun encode(text: String): String =
        text.trim().uppercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .map { word -> word.mapNotNull { table[it] }.joinToString(" ") }
            .filter { it.isNotEmpty() }
            .joinToString(" / ")

    /** Standard timing: dot 1, dash 3, symbol gap 1, letter gap 3, word gap 7. */
    fun pulses(text: String): List<Pulse> {
        val out = mutableListOf<Pulse>()
        val words = encode(text).split(" / ").filter { it.isNotEmpty() }
        words.forEachIndexed { wi, word ->
            if (wi > 0) out += Pulse(false, 7)
            word.split(" ").forEachIndexed { li, letter ->
                if (li > 0) out += Pulse(false, 3)
                letter.forEachIndexed { si, symbol ->
                    if (si > 0) out += Pulse(false, 1)
                    out += Pulse(true, if (symbol == '.') 1 else 3)
                }
            }
        }
        return out
    }
}
