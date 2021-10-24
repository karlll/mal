package main.kotlin.com.ninjacontrol.mal

import java.io.File
import java.nio.charset.Charset

val namespace: EnvironmentMap = mutableMapOf(
    symbol("+") to arithmeticFunction(ArithmeticOperation.Add),
    symbol("-") to arithmeticFunction(ArithmeticOperation.Subtract),
    symbol("*") to arithmeticFunction(ArithmeticOperation.Multiply),
    symbol("/") to arithmeticFunction(ArithmeticOperation.Divide),
    symbol("%") to arithmeticFunction(ArithmeticOperation.Modulo),
    symbol("prn") to prn(),
    symbol("pr-str") to pr_str(),
    symbol("str") to str(),
    symbol("println") to println(),
    symbol("list") to list(),
    symbol("list?") to `list?`(),
    symbol("empty?") to `empty?`(),
    symbol("head") to head(),
    symbol("tail") to tail(),
    symbol("count") to count(),
    symbol("=") to eq(),
    symbol(">") to gt(),
    symbol(">=") to gte(),
    symbol("<") to lt(),
    symbol("<=") to lte(),
    symbol("not") to not(),
    symbol("read-string") to `read-string`(),
    symbol("slurp") to slurp(),
    symbol("atom") to atom(),
    symbol("deref") to deref(),
    symbol("atom?") to `atom?`(),
    symbol("reset!") to reset(),
    symbol("swap!") to swap()
)

fun func(precondition: ((Arguments) -> MalError?)? = null, function: FunctionBody): MalFunction =
    MalFunction { args ->
        // Do not allow argument that was evaluated as errors
        args.firstOrNull { it is MalError }?.let { error ->
            return@MalFunction error
        }

        if (precondition != null) {
            when (val preconditionResult = precondition.invoke(args)) {
                is MalError -> return@MalFunction preconditionResult
            }
        }
        function.invoke(args)
    }

inline fun <reified T> assertArgumentType(args: Arguments) = args.all { arg -> arg is T }
inline fun <reified T> assertArgumentNotType(args: Arguments) = args.none { arg -> arg is T }
fun assertNumberOfArguments(args: Arguments, amount: Int) = args.size == amount
fun assertNumberOfArgumentsOrMore(args: Arguments, amount: Int) = args.size >= amount

fun functionOfArity(n: Int, function: FunctionBody): MalFunction =
    func(
        precondition = { args ->
            if (!assertNumberOfArguments(args, n)) {
                MalError("Invalid number of arguments, expected $n instead of ${args.size}.")
            } else null
        }
    ) { args ->
        function.invoke(args)
    }

fun functionOfAtLeastArity(n: Int, function: FunctionBody): MalFunction =
    func(
        precondition = { args ->
            if (!assertNumberOfArgumentsOrMore(args, n)) {
                MalError("Invalid number of arguments, expected at least $n arguments, got ${args.size}.")
            } else null
        }
    ) { args ->
        function.invoke(args)
    }

inline fun <reified T> typedArgumentFunction(
    arity: Int = -1,
    minArity: Int = -1,
    crossinline function: FunctionBody
): MalFunction = func(
    precondition = { args ->
        when {
            (
                arity > 0 && !assertNumberOfArguments(
                    args,
                    arity
                )
                ) -> MalError("Invalid number of arguments, expected $arity arguments, got ${args.size}.")
            (
                minArity > 0 && !assertNumberOfArgumentsOrMore(
                    args,
                    minArity
                )
                ) -> MalError("Invalid number of arguments, expected at least $minArity arguments, got ${args.size}.")
            !assertArgumentType<T>(args) -> MalError("Invalid argument type, ${T::class} expected")
            else -> null
        }
    }
) { args ->
    function.invoke(args)
}

fun integerFunction(function: FunctionBody): MalFunction = func(
    precondition = { args ->
        if (!assertArgumentType<MalInteger>(args)) {
            MalError("Invalid argument type, expected an integer")
        } else null
    }
) { args ->
    function.invoke(args)
}

fun integerFunctionOfArity(n: Int, function: FunctionBody): MalFunction = func(
    precondition = { args ->
        when {
            args.size != n -> MalError("Invalid number of arguments, expected $n instead of ${args.size}.")
            !assertArgumentType<MalInteger>(args) -> MalError("Invalid argument type, expected an integer")
            else -> null
        }
    }
) { args ->
    function.invoke(args)
}

fun stringFunctionOfArity(n: Int, function: FunctionBody): MalFunction = func(
    precondition = { args ->
        when {
            args.size != n -> MalError("Invalid number of arguments, expected $n instead of ${args.size}.")
            !assertArgumentType<MalString>(args) -> MalError("Invalid argument type, expected a string")
            else -> null
        }
    }
) { args ->
    function.invoke(args)
}

/* Printing */

fun prn() = func { args ->
    val string = args.joinToString(separator = " ") {
        printString(
            it,
            printReadably = true
        )
    }
    out(string)
    MalNil
}

fun pr_str() = func { args ->
    val string =
        args.joinToString(separator = " ") {
            printString(
                it,
                printReadably = true,
                quoted = true
            )
        }
    MalString(value = string)
}

fun str() = func { args ->
    val string =
        args.joinToString(separator = "") {
            printString(
                it,
                printReadably = false,
                quoted = false
            )
        }
    MalString(value = string)
}

fun println() = func { args ->
    val string = args.joinToString(separator = " ") {
        printString(
            it,
            printReadably = false,
            quoted = false
        )
    }
    out(string)
    MalNil
}

/* Comparison */

enum class ComparisonOperation {
    GreaterThan,
    GreaterThanOrEqual,
    LessThan,
    LessThanOrEqual
}

