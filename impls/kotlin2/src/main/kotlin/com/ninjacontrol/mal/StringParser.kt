package main.kotlin.com.ninjacontrol.mal

class StringParser() {
    private var raw: String? = null
    var error: String? = null
    var parsed: String? = null

    val escapedQuoted: String?
        get() = when (parsed) {
            null -> null
            else -> raw
        }
    val unescapedUnqouted: String?
        get() = parsed?.let { return it.substring(1, it.length - 1) }
    val escapedUnqouted: String?
        get() = escapedQuoted?.let { return it.substring(1, it.length - 1) }

    private fun parse(string: String?) {

        raw = string
        val sb = StringBuilder()
        if (string != null) {

            var escaped = false
            fun appendEscaped(char: Char) {
                sb.append(char); escaped = false
            }

            if (string.length < 2 || (string.first() != '"' || string.last() != '"')
            ) {
                error = "Invalid or unbalanced string"
                return
            }

            string.forEachIndexed { pos, char ->
                when (char) {
                    '\\' -> when (escaped) {
                        true -> {
                            appendEscaped(char)
                        }
                        false -> escaped = true
                    }
                    '"' -> when (escaped) {
                        true -> {
                            if (pos == string.length - 1) {
                                error = "unbalanced string"
                                return@parse
                            } else appendEscaped(char)
                        }
                        false -> {
                            if (pos > 0 && pos < string.length - 1) {
                                error = "Unexpected end of string (at position $pos)"
                                return@parse
                            } else {
                                sb.append(char)
                            }
                        }
                    }
                    else -> when (escaped) {
                        true -> {
                            when (val c = checkEscaped(char)) {
                                null -> {
                                    error = "Invalid escaped character '$char'"
                                    escaped = false
                                    return@parse
                                }
                                else -> appendEscaped(c)
                            }
                        }
                        false -> sb.append(char)
                    }
                }
            }
        } else {
            error = "String is null"
        }
        parsed = if (error == null) {
            sb.toString()
        } else {
            null
        }
    }

    private fun checkEscaped(char: Char) = when (char) {
        't' -> '\t'
        'n' -> '\n'
        'r' -> '\r'
        else -> null
    }

    companion object {

        fun parse(string: String): StringParser {
            val parser = StringParser()
            parser.parse(string)
            return parser
        }
    }
}
