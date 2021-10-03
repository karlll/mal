package main.kotlin.com.ninjacontrol.mal

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
    symbol("not") to not()
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

fun integerFunction(function: FunctionBody): MalFunction = func(
    precondition = { args ->
        if (!assertArgumentType<MalInteger>(args)) {
            MalError("Invalid argument type")
        } else null
    }
) { args ->
    function.invoke(args)
}

fun integerFunctionOfArity(n: Int, function: FunctionBody): MalFunction = func(
    precondition = { args ->
        when {
            args.size != n -> MalError("Invalid number of arguments, expected $n instead of ${args.size}.")
            !assertArgumentType<MalInteger>(args) -> MalError("Invalid argument type")
            else -> null
        }
    }
) { args ->
    function.invoke(args)
}

/* Printing */

fun prn() = functionOfAtLeastArity(1) { args ->
    val string = args.joinToString(separator = "", prefix = "", postfix = "") {
        printString(
            it,
            printReadably = true
        )
    }
    out(string)
    MalNil
}

fun pr_str() = functionOfAtLeastArity(1) { args ->
    val string = args.joinToString(separator = " ") { printString(it, printReadably = true) }
    MalString(value = string)
}

fun str() = functionOfAtLeastArity(1) { args ->
    val string = args.joinToString { printString(it, printReadably = false) }
    MalString(value = string)
}

fun println() = functionOfAtLeastArity(1) { args ->
    val string = args.joinToString(separator = " ") { printString(it, printReadably = false) }
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
    compare(it[0] as MalInteger, it[1] as MalInteger, ComparisonOperation.GreaterThanOrEqual)
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
        else -> MalError("Argument is not a list")
    }
}

fun count() = functionOfArity(1) { args ->
    when (val arg = args[0]) {
        is MalNil -> MalInteger(0)
        is MalList -> MalInteger(arg.size)
        else -> MalError("Argument is not a list")
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