fun eq() = functionOfArity(2) { MalBoolean(isEqual(it[0], it[1])) }
fun gt() = integerFunctionOfArity(2) {
    compare(it[0] as MalInteger, it[1] as MalInteger, ComparisonOperation.GreaterThan)
}

fun gte() = integerFunctionOfArity(2) {
    compare(
        it[0] as MalInteger,
        it[1] as MalInteger,
        ComparisonOperation.GreaterThanOrEqual
    )
}

fun lt() = integerFunctionOfArity(2) {
    compare(it[0] as MalInteger, it[1] as MalInteger, ComparisonOperation.LessThan)
}

fun lte() = integerFunctionOfArity(2) {
    compare(it[0] as MalInteger, it[1] as MalInteger, ComparisonOperation.LessThanOrEqual)
}

fun compare(a: MalInteger, b: MalInteger, operation: ComparisonOperation): MalBoolean {
    return when (operation) {
        ComparisonOperation.GreaterThan -> MalBoolean((a > b))
        ComparisonOperation.GreaterThanOrEqual -> MalBoolean((a >= b))
        ComparisonOperation.LessThan -> MalBoolean((a < b))
        ComparisonOperation.LessThanOrEqual -> MalBoolean((a <= b))
    }
}

fun not() = functionOfArity(1) {
    when (val arg = it[0]) {
        is MalNil -> True
        is MalBoolean -> MalBoolean(!arg.value)
        else -> False
    }
}

/* Lists */

fun list() = func { MalList(it.toMutableList()) }
fun `list?`() = functionOfArity(1) { args ->
    when (args[0]) {
        is MalList -> True
        else -> False
    }
}

fun `empty?`() = functionOfArity(1) { args ->
    when (val arg = args[0]) {
        is MalList -> if (arg.isEmpty()) True else False
        is MalVector -> if (arg.isEmpty()) True else False
        else -> MalError("Argument is not a list")
    }
}

fun count() = functionOfArity(1) { args ->
    when (val arg = args[0]) {
        is MalNil -> MalInteger(0)
        is MalList -> MalInteger(arg.size)
        is MalVector -> MalInteger(arg.size)
        else -> MalError("Argument is not a list nor a vector")
    }
}

fun head() = functionOfArity(1) { args ->
    when (val arg = args[0]) {
        is MalList -> arg.head
        else -> MalError("Argument is not a list")
    }
}

fun tail() = functionOfArity(1) { args ->
    when (val arg = args[0]) {
        is MalList -> arg.tail
        else -> MalError("Argument is not a list")
    }
}

/* Arithmetic functions */

enum class ArithmeticOperation {
    Add,
    Subtract,
    Multiply,
    Divide,
    Modulo
}

fun arithmeticFunction(operation: ArithmeticOperation) = integerFunction { args ->
    if (operation == ArithmeticOperation.Divide && args.drop(1)
        .any { (it as MalInteger).isZero }
    ) {
        return@integerFunction MalError("Division by zero")
    }
    args.reduce { acc, arg ->
        val op1 = acc as MalInteger
        val op2 = arg as MalInteger
        when (operation) {
            ArithmeticOperation.Add -> op1 + op2
            ArithmeticOperation.Subtract -> op1 - op2
            ArithmeticOperation.Multiply -> op1 * op2
            ArithmeticOperation.Divide -> op1 / op2
            ArithmeticOperation.Modulo -> op1 % op2
        }
    }
}

/* Input */

fun `read-string`() = stringFunctionOfArity(1) { args ->
    readStr((args[0] as MalString).value)
}

fun slurp() = stringFunctionOfArity(1) { args ->
    val fileName = args[0] as MalString
    readFileAsString(fileName.value, Charsets.UTF_8)
}

fun readFileAsString(fileName: String, charSet: Charset): MalType {
    val file = File(fileName)
    return when {
        !file.exists() -> MalError("File \"$fileName\" does not exist")
        !file.canRead() -> MalError("Can not read \"$fileName\"")
        file.length() > ((2L * 1024 * 1024 * 1024) - 1) -> MalError("File is too large")
        else -> MalString(file.readText(charSet))
    }
}

/* Atom */

fun atom() = functionOfArity(1) { args ->
    MalAtom(args[0])
}

fun deref() = typedArgumentFunction<MalAtom>(arity = 1) { args ->
    val atom = args[0] as MalAtom
    atom.value
}

fun `atom?`() = functionOfArity(1) { args ->
    if (args[0] is MalAtom) True else False
}

fun reset() = functionOfArity(2) { args ->
    when {
        (args[0] !is MalAtom) -> MalError("Not an atom")
        else -> {
            val atom = args[0] as MalAtom
            atom.value = args[1]
            atom.value
        }
    }
}

fun swap() = functionOfAtLeastArity(2) { args ->
    when {
        (args[0] !is MalAtom) -> MalError("Argument is not an atom")
        ((args[1] !is MalFunctionContainer) && (args[1] !is MalFunction)) -> MalError("Argument is not a function nor a function expression")
        else -> {
            val atom = args[0] as MalAtom
            val swapFunction = when (args[1]) {
                is MalFunctionContainer -> (args[1] as MalFunctionContainer).fn
                else -> (args[1] as MalFunction)
            }
            val additionalArgs =
                if (args.size > 2) args.sliceArray(2 until args.size) else emptyArray()
            val swapFunctionArgs = list(atom.value, *additionalArgs)
            when (val newValue = swapFunction.apply(swapFunctionArgs)) {
                is MalError -> newValue
                else -> {
                    atom.value = newValue
                    atom.value
                }
            }
        }
    }
}
