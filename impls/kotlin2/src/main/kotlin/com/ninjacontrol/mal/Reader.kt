package main.kotlin.com.ninjacontrol.mal

/**
 * ...
 * If the target language has objects types (OOP), then the next
 * step is to create a simple stateful Reader object in reader.qx.
 * This object will store the tokens and a position.
 * The Reader object will have two methods: next and peek.
 * next returns the token at the current position and increments the position.
 * peek just returns the token at the current position.
 *
 * - Add a function read_str in reader.qx.
 *   This function will call tokenize and then create a new Reader object instance with the tokens.
 *   Then it will call read_form with the Reader instance.
 * - Add a function tokenize in reader.qx.
 *   This function will take a single string and return an array/list of all the tokens (strings) in it.
 *
 * The following regular expression (PCRE) will match all mal tokens.
 *
 *   [\s,]*(~@|[\[\]{}()'`~^@]|"(?:\\.|[^\\"])*"?|;.*|[^\s\[\]{}('"`,;)]*)
 *
 */

class Reader(private val tokens: Array<String>) {
    private var pos = 0
    fun next(): String? = tokens.getOrNull(pos++)
    fun peek(): String? = tokens.getOrNull(pos)
    fun skip() = pos++
    fun currentFirst() = peek()?.firstOrNull()
    fun isCurrentFirst(char: Char) = peek()?.firstOrNull()?.equals(char) ?: false
}

val tokenPattern =
    "[\\s,]*(~@|[\\[\\]{}()'`~^@]|\"(?:\\\\.|[^\\\\\"])*\"?|;.*|[^\\s\\[\\]{}('\"`,;)]*)".toRegex()

fun trimmer(char: Char) = when (char) {
    ' ', '\n', '\t', ',' -> true
    else -> false
}

fun readStr(input: String): MalType = readForm(reader = Reader(tokens = tokenize(input)))

fun tokenize(input: String) =
    tokenPattern.findAll(input).map { it.value.trim(::trimmer) }.toList().toTypedArray()

/**
 * Add the function read_form to reader.qx.
 * This function will peek at the first token in the Reader object and switch on the
 * first character of that token. If the character is a left paren then read_list is
 * called with the Reader object. Otherwise, read_atom is called with the Reader Object.
 * The return value from read_form is a mal data type.
 * If your target language is statically typed then you will need some way for
 * read_form to return a variant or subclass type. For example, if your language is object oriented,
 * then you can define a top level MalType (in types.qx) that all your mal data types inherit from.
 * The MalList type (which also inherits from MalType) will contain a list/array of other MalTypes.
 * If your language is dynamically typed then you can likely just return a plain list/array of other mal types.
 */

fun readForm(reader: Reader): MalType {
    return when (reader.currentFirst()) {
        '(' -> readList(reader)
        '[' -> readVector(reader)
        '{' -> readMap(reader)
        ';' -> MalNil // comment
        '\'' -> readQuote(reader)
        '`' -> readQuote(reader, symbol = MalSymbol(name = "quasiquote"))
        '~' -> readQuote(reader, symbol = MalSymbol(name = "unquote"))
        null -> MalEOF
        else -> readAtom(reader)
    }
}

fun readQuote(reader: Reader, symbol: MalSymbol = MalSymbol(name = "quote")): MalType {
    reader.skip()
    val list = MalList(mutableListOf(symbol))
    return when (val form = readForm(reader)) {
        is MalError -> form
        is MalEOF -> MalError("Unexpected EOF")
        else -> {
            list.items.add(form)
            list
        }
    }
}

fun readList(reader: Reader): MalType {
    reader.skip() // skip list start marker
    val list = MalList(mutableListOf())
    while (true) {
        if (reader.isCurrentFirst(')')) {
            reader.skip() // end marker
            return list
        } else when (val form = readForm(reader)) {
            is MalError -> return form
            is MalEOF -> return MalError("Unexpected EOF")
            else -> list.items.add(form)
        }
    }
}

fun readVector(reader: Reader): MalType {
    reader.skip() // skip vector start marker
    val vector = MalVector(mutableListOf())
    while (true) {
        if (reader.isCurrentFirst(']')) {
            reader.skip() // end marker
            return vector
        } else when (val form = readForm(reader)) {
            is MalError -> return form
            is MalEOF -> return MalError("Unexpected EOF")
            else -> vector.items.add(form)
        }
    }
}

fun readMap(reader: Reader): MalType {
    reader.skip() // skip map start marker
    val map = MalMap(mutableMapOf())
    var key: MalType? = null
    while (true) {
        if (reader.isCurrentFirst('}')) {
            return if (key == null) {
                reader.skip()
                map
            } else {
                MalError("Missing value for key=$key")
            }
        } else when (val form = readForm(reader)) {
            is MalError -> return form
            is MalEOF -> return MalError("Unexpected EOF")
            else -> {
                if (key == null) {
                    key = form
                } else {
                    map.items[key] = form
                    key = null
                }
            }
        }
    }
}

/**
 * Add the function read_atom to reader.qx.
 * This function will look at the contents of the token and return the appropriate
 * scalar (simple/single) data type value. Initially, you can just implement numbers (integers) and symbols.
 * This will allow you to proceed through the next couple of steps before you will
 * need to implement the other fundamental mal types:
 *   nil, true, false, and string.
 * The remaining scalar mal type, keyword does not need to be implemented until step A
 * (but can be implemented at any point between this step and that).
 *
 * BTW, symbols types are just an object that contains a single string name value
 * (some languages have symbol types already).
 *
 */

fun readAtom(reader: Reader): MalType {
    return reader.next()?.let { atom ->
        when {
            atom == "nil" -> MalNil
            Atoms.integerPattern.matches(atom) -> MalInteger(atom.toInt())
            Atoms.stringPattern.matches(atom) -> {
                val parsed = StringParser.parse(atom)
                parsed.unescapedUnqouted?.let {
                    MalString(value = it)
                } ?: MalError(parsed.error ?: "Unexpected error when parsing string")
            }
            Atoms.keywordPattern.matches(atom) -> MalKeyword(name = atom.trimStart(':'))
            Atoms.booleanPattern.matches(atom) -> MalBoolean(value = atom.toBoolean())
            else -> MalSymbol(name = atom)
        }
    } ?: MalEOF
}

class Atoms {
    companion object {
        val integerPattern = "-?\\d+".toRegex()
        val stringPattern = "\".*".toRegex()
        val keywordPattern = ":.+".toRegex()
        val booleanPattern = "true|false".toRegex()
    }
}
