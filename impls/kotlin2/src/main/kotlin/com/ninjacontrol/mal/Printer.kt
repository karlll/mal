package main.kotlin.com.ninjacontrol.mal

/**
 * Add a file `printer.qx`. This file will contain a single function
 * `pr_str` which does the opposite of `read_str`: take a mal data
 * structure and return a string representation of it. But `pr_str` is
 * much simpler and is basically just a switch statement on the type of
 * the input object:
 *
 * * symbol: return the string name of the symbol
 * * number: return the number as a string
 * * list: iterate through each element of the list calling `pr_str` on
 * it, then join the results with a space separator, and surround the
 * final result with parens
 */

fun printString(form: MalType, debug: Boolean = false): String {

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
        is MalString -> dbg { "\"${form.value}\"" }
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
    }
}
