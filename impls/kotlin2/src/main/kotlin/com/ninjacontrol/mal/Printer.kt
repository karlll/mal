package main.kotlin.com.ninjacontrol.mal

fun printString(form: MalType, printReadably: Boolean = true, debug: Boolean = false): String {

    val dbg = fun(str: () -> String) =
        when (debug) {
            true -> form.toString()
            false -> str()
        }

    return when (form) {
        is MalInteger -> dbg { form.value.toString() }
        is MalEOF -> dbg { "<EOF>" }
        is MalSymbol -> dbg { form.name }
        is MalError -> dbg { "*** ${form.message}" }
        is MalList -> dbg {
            form.items.joinToString(
                " ",
                "(",
                ")"
            ) { item -> printString(item) }
        }
        is MalBoolean -> dbg { form.value.toString() }
        is MalNil -> dbg { "nil" }
        is MalString -> dbg {
            val parsed = StringParser.parse(form.value)
            when {
                parsed.error != null -> (parsed.error ?: "unknown error").let { "*** $it" }
                printReadably -> "\"${parsed.escapedUnqouted}\""
                else -> "\"${parsed.unescapedUnqouted}\""
            }
        }
        is MalKeyword -> dbg { ":${form.name}" }
        is MalVector -> dbg {
            form.items.joinToString(
                " ",
                "[",
                "]"
            ) { item -> printString(item) }
        }
        is MalMap -> dbg {
            form.items.entries.joinToString(
                " ", "{", "}"
            ) { (key, value) ->
                "${printString(key)} ${printString(value)}"
            }
        }
        is MalFunction -> dbg {
            "#<fun>"
        }
    }
}

fun out(string: String, newLine: Boolean = true) = if (newLine) println(string) else print(string)
